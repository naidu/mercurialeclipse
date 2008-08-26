/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian Doetsch	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team.cache;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.core.RepositoryProvider;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgClients;
import com.vectrace.MercurialEclipse.commands.HgIdentClient;
import com.vectrace.MercurialEclipse.commands.HgLogClient;
import com.vectrace.MercurialEclipse.commands.HgRootClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

/**
 * @author bastian
 * 
 */
public class LocalChangesetCache extends AbstractCache {
    /**
     * 
     */
    private static final MercurialStatusCache STATUS_CACHE = MercurialStatusCache
            .getInstance();

    private static LocalChangesetCache instance = null;

    private static Map<IPath, ReentrantLock> locks = new HashMap<IPath, ReentrantLock>();
    private static Map<IPath, SortedSet<ChangeSet>> localChangeSets;

    private LocalChangesetCache() {
        localChangeSets = new HashMap<IPath, SortedSet<ChangeSet>>();
    }

    /**
     * 
     */
    private boolean isGetFileInformationForChangesets() {
        return Boolean
                .valueOf(
                        HgClients
                                .getPreference(
                                        MercurialPreferenceConstants.RESOURCE_DECORATOR_SHOW_CHANGESET,
                                        "false")).booleanValue();
    }

    public static LocalChangesetCache getInstance() {
        if (instance == null) {
            instance = new LocalChangesetCache();
        }
        return instance;
    }

    public synchronized void clear() {
        localChangeSets.clear();
        locks.clear();
    }

    public void clear(IResource objectResource) throws HgException {
        ReentrantLock lock = getLock(objectResource);
        try {
            lock.lock();
            Set<IResource> members = getMembers(objectResource);
            members.add(objectResource);
            for (IResource resource : members) {
                localChangeSets.remove(resource.getLocation());
            }
            notifyChanged(objectResource);
        } finally {
            lock.unlock();
        }
    }

    public SortedSet<ChangeSet> getLocalChangeSets(IResource objectResource)
            throws HgException {
        ReentrantLock lock = getLock(objectResource);
        try {
            lock.lock();
            SortedSet<ChangeSet> revisions = localChangeSets.get(objectResource
                    .getLocation());
            if (revisions == null) {
                if (objectResource.getType() == IResource.FILE
                        || objectResource.getType() == IResource.PROJECT
                        && STATUS_CACHE.isSupervised(objectResource)
                        && !STATUS_CACHE.isAdded(objectResource.getProject(),
                                objectResource.getLocation())) {
                    refreshAllLocalRevisions(objectResource);
                    revisions = localChangeSets.get(objectResource
                            .getLocation());
                }
            }
            if (revisions != null) {
                return Collections.unmodifiableSortedSet(revisions);
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Gets version for given resource.
     * 
     * @param objectResource
     *            the resource to get status for.
     * @return a String with version information.
     * @throws HgException
     */
    public ChangeSet getNewestLocalChangeSet(IResource objectResource)
            throws HgException {
        ReentrantLock lock = getLock(objectResource);
        try {
            lock.lock();

            SortedSet<ChangeSet> revisions = getLocalChangeSets(objectResource);
            if (revisions != null && revisions.size() > 0) {
                return revisions.last();
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

    /**
     * @param objectResource
     * @throws HgException
     */
    public ReentrantLock getLock(IResource objectResource) throws HgException {
        ReentrantLock lock;
        IPath hgRoot;
        try {
            if (objectResource.isAccessible()) {
                hgRoot = new Path(MercurialTeamProvider.getHgRoot(
                        objectResource).getAbsolutePath());
            } else {
                hgRoot = new Path(objectResource.getProject().getLocation()
                        .toOSString());
            }
            lock = locks.get(hgRoot);
            if (lock == null) {
                lock = new ReentrantLock();
                locks.put(hgRoot, lock);
            }
            return lock;
        } catch (CoreException e) {
            MercurialEclipsePlugin.logError(e);
            throw new HgException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Checks whether version is known.
     * 
     * @param objectResource
     *            the resource to be checked.
     * @return true if known, false if not.
     * @throws HgException
     */
    public boolean isLocallyKnown(IResource objectResource) throws HgException {
        ReentrantLock lock = getLock(objectResource);
        try {
            lock.lock();
            return localChangeSets.containsKey(objectResource.getLocation());
        } finally {
            lock.unlock();
        }
    }

    /**
     * See
     * {@link LocalChangesetCache#refreshAllLocalRevisions(IResource, boolean)}
     * Calls with limit = true.
     * 
     * @param project
     * @throws HgException
     */
    public void refreshAllLocalRevisions(IResource res) throws HgException {
        this.refreshAllLocalRevisions(res, true);
    }

    /**
     * See
     * {@link LocalChangesetCache#refreshAllLocalRevisions(IResource, boolean, boolean)}
     * 
     * Obtains file information, if preference is set to display changeset
     * information on label decorator.
     * 
     * @param res
     * @param limit
     * @throws HgException
     */
    public void refreshAllLocalRevisions(IResource res, boolean limit)
            throws HgException {
        this.refreshAllLocalRevisions(res, limit,
                isGetFileInformationForChangesets());
    }

    /**
     * Refreshes all local revisions. If limit is set, it looks up the default
     * number of revisions to get and fetches the topmost till limit is reached.
     * 
     * If withFiles is true and a resource version can't be found in the topmost
     * revisions, the last revision of this file is obtained via additional
     * calls.
     * 
     * @param project
     * @param limit
     *            whether to limit or to have full project log
     * @param withFiles
     *            true = include file in changeset
     * @throws HgException
     */
    public void refreshAllLocalRevisions(IResource res, boolean limit,
            boolean withFiles) throws HgException {
        Assert.isNotNull(res);
        if (null != RepositoryProvider.getProvider(res.getProject(),
                MercurialTeamProvider.ID)
                && res.getProject().isOpen()) {
            int defaultLimit = 2000;
            String pref = HgClients.getPreference(
                    MercurialPreferenceConstants.LOG_BATCH_SIZE, String
                            .valueOf(defaultLimit));
            try {
                defaultLimit = Integer.parseInt(pref);
                if (defaultLimit < 0) {
                    throw new NumberFormatException("Limit < 0");
                }
            } catch (NumberFormatException e) {
                MercurialEclipsePlugin
                        .logWarning(
                                "Log limit not correctly configured in preferences.",
                                e);
            }

            this.refreshAllLocalRevisions(res, limit, defaultLimit, withFiles);
        }
    }

    public ChangeSet getLocalChangeSet(IResource res, String nodeId)
            throws HgException {
        return getLocalChangeSet(res, nodeId, true);
    }

    public ChangeSet getLocalChangeSet(IResource res, String nodeId,
            boolean redecorate) throws HgException {
        Assert.isNotNull(res);
        Assert.isNotNull(nodeId);
        if (STATUS_CACHE.isSupervised(res)) {
            ReentrantLock lock = getLock(res);
            try {
                lock.lock();

                ChangeSet changeSet = getChangeSet(nodeId);
                if (changeSet == null) {
                    changeSet = HgLogClient.getChangeset(res, nodeId,
                            isGetFileInformationForChangesets());
                }
                SortedSet<ChangeSet> set = new TreeSet<ChangeSet>();
                if (changeSet != null) {
                    set.add(changeSet);
                    addToNodeMap(set);
                }
                return changeSet;
            } finally {
                lock.unlock();
                if (redecorate) {
                    notifyChanged(res);
                }
            }

        }
        return null;
    }        

    public ChangeSet getCurrentWorkDirChangeset(IResource res)
            throws HgException {
        try {
            File root = HgClients.getHgRoot(res.getLocation().toFile());
            String nodeId = HgIdentClient.getCurrentChangesetId(root);
            if (nodeId != null
                    && !nodeId
                            .equals("0000000000000000000000000000000000000000")) {
                return getLocalChangeSet(res, nodeId, false);
            }
        } catch (IOException e) {
            MercurialEclipsePlugin.logError(e);
            throw new HgException(e.getLocalizedMessage(), e);
        } catch (CoreException e) {
            MercurialEclipsePlugin.logError(e);
            throw new HgException(e.getLocalizedMessage(), e);
        }
        return null;
    }

    public void refreshAllLocalRevisions(IResource res, boolean limit,
            int limitNumber, boolean withFiles) throws HgException {
        refreshAllLocalRevisions(res, limit, limitNumber, -1, withFiles);
    }

    /**
     * Refreshes all local revisions. If limit is set, it looks up the default
     * number of revisions to get and fetches the topmost till limit is reached.
     * 
     * If a resource version can't be found in the topmost revisions, the last
     * revisions of this file (10% of limit number) are obtained via additional
     * calls.
     * 
     * @param res
     * @param limit
     *            whether to limit or to have full project log
     * @param limitNumber
     *            if limit is set, how many revisions should be fetched
     * @param startRev
     *            the revision to start with
     * @throws HgException
     */
    public void refreshAllLocalRevisions(IResource res, boolean limit,
            int limitNumber, int startRev, boolean withFiles)
            throws HgException {
        Assert.isNotNull(res);
        if (null != RepositoryProvider.getProvider(res.getProject(),
                MercurialTeamProvider.ID)
                && res.getProject().isOpen()) {

            if (!STATUS_CACHE.isSupervised(res)) {
                return;
            }

            ReentrantLock lock = getLock(res);
            try {
                lock.lock();
                Map<IPath, SortedSet<ChangeSet>> revisions = null;

                if (limit) {
                    revisions = HgLogClient.getProjectLog(res, limitNumber,
                            startRev, withFiles);
                } else {
                    revisions = HgLogClient.getCompleteProjectLog(res,
                            withFiles);

                }

                Set<IPath> paths = new HashSet<IPath>();
                IResource[] localMembers = STATUS_CACHE.getLocalMembers(res);
                for (IResource resource : localMembers) {
                    paths.add(resource.getLocation());
                }

                Set<IPath> concernedPaths = new HashSet<IPath>();

                if (revisions != null && revisions.size() > 0) {

                    concernedPaths.add(res.getLocation());
                    concernedPaths.addAll(paths);

                    // add all concerned resources if project is updated
                    // so we have all resources' changesets of the most
                    // recent revs.
                    if (res.getType() == IResource.PROJECT) {
                        concernedPaths.addAll(revisions.keySet());
                    } else {
                        // every changeset is at least stored for the repository
                        // root

                        File root = HgRootClient.getHgRootAsFile(res);
                        IPath rootPath = new Path(root.getCanonicalPath());
                        localChangeSets.put(res.getLocation(), revisions
                                .get(rootPath));
                    }

                    IWorkspaceRoot workspaceRoot = res.getWorkspace().getRoot();

                    for (Iterator<IPath> iter = revisions.keySet().iterator(); iter
                            .hasNext();) {
                        IPath path = iter.next();
                        SortedSet<ChangeSet> changes = revisions.get(path);
                        // if changes for resource not in cache, get at least 1
                        // revision
                        if (changes == null
                                && limit
                                && withFiles
                                && STATUS_CACHE.isSupervised(res.getProject(),
                                        path)
                                && !STATUS_CACHE
                                        .isAdded(res.getProject(), path)) {

                            IResource myResource = workspaceRoot
                                    .getFileForLocation(path);
                            if (myResource != null) {
                                changes = HgLogClient.getRecentProjectLog(
                                        myResource, 1, withFiles).get(path);
                            }
                        }
                        // add changes to cache
                        addChangesToLocalCache(path, changes);
                    }

                }
            } catch (IOException e) {
                MercurialEclipsePlugin.logError(e);
                throw new HgException(e.getLocalizedMessage(), e);
            } finally {
                lock.unlock();
                notifyChanged(res);
            }
        }
    }

    /**
     * @param path
     * @param changes
     */
    private void addChangesToLocalCache(IPath path, SortedSet<ChangeSet> changes) {
        if (changes != null && changes.size() > 0) {
            SortedSet<ChangeSet> existing = localChangeSets.get(path);
            if (existing == null) {
                existing = new TreeSet<ChangeSet>();
            }
            existing.addAll(changes);
            localChangeSets.put(path, existing);
            addToNodeMap(changes);
        }
    }

    /**
     * @return the localUpdateInProgress
     * @throws HgException
     */
    public boolean isLocalUpdateInProgress(IResource res) throws HgException {
        return getLock(res).isLocked();
    }

    /**
     * @param project
     * @param branchName
     * @return
     * @throws HgException
     */
    public SortedSet<ChangeSet> getLocalChangeSetsByBranch(IProject project,
            String branchName) throws HgException {
        ReentrantLock lock = getLock(project);
        try {
            lock.lock();
            SortedSet<ChangeSet> changes = getLocalChangeSets(project);
            SortedSet<ChangeSet> branchChangeSets = new TreeSet<ChangeSet>();
            for (ChangeSet changeSet : changes) {
                if (changeSet.getBranch().equals(branchName)
                        || (branchName.equals("default") && changeSet
                                .getBranch().equals(""))) {
                    branchChangeSets.add(changeSet);
                }
            }
            return branchChangeSets;
        } finally {
            lock.unlock();
        }
    }
}
