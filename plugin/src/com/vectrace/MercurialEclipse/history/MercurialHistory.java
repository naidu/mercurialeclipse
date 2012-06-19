/*******************************************************************************
 * Copyright (c) 2007-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Stefan Groschupf          - logError
 *     Stefan C                  - Code cleanup
 *     Andrei Loskutov           - bugfixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.history;

import static com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.provider.FileHistory;

import com.aragost.javahg.Changeset;
import com.aragost.javahg.commands.ExecutionException;
import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgBisectClient;
import com.vectrace.MercurialEclipse.commands.HgBisectClient.Status;
import com.vectrace.MercurialEclipse.commands.HgClients;
import com.vectrace.MercurialEclipse.commands.HgLogClient;
import com.vectrace.MercurialEclipse.commands.HgParentClient;
import com.vectrace.MercurialEclipse.commands.HgTagClient;
import com.vectrace.MercurialEclipse.commands.extensions.HgSigsClient;
import com.vectrace.MercurialEclipse.history.GraphLayout.ParentProvider;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.JHgChangeSet;
import com.vectrace.MercurialEclipse.model.Signature;
import com.vectrace.MercurialEclipse.model.Tag;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.utils.BranchUtils;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * @author zingo
 */
public class MercurialHistory extends FileHistory {

	private static final Comparator<ChangeSet> CS_COMPARATOR = new Comparator<ChangeSet>() {
		public int compare(ChangeSet o1, ChangeSet o2) {
			return o2.getIndex() - o1.getIndex();
		}
	};

	private final IResource resource;
	private final HgRoot hgRoot;
	private final List<MercurialRevision> revisions = new ArrayList<MercurialRevision>();
	private Tag[] tags;
	private int lastReqRevision;
	private boolean showTags;
	private boolean bisectStarted;
	private GraphLayout layout;

	// constructors

	/**
	 * @param resource must be non null
	 */
	public MercurialHistory(IResource resource) {
		super();
		Assert.isNotNull(resource);
		HgRoot root = MercurialTeamProvider.getHgRoot(resource);
		if(root != null && root.getIPath().equals(ResourceUtils.getPath(resource))){
			this.resource = null;
		} else {
			this.resource = resource;
		}
		hgRoot = root;
	}

	/**
	 * @param hgRoot must be non null
	 */
	public MercurialHistory(HgRoot hgRoot) {
		super();
		Assert.isNotNull(hgRoot);
		this.resource = null;
		this.hgRoot = hgRoot;
	}

	// operations

	/**
	 * @return true if this is a history of the hg root, otherwise it's about any sibling of it
	 */
	protected boolean isRootHistory() {
		return resource == null;
	}

	public void setBisectStarted(boolean started){
		this.bisectStarted = started;
	}

	public boolean isBisectStarted() {
		return bisectStarted;
	}

	/**
	 * @return last revision index requested for the current history, or zero if no
	 *         revisions was requested.
	 */
	public int getLastRequestedVersion() {
		return lastReqRevision;
	}

	public int getLastVersion() {
		if(revisions.isEmpty()) {
			return 0;
		}
		return revisions.get(revisions.size() - 1).getRevision();
	}

	public IFileRevision[] getContributors(IFileRevision revision) {
		return null;
	}

	public IFileRevision getFileRevision(String id) {
		if (revisions.isEmpty()) {
			return null;
		}

		for (MercurialRevision rev : revisions) {
			if (rev.getContentIdentifier().equals(id)) {
				return rev;
			}
		}
		return null;
	}

	public IFileRevision[] getFileRevisions() {
		if (!revisions.isEmpty()) {
			return revisions.toArray(new MercurialRevision[revisions.size()]);
		}
		return new IFileRevision[0];
	}

	public List<MercurialRevision> getRevisions() {
		if (!revisions.isEmpty()) {
			return new ArrayList<MercurialRevision>(revisions);
		}
		return Collections.emptyList();
	}

	public IFileRevision[] getTargets(IFileRevision revision) {
		return new IFileRevision[0];
	}

	/**
	 * Load more revisions.
	 *
	 * @param monitor
	 * @param from Revision number. The batch of revisions before this revision are loaded.
	 * @throws CoreException
	 */
	public void load(IProgressMonitor monitor, int from) throws CoreException {
		if (from < 0) {
			return;
		}
		if (from == Integer.MAX_VALUE) {
			// We're getting revisions up to the latest one available.
			// So clear out the cached list, as it may contain revisions
			// that no longer exist (e.g. after a strip/rollback).
			clear();
			tags = null;
			lastReqRevision = 0;
		}

		// check if we have reached the bottom (initially = Integer.MAX_VALUE)
		if (from == lastReqRevision) {
			return;
		}

		final IPreferenceStore store = MercurialEclipsePlugin.getDefault().getPreferenceStore();
		final int logBatchSize = store.getInt(LOG_BATCH_SIZE);
		final SortedSet<JHgChangeSet> changeSets = new TreeSet<JHgChangeSet>(CS_COMPARATOR);
		final IPath location;

		if(!isRootHistory()) {
			changeSets.addAll(HgLogClient.getResourceLog(hgRoot, resource, logBatchSize, from));
			location = ResourceUtils.getPath(resource);
		} else {
			changeSets.addAll(HgLogClient.getRootLog(hgRoot, logBatchSize, from));
			location = hgRoot.getIPath();
		}

		// no result -> bottom reached
		if (changeSets.isEmpty()) {
			lastReqRevision = from;
			return;
		}

		if (revisions.size() < changeSets.size()
				// ^ ????
				|| !(location.equals(ResourceUtils.getPath(revisions.get(0).getResource())))) {
			clear();
		}

		List<MercurialRevision> batch = createMercurialRevisions(changeSets);

		loadGraphData(batch, revisions.isEmpty() ? null : revisions.get(revisions.size() - 1));

		if(!revisions.isEmpty()){
			// in case of a particular data fetch before, we may still have some
			// temporary tags assigned to the last visible revision => cleanup it now
			MercurialRevision lastOne = revisions.get(revisions.size() - 1);
			lastOne.cleanupExtraTags();
		}

		int i = revisions.size();

		for (MercurialRevision rev : batch) {

			if (layout != null) {
				rev.setGraphRow(layout.getRow(i));
			}

			revisions.add(rev);
			i += 1;
		}

		lastReqRevision = from;

		if(showTags){
			if(!isRootHistory()) {
				if(tags == null){
					fetchTags();
				}
				assignTagsToRevisions();
			}
		}
	}

	private List<MercurialRevision> createMercurialRevisions(SortedSet<JHgChangeSet> changeSets) throws CoreException {
		IResource revisionResource = isRootHistory() ? hgRoot.getResource() : resource;
		Map<String, Signature> sigMap = getSignatures();
		Map<String, Status> bisectMap = HgBisectClient.getBisectStatus(hgRoot);
		setBisectStarted(!bisectMap.isEmpty());
		List<MercurialRevision> batch = new ArrayList<MercurialRevision>();

		for (JHgChangeSet cs : changeSets) {
			batch.add(new MercurialRevision(cs, revisionResource, sigMap.get(cs.getNode()),
					bisectMap.get(cs.getNode())));
		}

		return batch;
	}

	private void loadGraphData(List<MercurialRevision> newRevs, MercurialRevision lastRev) {
		if (isRootHistory() || resource.getType() == IResource.FILE) {
			ParentProvider parentProvider;

			if (layout == null) {
				if (isRootHistory()) {
					parentProvider = GraphLayout.ROOT_PARENT_PROVIDER;
				} else {
					parentProvider = new FileParentProvider();
				}

				layout = new GraphLayout(parentProvider, GraphLogTableViewer.NUM_COLORS);
			}
			else
			{
				parentProvider = layout.getParentProvider();
			}

			Changeset[] changesets = new Changeset[newRevs.size()];
			int i = 0;
			for (MercurialRevision rev : newRevs) {
				changesets[i] = rev.getChangeSet().getData();
				i++;
			}

			if (parentProvider instanceof FileParentProvider)
			{
				((FileParentProvider) parentProvider).prime(newRevs);
			}

			layout.add(changesets, lastRev == null ? null : lastRev.getChangeSet().getData());
		}
	}

	/**
	 * Clear data
	 */
	private void clear() {
		revisions.clear();
		layout = null;
		// TODO: tags?
	}

	private Map<String, Signature> getSignatures() throws CoreException {
		// get signatures
		Map<String, Signature> sigMap = new HashMap<String, Signature>();

		boolean sigcheck = "true".equals(HgClients.getPreference(
				PREF_SIGCHECK_IN_HISTORY, "false")); //$NON-NLS-1$

		if (sigcheck) {
			if (!"false".equals(MercurialUtilities.getGpgExecutable())) { //$NON-NLS-1$
				List<Signature> sigs = HgSigsClient.getSigs(hgRoot);
				for (Signature signature : sigs) {
					sigMap.put(signature.getNodeId(), signature);
				}
			}
		}
		return sigMap;
	}

	private void fetchTags() {
		// we need extra tag changesets for files/folders only.
		Tag[] tags2 = HgTagClient.getTags(hgRoot);
		SortedSet<Tag> sorted = new TreeSet<Tag>();
		for (Tag tag : tags2) {
			if(!tag.isTip()){
				sorted.add(tag);
			}
		}
		// tags are sorted naturally descending by cs revision
		tags = sorted.toArray(new Tag[sorted.size()]);
	}

	private void assignTagsToRevisions() {
		if(tags == null || tags.length == 0){
			return;
		}
		int start = 0;
		// sorted ascending by revision
		for (Tag tag : tags) {
			int matchingRevision = getFirstMatchingRevision(tag, start);
			if(matchingRevision >= 0){
				start = matchingRevision;
				revisions.get(matchingRevision).addTag(tag);
			}
		}
	}

	/**
	 * TODO: rewrite so this is correct with non-linear graphs
	 *
	 * @param tag
	 *            tag to search for
	 * @param start
	 *            start index in the revisions array
	 * @return first matching revision index in the revisions array, or -1 if no one
	 *         revision matches given tag
	 */
	private int getFirstMatchingRevision(Tag tag, int start) {
		String tagBranch = tag.getChangeset().getBranch();
		int tagRev = tag.getChangeset().getRevision();
		// revisions are sorted descending by cs revision
		int lastRev = getLastRevision(tagBranch);
		for (int i = start; i <= lastRev; i++) {
			i = getNextRevision(i, tagBranch);
			int revision = revisions.get(i).getRevision();
			// perfect match
			if(revision == tagRev){
				return i;
			}
			// if tag rev is greater as greatest (first) revision, return the version,
			// because the last file version was created before the tag => so it
			// was the current one at the time the tag was created
			if(i == 0 && tagRev > revision){
				return i;
			}
			// if tag rev is smaller as smallest (last) revision, return
			if(i == lastRev && tagRev < revision){
				// fix for bug 10830
				return -1;
			}
			// if tag rev is greater as current rev, return the version
			if(tagRev > revision){
				return i;
			}
		}
		return -1;
	}

	/**
	 * @param branch
	 *            may be null
	 * @return <b>internal index</b> of the latest revision known for this branch, or -1 if there
	 *         are no matches
	 */
	private int getLastRevision(String branch) {
		for (int i = revisions.size() - 1; i >= 0; i--) {
			MercurialRevision rev = revisions.get(i);
			if(BranchUtils.same(rev.getChangeSet().getBranch(), branch)){
				return i;
			}
		}
		return -1;
	}

	/**
	 * @param from
	 *            the first revision to start looking for
	 * @param branch
	 *            may be null
	 * @return <b>internal index</b> of the next revision (starting from given one) known for this
	 *         branch, or -1 if there are no matches
	 */
	private int getNextRevision(int from, String branch) {
		for (int i = from; i < revisions.size(); i++) {
			MercurialRevision rev = revisions.get(i);
			if(BranchUtils.same(rev.getChangeSet().getBranch(), branch)){
				return i;
			}
		}
		return -1;
	}

	/**
	 * @param showTags true to show tagged changesets, even if they are not related to the
	 * current file
	 */
	public void setEnableExtraTags(boolean showTags) {
		this.showTags = showTags;
	}

	// inner types

	/**
	 * Parent provider for a file following renames
	 *
	 * Strategy: Invoke parents for each of the known paths at each revision
	 *
	 * Using file status isn't sufficient: Copy source isn't present for some merges:
	 * http://bz.selenic.com/show_bug.cgi?id=3495
	 *
	 * <pre>
	 * hg log -Gf plugin/src/com/vectrace/MercurialEclipse/model/GChangeSet.java
	 * between: a06450a60e5f and 2e26551ca397
	 * </pre>
	 */
	private class FileParentProvider implements ParentProvider {

		protected final Set<IPath> knownPaths = LocalChangesetCache.getInstance().getKnownPaths(
				hgRoot, hgRoot.getRelativePath(resource));

		private List<MercurialRevision> unknownPathRevs;

		// operations

		public void prime(List<MercurialRevision> changesets) {
			unknownPathRevs = new LinkedList<MercurialRevision>(changesets);
		}

		/**
		 * @see com.vectrace.MercurialEclipse.history.GraphLayout.ParentProvider#getParents(com.aragost.javahg.Changeset)
		 */
		public Changeset[] getParents(Changeset cs) {
			List<Changeset> parents = new ArrayList<Changeset>(4);

			for (IPath newPath : knownPaths) {
				try {
					for (Changeset newChangeset : HgParentClient.getParents(hgRoot, cs, newPath)) {
						if (!parents.contains(newChangeset)) {
							parents.add(newChangeset);
						}

						setPath(newChangeset, newPath);
					}
				} catch (ExecutionException e) {
				}
			}

			return parents.toArray(new Changeset[parents.size()]);
		}

		private void setPath(Changeset newChangeset, IPath newPath) {
			for (Iterator<MercurialRevision> it = unknownPathRevs.iterator(); it.hasNext();) {
				MercurialRevision rev = it.next();

				if (newChangeset.getRevision() == rev.getRevision()) {
					rev.setIPath(newPath);
					it.remove();
					return;
				}
			}

			// Might happen if a file has two children
			// Eg exists under two names in the same revision
		}
	}
}
