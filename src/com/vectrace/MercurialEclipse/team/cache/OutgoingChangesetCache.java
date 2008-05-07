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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.RepositoryProvider;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

/**
 * @author bastian
 *
 */
public class OutgoingChangesetCache extends AbstractCache {
    private static OutgoingChangesetCache instance;
    /**
     * The Map has the following structure: RepositoryLocation -> IResource -> Changeset-Set
     */
    private static Map<String, Map<IResource, SortedSet<ChangeSet>>> outgoingChangeSets;
    private boolean outgoingUpdateInProgress = false;
    
    private OutgoingChangesetCache() {        
        outgoingChangeSets = new HashMap<String, Map<IResource, SortedSet<ChangeSet>>>();
    }
    
    public static OutgoingChangesetCache getInstance() {
        if (instance == null) {
            instance = new OutgoingChangesetCache();
        }
        return instance;
    }
    
    public synchronized void clear() {
        outgoingChangeSets.clear();
    }

    public ChangeSet getNewestOutgoingChangeSet(IResource resource,
            String repositoryLocation) throws HgException {
    
        if (outgoingUpdateInProgress) {
            synchronized (outgoingChangeSets) {
                // wait for update...
            }
        }
    
        if (MercurialStatusCache.getInstance().isSupervised(resource)) {
    
            Map<IResource, SortedSet<ChangeSet>> repoMap = outgoingChangeSets
                    .get(repositoryLocation);
    
            SortedSet<ChangeSet> revisions = null;
            if (repoMap != null) {
                revisions = repoMap.get(resource);
            }
    
            if (revisions != null && revisions.size() > 0) {
                return revisions.last();
            }
        }
        return null;
    }

    /**
     * Gets all outgoing changesets of the given location for the given
     * IResource.
     * 
     * @param objectResource
     * @param repositoryLocation
     * @return
     * @throws HgException
     */
    public SortedSet<ChangeSet> getOutgoingChangeSets(IResource objectResource,
            String repositoryLocation) throws HgException {
        if (outgoingUpdateInProgress) {
            synchronized (outgoingChangeSets) {
                // wait...
            }
        }
        Map<IResource, SortedSet<ChangeSet>> repoOutgoing = outgoingChangeSets
                .get(repositoryLocation);
    
        SortedSet<ChangeSet> revisions = null;
        if (repoOutgoing != null) {
            revisions = repoOutgoing.get(objectResource);
        }
        if (revisions == null) {
            refreshOutgoingChangeSets(objectResource.getProject(),
                    repositoryLocation);
        }
    
        if (revisions != null) {
            return Collections.unmodifiableSortedSet(outgoingChangeSets.get(
                    repositoryLocation).get(objectResource));
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
    public IResource[] getOutgoingMembers(IResource resource,
            String repositoryLocation) {
        if (outgoingUpdateInProgress) {
            synchronized (outgoingChangeSets) {
                // wait...
            }
        }
        Map<IResource, SortedSet<ChangeSet>> changeSets = outgoingChangeSets
                .get(repositoryLocation);
        if (changeSets != null) {
            return changeSets.keySet()
                    .toArray(new IResource[changeSets.size()]);
        }
        return new IResource[0];
    }

    /**
     * Gets all outgoing changesets by querying Mercurial and adds them to the
     * caches.
     * 
     * @param project
     * @param repositoryLocation
     * @throws HgException
     */
    public void refreshOutgoingChangeSets(IProject project,
            String repositoryLocation) throws HgException {
    
        // check if mercurial is team provider and if we're working on an
        // open project
        if (null != RepositoryProvider.getProvider(project,
                MercurialTeamProvider.ID)
                && project.isOpen()) {
    
            // lock the cache till update is complete
            synchronized (outgoingChangeSets) {
                try {
                    outgoingUpdateInProgress = true;
    
                    addResourcesToCache(project, repositoryLocation,
                            outgoingChangeSets, Direction.OUTGOING);
                } finally {
                    outgoingUpdateInProgress = false;
                }
            }
        }
    }
}
