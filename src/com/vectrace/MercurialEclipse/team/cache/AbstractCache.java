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

import java.io.File;
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
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgIncomingClient;
import com.vectrace.MercurialEclipse.commands.HgOutgoingClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

/**
 * @author bastian
 *
 */
public abstract class AbstractCache extends Observable {

    protected final static Map<String, ChangeSet> nodeMap = new TreeMap<String, ChangeSet>();

    /**
     * @param repositoryLocation
     * @param direction
     *            flag, which direction should be queried.
     * @param changeSetMap
     * @throws HgException
     */
    protected void addResourcesToCache(
            IProject project,
            HgRepositoryLocation repositoryLocation,
            Map<HgRepositoryLocation, Map<IPath, SortedSet<ChangeSet>>> changeSetMap,
            Direction direction) throws HgException {

        // clear cache of old members
        final Map<IPath, SortedSet<ChangeSet>> removeMap = changeSetMap
        .get(repositoryLocation);

        if (removeMap != null) {
            removeMap.clear();
            changeSetMap.remove(repositoryLocation);
        }

        // get changesets from hg
        Map<IPath, SortedSet<ChangeSet>> resources;
        if (direction == Direction.OUTGOING) {
            resources = HgOutgoingClient.getOutgoing(project, repositoryLocation);
        } else {
            resources = HgIncomingClient.getHgIncoming(project, repositoryLocation);
        }

        // add them to cache(s)
        if (resources != null && resources.size() > 0) {

            for (Map.Entry<IPath, SortedSet<ChangeSet>> mapEntry : resources.entrySet()) {
                IPath path = mapEntry.getKey();
                SortedSet<ChangeSet> changes = mapEntry.getValue();

                if (changes != null && changes.size() > 0) {
                    SortedSet<ChangeSet> revisions = new TreeSet<ChangeSet>();
                    ChangeSet[] changeSets = changes
                    .toArray(new ChangeSet[changes.size()]);

                    if (changeSets != null) {
                        for (ChangeSet changeSet : changeSets) {
                            revisions.add(changeSet);
                            if (direction == Direction.INCOMING) {
                                synchronized (nodeMap) {
                                    nodeMap
                                    .put(changeSet.toString(),
                                            changeSet);
                                    nodeMap.put(changeSet.getChangeset(),
                                            changeSet);
                                }
                            }
                        }
                    }

                    Map<IPath, SortedSet<ChangeSet>> map = changeSetMap
                    .get(repositoryLocation);
                    if (map == null) {
                        map = new HashMap<IPath, SortedSet<ChangeSet>>();
                    }
                    map.put(path, revisions);
                    changeSetMap.put(repositoryLocation, map);
                }
            }
        }
    }

    protected void addToNodeMap(SortedSet<ChangeSet> changes) {
        for (ChangeSet changeSet : changes) {
            synchronized (AbstractCache.nodeMap) {
                AbstractCache.nodeMap.put(changeSet.toString(), changeSet);
                AbstractCache.nodeMap.put(changeSet.getChangeset(), changeSet);
            }
        }
    }

    /**
     * Gets Changeset by its identifier
     *
     * @param changeSet
     *            string in format rev:nodeshort or rev:node
     * @return
     */
    public ChangeSet getChangeSet(String changeSet) {
        return AbstractCache.nodeMap.get(changeSet);
    }

    public void notifyChanged(final IResource resource) {
        Set<IResource> resources = new HashSet<IResource>();
        resources.add(resource);
        notifyChanged(resources);
    }

    public void notifyChanged(Set<IResource> resources) {
        HashSet<IResource> set = new HashSet<IResource>();
        set.addAll(resources);
        for (IResource r : resources) {
            set.addAll(getMembers(r));
        }
        setChanged();
        notifyObservers(set);
    }

    protected Set<IResource> getMembers(IResource r) {
        HashSet<IResource> set = new HashSet<IResource>();
        if (r instanceof IContainer) {
            IContainer cont = (IContainer) r;
            try {
                IResource[] members = cont.members();
                if (members != null) {
                    for (IResource member : members) {
                        if (member instanceof IContainer) {
                            set.addAll(getMembers(member));
                        } else {
                            set.add(member);
                        }
                    }
                }
            } catch (CoreException e) {
                MercurialEclipsePlugin.logError(e);
            }
        }
        set.add(r);
        return set;
    }

    /**
     * @return never null
     */
    protected Set<IResource> getMembers(IResource resource,
            Map<IPath, SortedSet<ChangeSet>> changeSets) {
        Set<IResource> members = new HashSet<IResource>();
        if (changeSets != null) {
            IWorkspaceRoot root = resource.getWorkspace().getRoot();
            for (Iterator<IPath> i = changeSets.keySet().iterator(); i
            .hasNext();) {
                IPath path = i.next();
                IResource member = root.getFileForLocation(path);
                if (member != null
                        && member.getType() != IResource.FOLDER
                        && resource.getLocation().isPrefixOf(
                                member.getLocation())) {
                    members.add(member);
                }
            }
        }
        return members;
    }

    /**
     * @param hgRoot non null
     * @param project non null
     * @param repoRelPath path <b>relative</b> to the hg root
     * @return may return null, if the path is not found in the project
     */
    public IResource convertRepoRelPath(HgRoot hgRoot, IProject project, String repoRelPath) {
        // determine absolute path
        String resourceLocation = hgRoot.getAbsolutePath() + File.separator + repoRelPath;
        IPath path = new Path(resourceLocation);

        // determine project relative path
        int equalSegments = path.matchingFirstSegments(project.getLocation());
        path = path.removeFirstSegments(equalSegments);
        return project.findMember(path);
    }

    public synchronized static void clearNodeMap() {
        nodeMap.clear();
    }

}
