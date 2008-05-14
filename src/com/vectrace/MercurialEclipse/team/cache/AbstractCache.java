/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team.cache;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgIncomingClient;
import com.vectrace.MercurialEclipse.commands.HgOutgoingClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocationManager;

/**
 * @author bastian
 *
 */
public abstract class AbstractCache extends Observable {

    protected static Map<IProject, Set<IResource>> projectResources;
    protected static Comparator<ChangeSet> changeSetIndexComparator;
    protected static Map<String, ChangeSet> nodeMap = new TreeMap<String, ChangeSet>();
    
    
    /**
     * @param repositoryLocation
     * @param outgoing
     *            flag, which direction should be queried.
     * @param changeSetMap
     * @throws HgException
     */
    protected void addResourcesToCache(IProject project,
            String repositoryLocation,
            Map<String, Map<IResource, SortedSet<ChangeSet>>> changeSetMap,
            Direction direction) throws HgException {
        // load latest outgoing changesets from repository given in
        // parameter
        HgRepositoryLocationManager repoManager = MercurialEclipsePlugin
                .getRepoManager();
    
        HgRepositoryLocation hgRepositoryLocation = repoManager
                .getRepoLocation(repositoryLocation);
    
        // clear cache of old members
        final Map<IResource, SortedSet<ChangeSet>> removeMap = changeSetMap
                .get(repositoryLocation);
    
        if (removeMap != null) {
            removeMap.clear();
            changeSetMap.remove(repositoryLocation);
        }
    
        if (hgRepositoryLocation == null) {
            try {
                hgRepositoryLocation = new HgRepositoryLocation(
                        repositoryLocation);
                repoManager.addRepoLocation(hgRepositoryLocation);
            } catch (MalformedURLException e) {
                MercurialEclipsePlugin.logError(e);
                throw new HgException(e.getLocalizedMessage(), e);
            }
        }
    
        // get changesets from hg
        Map<IResource, SortedSet<ChangeSet>> resources;
        if (direction == Direction.OUTGOING) {
            resources = HgOutgoingClient.getOutgoing(project,
                    hgRepositoryLocation);
        } else {
            resources = HgIncomingClient.getHgIncoming(project,
                    hgRepositoryLocation);
        }
    
        // add them to cache(s)
        if (resources != null && resources.size() > 0) {
    
            for (Iterator<IResource> iter = resources.keySet().iterator(); iter
                    .hasNext();) {
                IResource res = iter.next();
                SortedSet<ChangeSet> changes = resources.get(res);
    
                if (changes != null && changes.size() > 0) {
                    SortedSet<ChangeSet> revisions = new TreeSet<ChangeSet>();
                    ChangeSet[] changeSets = changes
                            .toArray(new ChangeSet[changes.size()]);
    
                    if (changeSets != null) {
                        for (ChangeSet changeSet : changeSets) {
                            revisions.add(changeSet);
                            if (direction == Direction.INCOMING) {
                                synchronized (AbstractCache.nodeMap) {
                                    AbstractCache.nodeMap
                                            .put(changeSet.toString(),
                                                    changeSet);
                                }
                            }
                        }
                    }
    
                    Map<IResource, SortedSet<ChangeSet>> map = changeSetMap
                            .get(repositoryLocation);
                    if (map == null) {
                        map = new HashMap<IResource, SortedSet<ChangeSet>>();
                    }
                    map.put(res, revisions);
                    changeSetMap.put(repositoryLocation, map);
                }
            }
        }
    }
    /**
     * @param changes
     */
    protected void addToNodeMap(SortedSet<ChangeSet> changes) {
        for (ChangeSet changeSet : changes) {
            synchronized (AbstractCache.nodeMap) {
                AbstractCache.nodeMap.put(changeSet.toString(), changeSet);
            }
        }
    }
    
    protected void addToProjectResources(IResource member) {
        if (member.getType() == IResource.PROJECT) {
            return;
        }
        Set<IResource> set = AbstractCache.projectResources.get(member.getProject());
        if (set == null) {
            set = new HashSet<IResource>();
        }
        set.add(member);
        AbstractCache.projectResources.put(member.getProject(), set);
    }
    
    /**
     * Gets Changeset by its identifier
     * 
     * @param changeSet
     *            string in format rev:nodeshort
     * @return
     */
    public ChangeSet getChangeSet(String changeSet) {
        return AbstractCache.nodeMap.get(changeSet);
    }
    /**
     * @param resource
     */
    protected void notifyChanged(final IResource resource) {
        Set<IResource> resources = new HashSet<IResource>();
        resources.add(resource);
        notifyChanged(resources);
    }
    /**
     * @param resources
     */
    protected void notifyChanged(Set<IResource> resources) {
        for (IResource r : resources) {                    
        if (r instanceof IContainer) {
                IContainer cont = (IContainer) r;
                try {
                    resources.addAll(Arrays.asList(cont.members()));
                } catch (CoreException e) {
                    MercurialEclipsePlugin.logError(e);
                }
            }
        }
        setChanged();
        notifyObservers(resources);
    }

}
