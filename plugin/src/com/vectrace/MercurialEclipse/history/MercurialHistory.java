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
import java.util.HashSet;
import java.util.Iterator;
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
import com.vectrace.MercurialEclipse.history.GraphLayout.GraphRow;
import com.vectrace.MercurialEclipse.history.GraphLayout.ParentProvider;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.FileStatus;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.JHgChangeSet;
import com.vectrace.MercurialEclipse.model.Signature;
import com.vectrace.MercurialEclipse.model.Tag;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.utils.BranchUtils;
import com.vectrace.MercurialEclipse.utils.Pair;
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
	private final Map<MercurialRevision, Integer> revisionsByIndex = new HashMap<MercurialRevision, Integer>();
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
	 * @param prev
	 * @return a next revision int the history: revision wich is the successor of the given one (has
	 *         higher rev number)
	 */
	public MercurialRevision getNext(MercurialRevision prev){
		// revisions are sorted descending: first has the highest rev number
		for (int i = 0; i < revisions.size(); i++) {
			if (revisions.get(i) == prev) {
				if(i > 0){
					return revisions.get(i - 1);
				}
			}
		}
		return null;
	}

	/**
	 * @param next
	 * @return a previous revision int the history: revision wich is the ancestor of the given one
	 *         (has lower rev number)
	 */
	public MercurialRevision getPrev(MercurialRevision next){
		// revisions are sorted descending: first has the highest rev number
		for (int i = 0; i < revisions.size(); i++) {
			if (revisions.get(i) == next) {
				if (i + 1 < revisions.size()) {
					return revisions.get(i + 1);
				}
			}
		}
		return null;
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

	public void refresh(IProgressMonitor monitor, int from) throws CoreException {
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

		IPath location;
		List<JHgChangeSet> list;

		if(!isRootHistory()) {
			list = HgLogClient.getResourceLog(hgRoot, resource, logBatchSize, from);
			location = ResourceUtils.getPath(resource);
		} else {
			list = HgLogClient.getRootLog(hgRoot, logBatchSize, from);
			location = hgRoot.getIPath();
		}

		// no result -> bottom reached
		if (list.isEmpty()) {
			lastReqRevision = from;
			return;
		}

		// We need these to be in order for the GChangeSets to display properly
		TreeSet<JHgChangeSet> changeSets = new TreeSet<JHgChangeSet>(CS_COMPARATOR);
		changeSets.addAll(list);

		if (revisions.size() < changeSets.size()
				|| !(location.equals(ResourceUtils.getPath(revisions.get(0).getResource())))) {
			clear();
		}

		// Update graph data
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

			Changeset[] changesets = new Changeset[changeSets.size()];

			int i = 0;
			for (Iterator<JHgChangeSet> it = changeSets.iterator(); it.hasNext(); i++) {
				changesets[i] = it.next().getData();
			}

			layout.getParentProvider().prime(changesets);

			layout.add(changesets, revisions.isEmpty() ? null : revisions.get(revisions.size() - 1)
					.getChangeSet().getData());
		}

		if(!revisions.isEmpty()){
			// in case of a particular data fetch before, we may still have some
			// temporary tags assigned to the last visible revision => cleanup it now
			MercurialRevision lastOne = revisions.get(revisions.size() - 1);
			lastOne.cleanupExtraTags();
		}
		IResource revisionResource;
		if(isRootHistory()){
			revisionResource = hgRoot.getResource();
		} else {
			revisionResource = resource;
		}
		Map<String, Signature> sigMap = getSignatures();
		Map<String, Status> bisectMap = HgBisectClient.getBisectStatus(hgRoot);
		setBisectStarted(!bisectMap.isEmpty());

		int i = revisions.size();

		for (JHgChangeSet cs : changeSets) {
			Signature sig = !sigMap.isEmpty() ? sigMap.get(cs.getNode()) : null;
			Status bisectStatus = !bisectMap.isEmpty() ? bisectMap.get(cs.getNode()) : null;
			MercurialRevision rev = new MercurialRevision(cs, revisionResource, sig, bisectStatus);

			revisions.add(rev);
			revisionsByIndex.put(rev, new Integer(i));
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

	/**
	 * Clear data
	 */
	private void clear() {
		revisions.clear();
		revisionsByIndex.clear();
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

	private int getIndex(MercurialRevision rev) {
		return revisionsByIndex.get(rev).intValue();
	}

	public GraphRow getGraphRow(MercurialRevision rev) {
		return layout == null ? null : layout.getRow(getIndex(rev));
	}

	// inner types

	/**
	 * Parent provider for a file following renames
	 */
	private class FileParentProvider implements ParentProvider {

		protected Map<Pair<Changeset, IPath>, List<Changeset>> map = new HashMap<Pair<Changeset, IPath>, List<Changeset>>();

		protected Set<IPath> knownPaths = new HashSet<IPath>();

		// operations

		/**
		 * TODO: this is a hack to populate knownPaths - why can't we get the copy source for some
		 * changesets. Status appears to not show copy source for some merges.
		 *
		 * <pre>
		 * hg log -Gf plugin/src/com/vectrace/MercurialEclipse/model/GChangeSet.java
		 * between: a06450a60e5f and 2e26551ca397
		 * </pre>
		 */
		public void prime(Changeset[] changesets) {
			knownPaths.add(hgRoot.getRelativePath(resource));

			for(Changeset cs: changesets) {
				getParents(cs);
			}
		}

		/**
		 * @see com.vectrace.MercurialEclipse.history.GraphLayout.ParentProvider#getParents(com.aragost.javahg.Changeset)
		 */
		public Changeset[] getParents(Changeset cs) {
			Pair<Changeset, IPath> key = new Pair<Changeset, IPath>(cs, null);
			List<Changeset> parents = new ArrayList<Changeset>(4);
			List<IPath> pathsToAdd = null;

			for (IPath path : knownPaths) {
				key.b = path;

				List<Changeset> changesets = map.get(key);

				if (changesets == null) {
					List<IPath> newPaths = getPaths(cs, path);
					changesets = new ArrayList<Changeset>(4);

					for (IPath newPath : newPaths) {
						try {
							for (Changeset newChangeset : HgParentClient.getParents(hgRoot, cs,
									newPath)) {
								if (!changesets.contains(newChangeset)) {
									changesets.add(newChangeset);
								}
							}
						} catch (ExecutionException e) {
						}
					}
					map.put(key.clone(), changesets);

					if (!newPaths.isEmpty())
					{
						if (pathsToAdd == null) {
							pathsToAdd = new ArrayList<IPath>();
						}
						pathsToAdd.addAll(newPaths);
					}
				}
				parents.addAll(changesets);
			}

			if (pathsToAdd != null) {
				knownPaths.addAll(pathsToAdd);
			}

			// What about a head after a rename, will hg show these?

			return parents.toArray(new Changeset[parents.size()]);
		}

		private List<IPath> getPaths(Changeset cs, IPath path) {
			JHgChangeSet jcs = LocalChangesetCache.getInstance().get(hgRoot, cs);

			if (cs.getParent1() == null) {
				return Collections.EMPTY_LIST;
			} else if (cs.getParent2() == null) {
				return Collections.singletonList(getPath(jcs, path, 0));
			}

			List<IPath> l = new ArrayList<IPath>(2);

			l.add(getPath(jcs, path, 0));
			l.add(getPath(jcs, path, 1));

			return l;
		}

		private IPath getPath(JHgChangeSet jcs, IPath path, int parent) {
			for (FileStatus status : jcs.getChangedFiles(parent)) {
				if (path.equals(status.getRootRelativePath())) {
					if (status.getRootRelativeCopySourcePath() != null) {
						return status.getRootRelativeCopySourcePath();
					}
					break;
				}
			}

			return path;
		}
	}
}
