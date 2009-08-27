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
import java.util.Map;
import java.util.Observable;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgIncomingClient;
import com.vectrace.MercurialEclipse.commands.HgOutgoingClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * This is the base class for 4 different caches we have in the current code:
 * <p>
 * Caches for local resources, do not maintain their state and has to be managed by clients:
 * <ul>
 * <li>{@link MercurialStatusCache} - maintains the Eclipse {@link IResource} states</li>
 * <li>{@link LocalChangesetCache} - maintains the known changesets in the local hg repository</li>
 * </ul>
 * <p>
 * Caches for remote resources and semi-automatically maintain their state:
 * <ul>
 * <li>{@link OutgoingChangesetCache} - maintains new changesets in the local hg repository</li>
 * <li>{@link IncomingChangesetCache} - maintains new changesets in the remote hg repository</li>
 * </ul>
 * @author bastian
 * @author Andrei Loskutov
 */
public abstract class AbstractCache extends Observable {

    private final Map<IProject, Map<String, ChangeSet>> changesets = new HashMap<IProject, Map<String, ChangeSet>>();
    protected final boolean debug;


    public AbstractCache() {
        debug = MercurialEclipsePlugin.getDefault().isDebugging();
    }

    /**
     * @param direction
     *            flag, which direction should be queried.
     */
    protected void addResourcesToCache(
            IProject project,
            HgRepositoryLocation repository,
            Map<HgRepositoryLocation, Map<IPath, SortedSet<ChangeSet>>> changeSetMap,
            Direction direction) throws HgException {

        if(debug) {
            System.out.println("\n!fetch " + direction + " for " + project);
        }

        // clear cache of old members
        final Map<IPath, SortedSet<ChangeSet>> removeMap = changeSetMap.get(repository);

        if (removeMap != null) {
            removeMap.clear();
            changeSetMap.remove(repository);
        }

        // get changesets from hg
        Map<IPath, SortedSet<ChangeSet>> resources;
        if (direction == Direction.OUTGOING) {
            resources = HgOutgoingClient.getOutgoing(project, repository);
        } else {
            resources = HgIncomingClient.getHgIncoming(project, repository);
        }

        HashMap<IPath, SortedSet<ChangeSet>> map = new HashMap<IPath, SortedSet<ChangeSet>>();
        changeSetMap.put(repository, map);
        IPath projectPath = project.getLocation();
        map.put(projectPath, new TreeSet<ChangeSet>());

        // add them to cache(s)
        for (Map.Entry<IPath, SortedSet<ChangeSet>> mapEntry : resources.entrySet()) {
            IPath path = mapEntry.getKey();
            SortedSet<ChangeSet> changes = mapEntry.getValue();
            if (changes != null && changes.size() > 0) {
                // XXX Andrei: we remember only incoming changesets because outgoing should be in the local cache already?
                //if (direction == Direction.INCOMING) {
                    addChangesets(project, changes);
                //}
                map.put(path, changes);
                map.get(projectPath).addAll(changes);
            }
        }
    }

    protected void addChangesets(IProject project, Set<ChangeSet> changes) {
        synchronized (changesets) {
            Map<String, ChangeSet> map = changesets.get(project);
            if(map == null){
                map = new ConcurrentHashMap<String, ChangeSet>();
                changesets.put(project, map);
            }
            for (ChangeSet changeSet : changes) {
                map.put(changeSet.toString(), changeSet);
                map.put(changeSet.getChangeset(), changeSet);
            }
        }
    }

    /**
     * Gets changeset by its identifier
     *
     * @param changesetId
     *            string in format rev:nodeshort or rev:node
     * @return may return null, if changeset is not known
     */
    public ChangeSet getChangeset(IProject project, String changesetId) {
        Map<String, ChangeSet> map;
        synchronized (changesets) {
            map = changesets.get(project);
        }
        if(map != null) {
            return map.get(changesetId);
        }
        return null;
    }

    /**
     * Spawns an update job to notify all the clients about given resource changes
     * @param resource non null
     */
    protected void notifyChanged(final IResource resource, boolean expandMembers) {
        final Set<IResource> resources = new HashSet<IResource>();
        resources.add(resource);
        notifyChanged(resources, expandMembers);
    }

    /**
     * Spawns an update job to notify all the clients about given resource changes
     * @param resources non null
     */
    protected void notifyChanged(final Set<IResource> resources, final boolean expandMembers) {
        Job job = new Job("hg cache clients update..."){
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                Set<IResource> set;
                if(!expandMembers){
                    set = resources;
                } else {
                    set = new HashSet<IResource>(resources);
                    for (IResource r : resources) {
                        if(monitor.isCanceled()){
                            return Status.CANCEL_STATUS;
                        }
                        set.addAll(getMembers(r));
                    }
                }
                setChanged();
                notifyObservers(set);
                return Status.OK_STATUS;
            }
        };
        job.setSystem(true);
        job.schedule();
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
        if (changeSets == null) {
            return members;
        }
        IWorkspaceRoot root = resource.getWorkspace().getRoot();
        IPath location = ResourceUtils.getPath(resource);
        for (IPath path : changeSets.keySet()) {
            IFile member = root.getFileForLocation(path);
            if (member != null && location.isPrefixOf(member.getLocation())) {
                members.add(member);
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

    protected boolean clearChangesets(IProject project) {
        synchronized (changesets){
            Map<String, ChangeSet> map = changesets.remove(project);
            return map != null && !map.isEmpty();
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
