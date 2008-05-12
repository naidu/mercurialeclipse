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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
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

    private boolean localUpdateInProgress = false;
    private static Map<IResource, SortedSet<ChangeSet>> localChangeSets;

    private LocalChangesetCache() {
        localChangeSets = new HashMap<IResource, SortedSet<ChangeSet>>();
        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
                .getProjects();
        if (projects != null) {
            for (IProject project : projects) {
                new RefreshLocalChangesetsJob(
                        "Initializing changeset cache for " + project.getName(),
                        project).schedule();
            }
        }
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

    public SortedSet<ChangeSet> getLocalChangeSets(IResource objectResource)
            throws HgException {
        SortedSet<ChangeSet> revisions = localChangeSets.get(objectResource);
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
        if (localUpdateInProgress) {
            synchronized (localChangeSets) {
                // waiting for update...
            }
        }
        SortedSet<ChangeSet> revisions = getLocalChangeSets(objectResource);
        if (revisions != null && revisions.size() > 0) {
            return revisions.last();
        }
        return null;
    }

    /**
     * Checks whether version is known.
     * 
     * @param objectResource
     *            the resource to be checked.
     * @return true if known, false if not.
     */
    public boolean isLocallyKnown(IResource objectResource) {
        if (localUpdateInProgress) {
            synchronized (localChangeSets) {
                // wait...
            }
        }
        return localChangeSets.containsKey(objectResource);
    }

    /**
     * Refreshes all local revisions, uses default limit of revisions to get,
     * e.g. the top 50 revisions.
     * 
     * If a resource version can't be found in the topmost revisions, the last
     * revisions of this file (10% of limit number) are obtained via additional
     * calls.
     * 
     * @param project
     * @throws HgException
     */
    public void refreshAllLocalRevisions(IResource res) throws HgException {
        this.refreshAllLocalRevisions(res, true);
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
     * @throws HgException
     */
    public void refreshAllLocalRevisions(IResource res, boolean limit)
            throws HgException {
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

            this.refreshAllLocalRevisions(res, limit, defaultLimit);
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
            int limitNumber) throws HgException {
        if (null != RepositoryProvider.getProvider(res.getProject(),
                MercurialTeamProvider.ID)
                && res.getProject().isOpen()) {
            synchronized (localChangeSets) {
                try {
                    localUpdateInProgress = true;

                    Map<IResource, SortedSet<ChangeSet>> revisions = null;

                    if (limit) {
                        revisions = HgLogClient.getRecentProjectLog(res,
                                limitNumber);
                    } else {
                        revisions = HgLogClient.getCompleteProjectLog(res);

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
                        }

                        for (Iterator<IResource> iter = revisions.keySet()
                                .iterator(); iter.hasNext();) {
                            IResource resource = iter.next();
                            SortedSet<ChangeSet> changes = revisions
                                    .get(resource);
                            // if changes for resource not in top 50, get at
                            // least 10%
                            if (changes == null && limit) {
                                changes = HgLogClient.getRecentProjectLog(
                                        resource, limitNumber / 10).get(
                                        resource);
                            }
                            // add changes to cache
                            if (changes != null && changes.size() > 0) {
                                if (STATUS_CACHE.isSupervised(resource)
                                        && !STATUS_CACHE.isAdded(resource)) {
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
                    }
                } finally {
                    localUpdateInProgress = false;
                    setChanged();
                    notifyObservers(res);
                }
            }
        }
    }

    public ChangeSet getLocalChangeSet(IResource res, int changesetIndex)
            throws HgException {
        if (localUpdateInProgress) {
            synchronized (AbstractCache.nodeMap) {
                // wait
            }
        }
        SortedSet<ChangeSet> locals = getLocalChangeSets(res);
        List<ChangeSet> list = new ArrayList<ChangeSet>(locals);
        int index = Collections.binarySearch(list, new ChangeSet(
                changesetIndex, "", "", ""),
                AbstractCache.changeSetIndexComparator);
        if (index >= 0) {
            return list.get(index);
        }
        return null;
    }

    /**
     * @return the localUpdateInProgress
     */
    public boolean isLocalUpdateInProgress() {
        return localUpdateInProgress;
    }

}
