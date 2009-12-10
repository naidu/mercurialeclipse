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

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.provider.FileHistory;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgLogClient;
import com.vectrace.MercurialEclipse.commands.extensions.HgGLogClient;
import com.vectrace.MercurialEclipse.commands.extensions.HgSigsClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.GChangeSet;
import com.vectrace.MercurialEclipse.model.Signature;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

/**
 * @author zingo
 *
 */
public class MercurialHistory extends FileHistory {

	private static final class RevisionComparator implements
	Comparator<MercurialRevision>, Serializable {

		private static final long serialVersionUID = 5305190339206751711L;

		public int compare(MercurialRevision o1, MercurialRevision o2) {
			int result = o2.getRevision() - o1.getRevision();

			// we need to cover the situation when repo-indices are the same
			if (result == 0 && o1.getChangeSet().getDateString() != null
					&& o2.getChangeSet().getDateString() != null) {
				int dateCompare = o2.getChangeSet().getRealDate().compareTo(
						o1.getChangeSet().getRealDate());
				if (dateCompare != 0) {
					result = dateCompare;
				}
			}

			return result;
		}
	}

	private static final ChangeSetComparator csComparator = new ChangeSetComparator();
	private static final RevisionComparator revComparator = new RevisionComparator();

	private final IResource resource;
	private SortedSet<MercurialRevision> revisions;
	private Map<Integer, GChangeSet> gChangeSets;
	private int bottom;

	public MercurialHistory(IResource resource) {
		super();
		this.resource = resource;
	}

	/**
	 * @param prev
	 * @return a next revision int the history: revision wich is the successor of the
	 * given one (has higher rev number)
	 */
	public MercurialRevision getNext(MercurialRevision prev){
		// revisions are sorted descending: first has the highest rev number
		List<MercurialRevision> list = new ArrayList<MercurialRevision>(revisions);

		for (int i = 0; i < list.size(); i++) {
			if(list.get(i) == prev){
				if(i > 0){
					return list.get(i - 1);
				}
			}
		}
		return null;
	}

	/**
	 * @param next
	 * @return a previous revision int the history: revision wich is the ancestor of the
	 * given one (has lower rev number)
	 */
	public MercurialRevision getPrev(MercurialRevision next){
		// revisions are sorted descending: first has the highest rev number
		List<MercurialRevision> list = new ArrayList<MercurialRevision>(revisions);

		for (int i = 0; i < list.size(); i++) {
			if(list.get(i) == next){
				if(i + 1 < list.size()){
					return list.get(i + 1);
				}
			}
		}
		return null;
	}

	public int getBottom() {
		return bottom;
	}

	public void setBottom(int bottom) {
		this.bottom = bottom;
	}

	public IFileRevision[] getContributors(IFileRevision revision) {
		return null;
	}

	public IFileRevision getFileRevision(String id) {
		if (revisions == null || revisions.size() == 0) {
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
		if (revisions != null) {
			return revisions.toArray(new MercurialRevision[revisions.size()]);
		}
		return new IFileRevision[0];
	}

	public IFileRevision[] getTargets(IFileRevision revision) {
		return new IFileRevision[0];
	}

	public void refresh(IProgressMonitor monitor, int from)
	throws CoreException {
		RepositoryProvider provider = RepositoryProvider.getProvider(resource
				.getProject());
		if (!(provider instanceof MercurialTeamProvider)) {
			return;
		}
		if (from == Integer.MAX_VALUE && revisions != null) {
			// We're getting revisions up to the latest one available.
			// So clear out the cached list, as it may contain revisions
			// that no longer exist (e.g. after a strip/rollback).
			revisions.clear();
		}
		// We need these to be in order for the GChangeSets to display
		// properly

		SortedSet<ChangeSet> changeSets = new TreeSet<ChangeSet>(csComparator);

		int logBatchSize = Integer.parseInt(MercurialUtilities
				.getPreference(MercurialPreferenceConstants.LOG_BATCH_SIZE,
						"500")); //$NON-NLS-1$

		// check if we have reached the bottom (initially = 0)
		if (from == this.bottom || from < 0) {
			return;
		}
		Map<IPath, Set<ChangeSet>> map = HgLogClient.getProjectLog(
				resource, logBatchSize, from, false);

		// no result -> bottom reached
		if (map == null) {
			this.bottom = from;
			return;
		}

		// still changesets there -> process
		Set<ChangeSet> localChangeSets = map.get(resource
				.getLocation());
		if (localChangeSets == null) {
			return;
		}

		// get signatures
		File file = resource.getLocation().toFile();

		List<Signature> sigs = HgSigsClient.getSigs(file);
		Map<String, Signature> sigMap = new HashMap<String, Signature>();
		if (!MercurialUtilities.getGpgExecutable().equals("false")) { //$NON-NLS-1$
			for (Signature signature : sigs) {
				sigMap.put(signature.getNodeId(), signature);
			}
		}

		changeSets.addAll(localChangeSets);

		if (revisions == null || revisions.size() == 0
				|| revisions.size() < changeSets.size()
				|| !(revisions.first().getResource().equals(resource))) {
			revisions = new TreeSet<MercurialRevision>(revComparator);
		}

		// Update graph data also in batch
		updateGraphData(changeSets, logBatchSize, from);

		for (ChangeSet cs : changeSets) {
			Signature sig = sigMap.get(cs.getChangeset());
			revisions.add(new MercurialRevision(cs, gChangeSets
					.get(Integer.valueOf(cs.getChangesetIndex())),
					resource, sig));
		}
	}

	private void updateGraphData(SortedSet<ChangeSet> changeSets, int logBatchSize, int from) {
		// put glog changesets in map for later referencing
		gChangeSets = new HashMap<Integer, GChangeSet>();
		try {
			List<GChangeSet> gLogChangeSets = new HgGLogClient(resource, logBatchSize, from)
				.update(changeSets).getChangeSets();
			for (GChangeSet gs : gLogChangeSets) {
				if (gs != null) {
					gChangeSets.put(Integer.valueOf(gs.getRev()), gs);
				}
			}
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
		}
	}

}
