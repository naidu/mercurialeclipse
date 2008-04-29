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
package com.vectrace.MercurialEclipse.synchronize;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.ISubscriberChangeEvent;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.subscribers.SubscriberChangeEvent;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;
import org.eclipse.team.ui.synchronize.ISynchronizeScope;

import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.team.IStorageMercurialRevision;
import com.vectrace.MercurialEclipse.team.MercurialStatusCache;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

public class MercurialSynchronizeSubscriber extends Subscriber {
    private static MercurialStatusCache statusCache = MercurialStatusCache
            .getInstance();
    private ISynchronizeScope myScope;
    private IResource[] myRoots;
    private String repositoryLocation;

    public MercurialSynchronizeSubscriber(ISynchronizeScope scope,
            String repositoryLocation) {
        this.myScope = scope;
        this.repositoryLocation = repositoryLocation;
    }

    @Override
    public String getName() {
        return "Mercurial Repository Watcher";
    }

    @Override
    public IResourceVariantComparator getResourceComparator() {
        return MercurialResourceVariantComparator.getInstance();
    }

    @Override
    public SyncInfo getSyncInfo(IResource resource) throws TeamException {
        if (null != RepositoryProvider.getProvider(resource.getProject(),
                MercurialTeamProvider.ID)
                && resource.getProject().isOpen()) {
            ChangeSet csBase = statusCache.getNewestLocalChangeSet(resource);
            ChangeSet csRemote = statusCache.getNewestIncomingChangeSet(
                    resource, repositoryLocation);

            IResourceVariant base;
            IResourceVariant remote;

            if (csBase != null) {
                IStorageMercurialRevision baseIStorage = new IStorageMercurialRevision(
                        resource, csBase.getRevision().getRevision() + "",
                        csBase.getChangeset(), csBase);

                base = new MercurialResourceVariant(baseIStorage);

                IStorageMercurialRevision remoteIStorage;
                if (csRemote != null) {

                    remoteIStorage = new IStorageMercurialRevision(resource,
                            csRemote.getRevision().getRevision() + "", csRemote
                                    .getChangeset(), csRemote);
                } else {
                    remoteIStorage = baseIStorage;
                }

                remote = new MercurialResourceVariant(remoteIStorage);

                SyncInfo info = new MercurialSyncInfo(resource, base, remote,
                        MercurialResourceVariantComparator.getInstance());

                info.init();
                return info;
            }
        }
        return null;

    }

    @Override
    public boolean isSupervised(IResource resource) throws TeamException {

        return statusCache.isSupervised(resource);
    }

    @Override
    public IResource[] members(IResource resource) throws TeamException {
        Set<IResource> members = new HashSet<IResource>();
        IResource[] localMembers = statusCache.getLocalMembers(resource);
        IResource[] remoteMembers = statusCache.getIncomingMembers(resource,
                repositoryLocation);
        
        if (localMembers != null && localMembers.length > 0) {
            members.addAll(Arrays.asList(localMembers));
        }
        
        if (remoteMembers != null && remoteMembers.length > 0) {
            members.addAll(Arrays.asList(remoteMembers));
        }
        
        // we don't want ourself or the project as our member
        members.remove(resource.getProject());
        members.remove(resource);
        return members.toArray(new IResource[members.size()]);
    }

    @Override
    public void refresh(IResource[] resources, int depth,
            IProgressMonitor monitor) throws TeamException {
        IResource[] toRefresh = resources;

        if (toRefresh == null) {
            toRefresh = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        }
        Set<IProject> refreshed = new HashSet<IProject>(toRefresh.length);
        monitor.subTask("Refreshing resources...");
        List<ISubscriberChangeEvent> changeEvents = new ArrayList<ISubscriberChangeEvent>();
        for (IResource resource : toRefresh) {
            if (monitor.isCanceled()) {
                return;
            }
            IProject project = resource.getProject();
                        

            if (refreshed.contains(project)) {                
                monitor.worked(1);
                continue;
            }
                      
            statusCache.refresh(project, monitor, repositoryLocation);
            refreshed.add(project);
            IResource[] localMembers = statusCache.getLocalMembers(resource);
            for (IResource local : localMembers) {
                monitor.subTask("Adding local members of " + resource.getName()
                        + " to change.");
                changeEvents.add(new SubscriberChangeEvent(this,
                        ISubscriberChangeEvent.SYNC_CHANGED, local));
                monitor.worked(1);
            }
            IResource[] incomingMembers = statusCache.getIncomingMembers(
                    resource, null);
            for (IResource incoming : incomingMembers) {
                monitor.subTask("Adding incoming members of "
                        + resource.getName() + " to change.");
                changeEvents.add(new SubscriberChangeEvent(this,
                        ISubscriberChangeEvent.SYNC_CHANGED, incoming));
                monitor.worked(1);
            }
            monitor.worked(1);
        }
        monitor.subTask("Triggering sync status calculation.");
        super.fireTeamResourceChange(changeEvents
                .toArray(new ISubscriberChangeEvent[changeEvents.size()]));
        monitor.worked(1);
    }

    @Override
    public IResource[] roots() {
        if (myRoots == null) {
            if (myScope != null && myScope.getRoots() != null) {
                myRoots = myScope.getRoots();
            } else {
                myRoots = ResourcesPlugin.getWorkspace().getRoot()
                        .getProjects();
            }
        }
        return myRoots;
    }

}
