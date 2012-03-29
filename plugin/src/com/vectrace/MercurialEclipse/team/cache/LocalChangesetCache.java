/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch	        - implementation
 *     Andrei Loskutov          - bugfixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team.cache;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observer;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.preference.IPreferenceStore;

import com.aragost.javahg.Changeset;
import com.google.common.collect.MapMaker;
import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgLogClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.JHgChangeSet;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.utils.BranchUtils;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * The cache does NOT keeps the state automatically. Clients have explicitely request and manage
 * cache updates.
 * <p>
 * There is no guarantee that the data in the cache is up-to-date with the server. To get the latest
 * data, clients have explicitely refresh the cache before using it.
 * <p>
 * The cache does not maintain any states. If client "clear" this cache, it must make sure that they
 * request an explicit cache update. After "clear" and "refresh", a notification is sent to the
 * observing clients.
 * <p>
 * <b>Implementation note 1</b> the cache does not send any notifications...
 *
 * @author bastian
 * @author Andrei Loskutov
 */
public final class LocalChangesetCache extends AbstractCache {

	private static final MercurialStatusCache STATUS_CACHE = MercurialStatusCache.getInstance();

	private static LocalChangesetCache instance;

	private final ConcurrentMap<Changeset, JHgChangeSet> changesetCache  = new MapMaker().softValues().makeMap();

	/**
	 * Contains all the loaded changesets for each of the paths (resources)
	 */
	private final Map<IPath, SortedSet<JHgChangeSet>> localChangeSets;
	/**
	 * Stores all changesets for each project. changesets can be retreived by its rev:node or rev:shortnode strings. Only actually used for reading in
	 * getChangesetById(Project, String) which is private. This can probably be removed without adverse effect.
	 */
	private final Map<IProject, Map<String, JHgChangeSet>> changesets;
	/**
	 * Stores the latest changeset for each root
	 */
	private final Map<HgRoot, JHgChangeSet> latestChangesets;

	private int logBatchSize;

	private LocalChangesetCache() {
		super();
		localChangeSets = new HashMap<IPath, SortedSet<JHgChangeSet>>();
		changesets = new HashMap<IProject, Map<String, JHgChangeSet>>();
		latestChangesets = new HashMap<HgRoot, JHgChangeSet>();
	}

	public static synchronized LocalChangesetCache getInstance() {
		if (instance == null) {
			instance = new LocalChangesetCache();
		}
		return instance;
	}

	public JHgChangeSet get(HgRoot root, Changeset set) {
		if (set == null) {
			return null;
		}

		JHgChangeSet newCS = new JHgChangeSet(root, set);
		JHgChangeSet oldCS = changesetCache.putIfAbsent(set, newCS);

		return oldCS != null ? oldCS : newCS;
	}

	public void clear(HgRoot root) {
		synchronized (latestChangesets) {
			latestChangesets.remove(root);
		}
		synchronized (localChangeSets) {
			localChangeSets.remove(root.getIPath());
		}
		Set<IProject> projects = ResourceUtils.getProjects(root);
		for (IProject project : projects) {
			clear(project);
		}
	}

	/**
	 *
	 * @param resource
	 * @param notify
	 * @deprecated {@link #clear(HgRoot, boolean)} should be used in most cases
	 */
	@Deprecated
	public void clear(IResource resource) {
		Set<IResource> members = ResourceUtils.getMembers(resource);
		if(resource instanceof IProject && !resource.exists()) {
			members.remove(resource);
		}
		synchronized(localChangeSets){
			for (IResource member : members) {
				localChangeSets.remove(ResourceUtils.getPath(member));
			}
		}
		if(resource instanceof IProject){
			synchronized (changesets){
				changesets.remove(resource.getProject());
			}
		}
	}

	@Override
	protected void projectDeletedOrClosed(IProject project) {
		clear(project);
	}

	/**
	 * @param resource non null
	 * @return never null, but possibly empty set
	 */
	public SortedSet<JHgChangeSet> getOrFetchChangeSets(IResource resource) throws HgException {
		IPath location = ResourceUtils.getPath(resource);
		if(location.isEmpty()) {
			return EMPTY_SET;
		}

		SortedSet<JHgChangeSet> revisions;
		synchronized(localChangeSets){
			revisions = localChangeSets.get(location);
			if (revisions == null) {
				if (resource.getType() == IResource.FILE
						|| resource.getType() == IResource.PROJECT
						&& STATUS_CACHE.isSupervised(resource)
						&& !STATUS_CACHE.isAdded(location)) {
					refreshAllLocalRevisions(resource, true);
					revisions = localChangeSets.get(location);
				}
			}
		}
		if (revisions != null) {
			return Collections.unmodifiableSortedSet(revisions);
		}
		return EMPTY_SET;
	}

	/**
	 * @param hgRoot non null
	 * @return never null, but possibly empty set
	 */
	public SortedSet<JHgChangeSet> getOrFetchChangeSets(HgRoot hgRoot) throws HgException {
		IPath location = hgRoot.getIPath();

		SortedSet<JHgChangeSet> revisions;
		synchronized(localChangeSets){
			revisions = localChangeSets.get(location);
			if (revisions == null) {
				refreshAllLocalRevisions(hgRoot, true);
				revisions = localChangeSets.get(location);
			}
		}
		if (revisions != null) {
			return Collections.unmodifiableSortedSet(revisions);
		}
		return EMPTY_SET;
	}

	/**
	 * Gets changeset for given resource.
	 *
	 * @param resource
	 *            the resource to get status for.
	 * @return may return null
	 * @throws HgException
	 */
	public JHgChangeSet getNewestChangeSet(IResource resource) throws HgException {
		SortedSet<JHgChangeSet> revisions = getOrFetchChangeSets(resource);
		if (revisions.size() > 0) {
			return revisions.last();
		}
		return null;
	}

	public JHgChangeSet getNewestChangeSet(HgRoot hgRoot) throws HgException {
		SortedSet<JHgChangeSet> revisions = getOrFetchChangeSets(hgRoot);
		if (revisions.size() > 0) {
			return revisions.last();
		}

		return null;
	}

	/**
	 * Refreshes all local revisions. If limit is set, it looks up the default
	 * number of revisions to get and fetches the topmost till limit is reached.
	 * <p>
	 * A clear of all existing data for the given resource is triggered.
	 *
	 * @param res non null
	 * @param limit
	 *            whether to limit or to have full project log
	 * @throws HgException
	 */
	public void refreshAllLocalRevisions(IResource res, boolean limit) throws HgException {
		Assert.isNotNull(res);
		IProject project = res.getProject();
		if (MercurialTeamProvider.isHgTeamProviderFor(project)) {
			clear(res);
			fetchRevisions(res, limit, getLogBatchSize(), -1);
		}
	}

	/**
	 * Refreshes all local revisions. If limit is set, it looks up the default
	 * number of revisions to get and fetches the topmost till limit is reached.
	 * <p>
	 * A clear of all existing data for the given resource is triggered.
	 *
	 * @param hgRoot non null
	 * @param limit
	 *            whether to limit or to have full project log
	 * @throws HgException
	 */
	public Set<JHgChangeSet> refreshAllLocalRevisions(HgRoot hgRoot, boolean limit) throws HgException {
		Assert.isNotNull(hgRoot);
		clear(hgRoot);

		return fetchRevisions(hgRoot, limit, getLogBatchSize(), -1);
	}

	@Override
	protected void configureFromPreferences(IPreferenceStore store){
		logBatchSize = store.getInt(MercurialPreferenceConstants.LOG_BATCH_SIZE);
		if (logBatchSize < 0) {
			logBatchSize = 2000;
			MercurialEclipsePlugin.logWarning(Messages.localChangesetCache_LogLimitNotCorrectlyConfigured, null);
		}
	}


	/**
	 * Gets the configured log batch size.
	 */
	public int getLogBatchSize() {
		return logBatchSize;
	}

	/**
	 * Get a changeset
	 *
	 * @param hgRoot The root
	 * @param nodeId The node
	 * @return Never null
	 */
	public JHgChangeSet get(HgRoot hgRoot, String nodeId) throws HgException {
		Assert.isNotNull(hgRoot);
		Assert.isNotNull(nodeId);
		SortedSet<JHgChangeSet> sets = getOrFetchChangeSets(hgRoot);
		for (JHgChangeSet changeSet : sets) {
			if(nodeId.equals(changeSet.getNode())
					|| nodeId.equals(changeSet.toString())
					|| nodeId.equals(changeSet.getName())){
				return changeSet;
			}
		}

		// TODO: Cache by root?
		return HgLogClient.getChangeSet(hgRoot, nodeId);
	}

	public JHgChangeSet get(HgRoot root, int rev) throws HgException {
		Assert.isNotNull(root);
		Assert.isLegal(rev >= 0);
		SortedSet<JHgChangeSet> sets = getOrFetchChangeSets(root);
		for (JHgChangeSet changeSet : sets) {
			if (changeSet.getIndex() == rev) {
				return changeSet;
			}
		}

		// TODO: Cache by root?
		return HgLogClient.getChangeSet(root, rev);
	}

	/**
	 * @return may return null
	 */
	public ChangeSet getChangesetByRoot(IResource res) throws HgException {
		HgRoot root = MercurialTeamProvider.getHgRoot(res);
		if(root == null) {
			return null;
		}
		return getChangesetForRoot(root);
	}

	/**
	 * @return may return null
	 */
	public JHgChangeSet getChangesetForRoot(HgRoot root) throws HgException {
		// for projects in the same root try to use root cache
		synchronized (latestChangesets) {
			JHgChangeSet changeSet = latestChangesets.get(root);
			if(changeSet != null) {
				return changeSet;
			}
			String nodeId = HgLogClient.getCurrentChangesetId(root);
			if (!JHgChangeSet.NULL_ID.equals(nodeId)) {
				JHgChangeSet lastSet = HgLogClient.getChangeSet(root, nodeId);
				if (lastSet != null) {
					latestChangesets.put(root, lastSet);
				}
				return lastSet;
			}
		}
		return null;
	}

	/**
	 * Checks if the cache contains an old changeset. If this is the case, simply removes the cached
	 * value (new value will be retrieved later)
	 *
	 * @param root
	 *            working dir
	 * @param nodeId
	 *            latest working dir (full) changeset id
	 */
	public void checkLatestChangeset(HgRoot root, String nodeId) {
		if (nodeId == null || root == null) {
			return;
		}
		if (!JHgChangeSet.NULL_ID.equals(nodeId)) {
			synchronized (latestChangesets) {
				ChangeSet lastSet = latestChangesets.get(root);
				if (lastSet != null && !nodeId.equals(lastSet.getNode())) {
					latestChangesets.remove(root);
				}
			}
		}
	}


	/**
	 * Fetches local revisions. If limit is set, it looks up the default
	 * number of revisions to get and fetches the topmost till limit is reached.
	 *
	 * If a resource version can't be found in the topmost revisions, the last
	 * revisions of this file (10% of limit number) are obtained via additional
	 * calls.
	 *
	 * @param res non null
	 * @param limit
	 *            whether to limit or to have full project log
	 * @param limitNumber
	 *            if limit is set, how many revisions should be fetched
	 * @param startRev
	 *            the revision to start with
	 * @throws HgException
	 */
	public void fetchRevisions(IResource res, boolean limit,
			int limitNumber, int startRev) throws HgException {
		Assert.isNotNull(res);
		IProject project = res.getProject();
		if (!project.isOpen() || !STATUS_CACHE.isSupervised(res)) {
			return;
		}
		HgRoot root = MercurialTeamProvider.getHgRoot(res);
		Assert.isNotNull(root);

		List<JHgChangeSet> revisions;
		// now we may change cache state, so lock
		synchronized(localChangeSets){
			if (limit) {
				revisions = HgLogClient.getResourceLog(root, res, limitNumber, startRev);
			} else {
				revisions = HgLogClient.getResourceLog(root, res, -1, Integer.MAX_VALUE);
			}
			if (revisions.size() == 0) {
				return;
			}

			addChangesToLocalCache(project, ResourceUtils.getPath(res), revisions);
		}
	}

	/**
	 * Fetches local revisions. If limit is set, it looks up the default
	 * number of revisions to get and fetches the topmost till limit is reached.
	 *
	 * If a resource version can't be found in the topmost revisions, the last
	 * revisions of this file (10% of limit number) are obtained via additional
	 * calls.
	 *
	 * @param hgRoot non null
	 * @param limit
	 *            whether to limit or to have full project log
	 * @param limitNumber
	 *            if limit is set, how many revisions should be fetched
	 * @param startRev
	 *            the revision to start with
	 * @throws HgException
	 */
	public Set<JHgChangeSet> fetchRevisions(HgRoot hgRoot, boolean limit,
			int limitNumber, int startRev) throws HgException {
		Assert.isNotNull(hgRoot);

		List<JHgChangeSet> revisions;
		// now we may change cache state, so lock
		synchronized(localChangeSets){
			if (limit) {
				revisions = HgLogClient.getRootLog(hgRoot, limitNumber, startRev);
			} else {
				revisions = HgLogClient.getRootLog(hgRoot, -1, Integer.MAX_VALUE);
			}
			if (revisions.size() == 0) {
				return EMPTY_SET;
			}

			IPath path = hgRoot.getIPath();

			addChangesToLocalCache(null, path, revisions);

			return localChangeSets.get(path);
		}
	}

	@Override
	public synchronized void addObserver(Observer o) {
		// last implementation was very inefficient: the only listener was
		// the decorator, and this one has generated NEW cache updates each time
		// he was notified about changes, so it is an endless loop.
		// So temporary do not allow to observe this cache, until the code is improved
		// has no effect
		MercurialEclipsePlugin.logError(new UnsupportedOperationException("Observer not supported: " + o));
	}

	@Override
	public synchronized void deleteObserver(Observer o) {
		// has no effect
	}

	/**
	 * Spawns an update job to notify all the clients about given resource changes
	 * @param resource non null
	 */
	@Override
	protected void notifyChanged(final IResource resource, boolean expandMembers) {
		// has no effect
		MercurialEclipsePlugin.logError(new UnsupportedOperationException("notifyChanged not supported"));
	}

	/**
	 * Spawns an update job to notify all the clients about given resource changes
	 * @param resources non null
	 */
	@Override
	protected void notifyChanged(final Set<IResource> resources, final boolean expandMembers) {
		// has no effect
		MercurialEclipsePlugin.logError(new UnsupportedOperationException("notifyChanged not supported"));
	}

	private void addChangesets(IProject project, Collection<JHgChangeSet> changes) {
		synchronized (changesets) {
			Map<String, JHgChangeSet> map = changesets.get(project);
			if(map == null){
				map = new ConcurrentHashMap<String, JHgChangeSet>();
				changesets.put(project, map);
			}
			for (JHgChangeSet changeSet : changes) {
				map.put(changeSet.toString(), changeSet);
				map.put(changeSet.getNode(), changeSet);
				map.put(changeSet.getName(), changeSet);
			}
		}
	}

	/**
	 * @param path absolute file path
	 * @param changes may be null
	 */
	private void addChangesToLocalCache(IProject project, IPath path, Collection<JHgChangeSet> changes) {
		if (changes != null && changes.size() > 0) {
			SortedSet<JHgChangeSet> existing = localChangeSets.get(path);
			if (existing == null) {
				existing = new TreeSet<JHgChangeSet>();
			}
			localChangeSets.put(path, existing);
			existing.addAll(changes);
			if(project != null) {
				addChangesets(project, changes);
			}
		}
	}

	public SortedSet<JHgChangeSet> getOrFetchChangeSetsByBranch(HgRoot hgRoot, String branchName)
			throws HgException {

		SortedSet<JHgChangeSet> changes = getOrFetchChangeSets(hgRoot);
		SortedSet<JHgChangeSet> branchChangeSets = new TreeSet<JHgChangeSet>();
		for (JHgChangeSet changeSet : changes) {
			String changesetBranch = changeSet.getBranch();
			if (BranchUtils.same(branchName, changesetBranch)) {
				branchChangeSets.add(changeSet);
			}
		}
		return branchChangeSets;
	}
}
