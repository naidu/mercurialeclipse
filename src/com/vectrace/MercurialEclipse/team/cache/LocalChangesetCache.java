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

import java.util.Arrays;
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
import org.eclipse.team.core.RepositoryProvider;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgLogClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

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

    private static Map<IResource, ReentrantLock> locks = new HashMap<IResource, ReentrantLock>();
    private static Map<IResource, SortedSet<ChangeSet>> localChangeSets;

    private LocalChangesetCache() {
        localChangeSets = new HashMap<IResource, SortedSet<ChangeSet>>();
    }

    /**
     * 
     */
    private boolean isGetFileInformationForChangesets() {
        return Boolean
                .valueOf(
                        MercurialUtilities
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
    }
    
    public void clear(IResource objectResource) {
        ReentrantLock lock = getLock(objectResource);
        try {
            lock.lock();
            Set<IResource>members = getMembers(objectResource);
            members.add(objectResource);
            for (IResource resource : members) {
                localChangeSets.remove(resource);
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
            SortedSet<ChangeSet> revisions = localChangeSets
                    .get(objectResource);
            if (revisions == null) {
                if (objectResource.getType() != IResource.FOLDER
                        && STATUS_CACHE.isSupervised(objectResource)
                        && !STATUS_CACHE.isAdded(objectResource)) {
                    refreshAllLocalRevisions(objectResource);
                    revisions = localChangeSets.get(objectResource);
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
     */
    public ReentrantLock getLock(IResource objectResource) {
        ReentrantLock lock = locks.get(objectResource.getProject());
        if (lock == null) {
            lock = new ReentrantLock();
            locks.put(objectResource.getProject(), lock);
        }
        return lock;
    }

    /**
     * Checks whether version is known.
     * 
     * @param objectResource
     *            the resource to be checked.
     * @return true if known, false if not.
     */
    public boolean isLocallyKnown(IResource objectResource) {
        ReentrantLock lock = getLock(objectResource);
        try {
            lock.lock();
            return localChangeSets.containsKey(objectResource);
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
        if (null != RepositoryProvider.getProvider(res.getProject(),
                MercurialTeamProvider.ID)
                && res.getProject().isOpen()) {
            int defaultLimit = 2000;
            String pref = MercurialUtilities.getPreference(
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

    /**
     * Refreshes all local revisions. If limit is set, it looks up the default
     * number of revisions to get and fetches the topmost till limit is reached.
     * 
     * If a resource version can't be found in the topmost revisions, the last
     * revisions of this file (10% of limit number) are obtained via additional
     * calls.
     * 
     * @param project
     * @param limit
     *            whether to limit or to have full project log
     * @param limitNumber
     *            if limit is set, how many revisions should be fetched
     * @throws HgException
     */
    public void refreshAllLocalRevisions(IResource res, boolean limit,
            int limitNumber, boolean withFiles) throws HgException {
        if (null != RepositoryProvider.getProvider(res.getProject(),
                MercurialTeamProvider.ID)
                && res.getProject().isOpen()) {

            if (!STATUS_CACHE.isSupervised(res)) {
                return;
            }

            ReentrantLock lock = getLock(res);
            try {
                lock.lock();
                Map<IResource, SortedSet<ChangeSet>> revisions = null;

                if (limit) {
                    revisions = HgLogClient.getRecentProjectLog(res,
                            limitNumber, withFiles);
                } else {
                    revisions = HgLogClient.getCompleteProjectLog(res,
                            withFiles);

                }

                Set<IResource> resources = new HashSet<IResource>(Arrays
                        .asList(STATUS_CACHE.getLocalMembers(res)));

                Set<IResource> concernedResources = new HashSet<IResource>();

                if (revisions != null && revisions.size() > 0) {

                    concernedResources.add(res);
                    concernedResources.addAll(resources);

                    // add all concerned resources if project is updated
                    // so we have all resources' changesets of the most
                    // recent revs.
                    if (res.getType() == IResource.PROJECT) {
                        concernedResources.addAll(revisions.keySet());
                    } else {
                        // every changeset is at least stored for the project
                        localChangeSets.put(res, revisions
                                .get(res.getProject()));
                    }

                    for (Iterator<IResource> iter = revisions.keySet()
                            .iterator(); iter.hasNext();) {
                        IResource resource = iter.next();
                        SortedSet<ChangeSet> changes = revisions.get(resource);
                        // if changes for resource not in top 50, get at least 1
                        if (changes == null && limit && withFiles
                                && STATUS_CACHE.isSupervised(resource)
                                && !STATUS_CACHE.isAdded(resource)) {
                            changes = HgLogClient.getRecentProjectLog(resource,
                                    1, withFiles).get(resource);
                        }
                        // add changes to cache
                        if (changes != null && changes.size() > 0) {
                            SortedSet<ChangeSet> existing = localChangeSets
                                    .get(resource);
                            if (existing == null) {
                                existing = new TreeSet<ChangeSet>();
                            }
                            existing.addAll(changes);
                            localChangeSets.put(resource, existing);
                            addToNodeMap(changes);
                        }
                    }

                }
            } finally {
                lock.unlock();
                notifyChanged(res);
            }
        }
    }

    /**
     * @return the localUpdateInProgress
     */
    public boolean isLocalUpdateInProgress(IResource res) {
        return getLock(res).isLocked();
    }

    /**
     * @param project
     * @param branchName
     * @return 
     * @throws HgException
     */
    public SortedSet<ChangeSet> getLocalChangeSetsByBranch(IProject project, String branchName)
            throws HgException {
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
