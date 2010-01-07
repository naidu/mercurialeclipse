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
 *     Andrei Loskutov (Intland) - bugfixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.history;

import static com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants.*;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.provider.FileHistory;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgClients;
import com.vectrace.MercurialEclipse.commands.HgLogClient;
import com.vectrace.MercurialEclipse.commands.HgTagClient;
import com.vectrace.MercurialEclipse.commands.extensions.HgGLogClient;
import com.vectrace.MercurialEclipse.commands.extensions.HgSigsClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.GChangeSet;
import com.vectrace.MercurialEclipse.model.Signature;
import com.vectrace.MercurialEclipse.model.Tag;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

/**
 * @author zingo
 */
public class MercurialHistory extends FileHistory {

	private static final class RevisionComparator implements Comparator<MercurialRevision>,
			Serializable {

		private static final long serialVersionUID = 5305190339206751711L;

		public int compare(MercurialRevision o1, MercurialRevision o2) {
			int result = o2.getRevision() - o1.getRevision();

			// we need to cover the situation when repo-indices are the same
			if (result == 0 && o1.getChangeSet().getDateString() != null
					&& o2.getChangeSet().getDateString() != null) {
				result = o2.getChangeSet().getRealDate().compareTo(
						o1.getChangeSet().getRealDate());
			}
			return result;
		}
	}

	private static final ChangeSetComparator csComparator = new ChangeSetComparator();
	private static final RevisionComparator revComparator = new RevisionComparator();

	private final IResource resource;
	private final List<MercurialRevision> revisions;
	private Tag[] tags;
	private final Map<Integer, GChangeSet> gChangeSets;
	private GChangeSet lastGCS;
	private int lastReqRevision;
	private boolean showTags;

	/**
	 * @param resource must be non null
	 */
	public MercurialHistory(IResource resource) {
		super();
		Assert.isNotNull(resource);
		this.resource = resource;
		revisions = new ArrayList<MercurialRevision>();
		gChangeSets = new HashMap<Integer, GChangeSet>();
	}

	/**
	 * @param prev
	 * @return a next revision int the history: revision wich is the successor of the given one (has
	 *         higher rev number)
	 */
	public MercurialRevision getNext(MercurialRevision prev) {
		// revisions are sorted descending: first has the highest rev number
		for (int i = 0; i < revisions.size(); i++) {
			if (revisions.get(i) == prev) {
				if (i > 0) {
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
	public MercurialRevision getPrev(MercurialRevision next) {
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

	public IFileRevision[] getTargets(IFileRevision revision) {
		return new IFileRevision[0];
	}

	public void refresh(IProgressMonitor monitor, int from) throws CoreException {
		RepositoryProvider provider = RepositoryProvider.getProvider(resource.getProject());
		if (!(provider instanceof MercurialTeamProvider) || from < 0) {
			return;
		}
		if (from == Integer.MAX_VALUE && !revisions.isEmpty()) {
			// We're getting revisions up to the latest one available.
			// So clear out the cached list, as it may contain revisions
			// that no longer exist (e.g. after a strip/rollback).
			revisions.clear();
			gChangeSets.clear();
			tags = null;
			lastReqRevision = 0;
		}

		// check if we have reached the bottom (initially = Integer.MAX_VALUE)
		if (from == lastReqRevision) {
			return;
		}

		IPreferenceStore store = MercurialEclipsePlugin.getDefault().getPreferenceStore();
		int logBatchSize = store.getInt(LOG_BATCH_SIZE);

		Map<IPath, Set<ChangeSet>> map = HgLogClient.getProjectLog(resource, logBatchSize, from,
				false);

		// no result -> bottom reached
		if (map == null) {
			lastReqRevision = from;
			return;
		}

		// still changesets there -> process
		Set<ChangeSet> localChangeSets = map.get(resource.getLocation());
		if (localChangeSets == null) {
			lastReqRevision = from;
			return;
		}

		// We need these to be in order for the GChangeSets to display properly
		SortedSet<ChangeSet> changeSets = new TreeSet<ChangeSet>(csComparator);
		changeSets.addAll(localChangeSets);

		if (revisions.size() < changeSets.size()
				|| !(revisions.get(0).getResource().equals(resource))) {
			revisions.clear();
			gChangeSets.clear();
		}

		// Update graph data also in batch
		updateGraphData(changeSets, logBatchSize, from, store.getBoolean(ENABLE_FULL_GLOG));

		if(!revisions.isEmpty()){
			// in case of a particular data fetch before, we may still have some
			// temporary tags assigned to the last visible revision => cleanup it now
			MercurialRevision lastOne = revisions.get(revisions.size() - 1);
			lastOne.cleanupExtraTags();
		}

		Map<String, Signature> sigMap = getSignatures();
		for (ChangeSet cs : changeSets) {
			Signature sig = !sigMap.isEmpty() ? sigMap.get(cs.getChangeset()) : null;
			GChangeSet set = gChangeSets.get(Integer.valueOf(cs.getChangesetIndex()));
			revisions.add(new MercurialRevision(cs, set, resource, sig));
		}
		Collections.sort(revisions, revComparator);
		lastReqRevision = from;

		if(showTags){
			if(tags == null){
				fetchTags();
			}
			assignTagsToRevisions();
		}
	}

	private Map<String, Signature> getSignatures() throws CoreException {
		// get signatures
		Map<String, Signature> sigMap = new HashMap<String, Signature>();

		boolean sigcheck = HgClients.getPreference(
				PREF_SIGCHECK_IN_HISTORY, "false").equals("true"); //$NON-NLS-1$

		if (sigcheck) {
			File file = resource.getLocation().toFile();
			if (!MercurialUtilities.getGpgExecutable().equals("false")) { //$NON-NLS-1$
				List<Signature> sigs = HgSigsClient.getSigs(file);
				for (Signature signature : sigs) {
					sigMap.put(signature.getNodeId(), signature);
				}
			}
		}
		return sigMap;
	}

	private void fetchTags() throws HgException {
		Tag[] tags2 = HgTagClient.getTags(resource.getProject());
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
				revisions.get(start).addTag(tag);
			}
		}
	}

	/**
	 * @param tag
	 *            tag to serach for
	 * @param start
	 *            start index in the revisions array
	 * @return first matching revision index in the revisions array, or -1 if no one
	 *         revision matches given tag
	 */
	private int getFirstMatchingRevision(Tag tag, int start) {
		int tagRev = tag.getRevision();
		// revisions are sorted descending by cs revision
		int lastRev = revisions.size() - 1;
		for (int i = start; i <= lastRev ; i++) {
			int revision = revisions.get(i).getRevision();
			// perfect match
			if(revision == tagRev){
				return i;
			}
			// if tag rev is greater as greatest (first) revision, return -1 because
			// there can be no other version which match given tags
			if(i == 0 && tagRev > revision){
				return -1;
			}
			// if tag rev is smaller as smallest (last) revision, return
			if(i == lastRev && tagRev < revision){
				return lastRev;
			}
			// if tag rev is greater as current rev, return previous one
			if(tagRev > revision){
				return i - 1;
			}
		}
		return -1;
	}

	private void updateGraphData(SortedSet<ChangeSet> changeSets, int logBatchSize, int from,
			boolean enableFullLog) {
		if(enableFullLog && !gChangeSets.isEmpty() || resource.getType() == IResource.FOLDER){
			return;
		}
		logBatchSize = enableFullLog? 0 : logBatchSize;

		// put glog changesets in map for later referencing
		List<GChangeSet> gLogChangeSets;
		try {
			// the code below will produce sometimes bad graphs because the glog is re-set
			// each time we request the new portion of data.
			// the only reason why we use logBatchSize here and accept "bad" graphs is the performance
			gLogChangeSets = new HgGLogClient(resource, logBatchSize, from).getChangeSets();
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
			return;
		}
		if(!enableFullLog && !gLogChangeSets.isEmpty()){
			GChangeSet firstNew = gLogChangeSets.get(0);
			GChangeSet lastOld = lastGCS;
			if(lastOld != null) {
				// a VERY primitive, wrong way to connect graphs. works only if there
				// is one active branch at given changeset time
				firstNew.clean(lastOld);
			}
		}
		for (GChangeSet gs : gLogChangeSets) {
			if (gs != null) {
				gChangeSets.put(Integer.valueOf(gs.getRev()), gs);
			}
		}
		if(!enableFullLog && !gLogChangeSets.isEmpty()) {
			lastGCS = gLogChangeSets.get(gLogChangeSets.size() - 1);
		}
	}

	/**
	 * @param showTags true to show tagged changesets, even if they are not related to the
	 * current file
	 */
	public void setEnableExtraTags(boolean showTags) {
		this.showTags = showTags;
	}

}
