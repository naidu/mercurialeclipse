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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.team.core.RepositoryProvider;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

/**
 * @author bastian
 * 
 */
public class IncomingChangesetCache extends AbstractCache {
    private static IncomingChangesetCache instance = null;
    private static Map<IResource, ReentrantLock> locks = new HashMap<IResource, ReentrantLock>();

    /**
     * The Map has the following structure: RepositoryLocation -> IResource ->
     * Changeset-Set
     */
    private static Map<HgRepositoryLocation, Map<IPath, SortedSet<ChangeSet>>> incomingChangeSets;

    private IncomingChangesetCache() {
        incomingChangeSets = new HashMap<HgRepositoryLocation, Map<IPath, SortedSet<ChangeSet>>>();
    }

    public static IncomingChangesetCache getInstance() {
        if (instance == null) {
            instance = new IncomingChangesetCache();
        }
        return instance;
    }
    
    public synchronized void clear(HgRepositoryLocation repo) {
        incomingChangeSets.remove(repo);
    }
    
    public synchronized void clear() {
        incomingChangeSets.clear();
        locks.clear();
    }

    private ReentrantLock getLock(IResource objectResource) {
        ReentrantLock lock = locks.get(objectResource.getProject());
        if (lock == null) {
            lock = new ReentrantLock();
            locks.put(objectResource.getProject(), lock);
        }
        return lock;
    }

    /**
     * Gets all incoming chnagesets of all known project locations for the given
     * IResource.
     * 
     * @param objectResource
     * @return
     * @throws HgException
     */
    public SortedSet<ChangeSet> getIncomingChangeSets(IResource objectResource)
            throws HgException {
        ReentrantLock lock = getLock(objectResource);
        if (lock.isLocked()) {
            lock.lock();
            lock.unlock();
        }
        Set<HgRepositoryLocation> repos = MercurialEclipsePlugin
                .getRepoManager().getAllProjectRepoLocations(
                        objectResource.getProject());

        SortedSet<ChangeSet> allChanges = new TreeSet<ChangeSet>();
        for (Iterator<HgRepositoryLocation> iterator = repos.iterator(); iterator
                .hasNext();) {
            HgRepositoryLocation hgRepositoryLocation = iterator.next();
            SortedSet<ChangeSet> repoChanges = getIncomingChangeSets(
                    objectResource, hgRepositoryLocation);
            if (repoChanges != null) {
                allChanges.addAll(repoChanges);
            }
        }
        return Collections.unmodifiableSortedSet(allChanges);
    }

    /**
     * Gets all incoming changesets of the given location for the given
     * IResource.
     * 
     * @param objectResource
     * @param repositoryLocation
     * @return
     * @throws HgException
     */
    public SortedSet<ChangeSet> getIncomingChangeSets(IResource objectResource,
            HgRepositoryLocation repositoryLocation) throws HgException {
        ReentrantLock lock = getLock(objectResource);
        if (lock.isLocked()) {
            lock.lock();
            lock.unlock();
        }
        Map<IPath, SortedSet<ChangeSet>> repoIncoming = incomingChangeSets
                .get(repositoryLocation);

        SortedSet<ChangeSet> revisions = null;
        if (repoIncoming != null) {
            revisions = repoIncoming.get(objectResource.getLocation());
        }
        if (revisions == null) {
            refreshIncomingChangeSets(objectResource.getProject(),
                    repositoryLocation);
            repoIncoming = incomingChangeSets.get(repositoryLocation);
            if (repoIncoming != null) {
                revisions = repoIncoming.get(objectResource.getLocation());
            }
        }

        if (revisions != null) {
            return Collections.unmodifiableSortedSet(incomingChangeSets.get(
                    repositoryLocation).get(objectResource.getLocation()));
        }
        return null;
    }

    /**
     * Gets all resources that are changed in incoming changesets of given
     * repository, even resources not known in local workspace.
     * 
     * @param resource
     * @param repositoryLocation
     * @return
     */
    public IResource[] getIncomingMembers(IResource resource,
            HgRepositoryLocation repositoryLocation) {
        ReentrantLock lock = getLock(resource);
        try {
            lock.lock();
            List<IResource> members = new ArrayList<IResource>();
            Map<IPath, SortedSet<ChangeSet>> changeSets = incomingChangeSets
                    .get(repositoryLocation);
            members.addAll(getMembers(resource, changeSets));
            return members.toArray(new IResource[members.size()]);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Gets the newest incoming changeset of <b>all repositories</b>.
     * 
     * @param resource
     *            the resource to get the changeset for
     * @return
     * @throws HgException
     */
    public ChangeSet getNewestIncomingChangeSet(IResource objectResource)
            throws HgException {
        Set<HgRepositoryLocation> locs = MercurialEclipsePlugin
                .getRepoManager().getAllProjectRepoLocations(
                        objectResource.getProject());
        SortedSet<ChangeSet> changeSets = new TreeSet<ChangeSet>();
        for (HgRepositoryLocation hgRepositoryLocation : locs) {
            ChangeSet candidate = getNewestIncomingChangeSet(objectResource,
                    hgRepositoryLocation);
            if (candidate != null) {
                changeSets.add(candidate);
            }
        }
        if (changeSets.size() > 0) {
            return changeSets.last();
        }
        return null;
    }

    public ChangeSet getNewestIncomingChangeSet(IResource resource,
            HgRepositoryLocation repositoryLocation) throws HgException {

        ReentrantLock lock = getLock(resource);
        if (lock.isLocked()) {
            lock.lock();
            lock.unlock();
        }

        if (MercurialStatusCache.getInstance().isSupervised(resource) || (!resource.exists())) {

            Map<IPath, SortedSet<ChangeSet>> repoMap = incomingChangeSets
                    .get(repositoryLocation);

            SortedSet<ChangeSet> revisions = null;
            if (repoMap != null) {
                revisions = repoMap.get(resource.getLocation());
            }

            if (revisions != null && revisions.size() > 0) {
                return revisions.last();
            }
        }
        return null;
    }

    /**
     * Checks if incoming status for given project is known for any location.
     * 
     * @param project
     *            the project to be checked
     * @return true if known, false if not.
     */
    public boolean isIncomingStatusKnown(IProject project) {
        ReentrantLock lock = getLock(project);
        if (lock.isLocked()) {
            lock.lock();
            lock.unlock();
        }
        if (incomingChangeSets != null && incomingChangeSets.size() > 0) {
            for (Iterator<HgRepositoryLocation> iterator = incomingChangeSets
                    .keySet()
                    .iterator(); iterator.hasNext();) {
                Map<IPath, SortedSet<ChangeSet>> currLocMap = incomingChangeSets
                        .get(iterator.next());
                if (currLocMap != null
                        && currLocMap.get(project.getLocation()) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Gets all incoming changesets by querying Mercurial and adds them to the
     * caches.
     * 
     * @param project
     * @param repositoryLocation
     * @throws HgException
     */
    public void refreshIncomingChangeSets(IProject project,
            HgRepositoryLocation repositoryLocation) throws HgException {
        Assert.isNotNull(project);

        // check if mercurial is team provider and if we're working on an
        // open project
        if (null != RepositoryProvider.getProvider(project,
                MercurialTeamProvider.ID)
                && project.isOpen()) {

            // lock the cache till update is complete
            ReentrantLock lock = getLock(project);
            try {
                lock.lock();
                addResourcesToCache(project, repositoryLocation,
                        incomingChangeSets, Direction.INCOMING);

            } finally {
                lock.unlock();
                notifyChanged(project);
            }
        }
    }
}
