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
import java.util.HashSet;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.core.subscribers.ISubscriberChangeEvent;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.subscribers.SubscriberChangeEvent;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgIdentClient;
import com.vectrace.MercurialEclipse.commands.HgRootClient;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.IStorageMercurialRevision;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.cache.AbstractCache;
import com.vectrace.MercurialEclipse.team.cache.IncomingChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.team.cache.OutgoingChangesetCache;

public class MercurialSynchronizeSubscriber extends Subscriber implements Observer {

    private static final IncomingChangesetCache INCOMING_CACHE = IncomingChangesetCache
    .getInstance();

    private static final OutgoingChangesetCache OUTGOING_CACHE = OutgoingChangesetCache
    .getInstance();

    private static final MercurialStatusCache STATUS_CACHE = MercurialStatusCache
    .getInstance();

    private final ISynchronizationScope myScope;
    private IResource[] myRoots;
    private final HgRepositoryLocation repositoryLocation;
    private IResourceVariantComparator comparator;

    public MercurialSynchronizeSubscriber(
            ISynchronizationScope synchronizationScope,
            HgRepositoryLocation repositoryLocation) {
        this.myScope = synchronizationScope;
        this.repositoryLocation = repositoryLocation;
        STATUS_CACHE.addObserver(this);
    }

    @Override
    public String getName() {
        return Messages.getString("MercurialSynchronizeSubscriber.repoWatcher"); //$NON-NLS-1$
    }

    @Override
    public IResourceVariantComparator getResourceComparator() {
        if (comparator == null) {
            comparator = new MercurialResourceVariantComparator();
        }
        return comparator;
    }

    @Override
    public SyncInfo getSyncInfo(IResource resource) throws TeamException {
        if (!isInteresting(resource)) {
            return null;
        }
        // get newest outgoing changeset
        ChangeSet csOutgoing = OUTGOING_CACHE.getNewestOutgoingChangeSet(resource, repositoryLocation);

        IStorageMercurialRevision outgoingIStorage;
        try {
            IResourceVariant outgoing;
            // determine outgoing revision
            if (csOutgoing != null) {
                outgoingIStorage = new IStorageMercurialRevision(resource,
                        csOutgoing.getRevision().getRevision(),
                        csOutgoing.getChangeset(), csOutgoing);

                outgoing = new MercurialResourceVariant(outgoingIStorage);
            } else {
                // if outgoing != null it's our base, else we gotta construct one
                boolean exists = resource.exists();
                if (exists && !STATUS_CACHE.isAdded(resource.getProject(), resource.getLocation())
                        || (!exists && STATUS_CACHE.isRemoved(resource))) {

                    // Find current working directory changeset (not head)
                    HgRoot root = HgRootClient.getHgRoot(resource);
                    String nodeId = HgIdentClient.getCurrentChangesetId(root);

                    // try to get from cache (without loading)
                    csOutgoing = LocalChangesetCache.getInstance().getChangeSet(nodeId);

                    // okay, we gotta load the changeset via hg log
                    if (csOutgoing == null) {
                        csOutgoing = LocalChangesetCache.getInstance().getLocalChangeSet(resource, nodeId, true);
                    }

                    // construct base revision
                    outgoingIStorage = new IStorageMercurialRevision(resource,
                            csOutgoing.getChangesetIndex(), csOutgoing.getChangeset(), csOutgoing);

                    outgoing = new MercurialResourceVariant(outgoingIStorage);
                } else {
                    // new incoming file - no local available
                    outgoingIStorage = null;
                    outgoing = null;
                }
            }

            // determine incoming revision
            IStorageMercurialRevision incomingIStorage;
            // get newest incoming changeset
            ChangeSet csIncoming = INCOMING_CACHE.getNewestIncomingChangeSet(resource,
                    repositoryLocation);
            if (csIncoming != null) {
                incomingIStorage = getIncomingIStorage(resource, csIncoming);
            } else {
                // if no incoming revision, incmoing = base/outgoing
                incomingIStorage = outgoingIStorage;
            }

            IResourceVariant incoming;
            if (incomingIStorage != null) {
                incoming = new MercurialResourceVariant(incomingIStorage);
            } else {
                // neither base nor outgoing nor incoming revision
                incoming = null;
            }

            // now create the sync info object. everything may be null,
            // but resource and comparator
            SyncInfo info = new MercurialSyncInfo(resource, outgoing, incoming, getResourceComparator());

            info.init();
            return info;
        } catch (CoreException e) {
            MercurialEclipsePlugin.logError(e);
        }
        return null;

    }

    private boolean isInteresting(IResource resource) {
        return resource != null
                && RepositoryProvider.getProvider(resource.getProject(), MercurialTeamProvider.ID) != null
                && (isSupervised(resource) || (!resource.exists()));
    }

    private IStorageMercurialRevision getIncomingIStorage(IResource resource,
            ChangeSet csRemote) {
        IStorageMercurialRevision incomingIStorage = new IStorageMercurialRevision(
                resource, csRemote.getRevision().getRevision(), csRemote
                .getChangeset(), csRemote);
        return incomingIStorage;
    }

    @Override
    public boolean isSupervised(IResource resource) {
        return resource.getType() == IResource.FILE && STATUS_CACHE.isSupervised(resource);
    }

    @Override
    public IResource[] members(IResource resource) throws TeamException {
        Set<IResource> members = new HashSet<IResource>();
        Set<IResource> localMembers = STATUS_CACHE.getLocalMembers(resource);
        Set<IResource> outgoingMembers = OUTGOING_CACHE.getOutgoingMembers(
                resource, repositoryLocation);
        Set<IResource> incomingMembers = INCOMING_CACHE.getIncomingMembers(
                resource, repositoryLocation);

        if (localMembers.size() > 0) {
            members.addAll(localMembers);
        }
        if (outgoingMembers.size() > 0) {
            members.addAll(outgoingMembers);
        }
        if (incomingMembers.size() > 0) {
            members.addAll(incomingMembers);
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
        monitor
        .beginTask(
                Messages
                .getString("MercurialSynchronizeSubscriber.refreshing") + getName() + Messages.getString("MercurialSynchronizeSubscriber.refreshing.2") //$NON-NLS-1$ //$NON-NLS-2$
                + repositoryLocation + "...", 10); //$NON-NLS-1$
        monitor
        .subTask(Messages
                .getString("MercurialSynchronizeSubscriber.refreshingResources")); //$NON-NLS-1$
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

            AbstractCache.clearNodeMap();
            monitor
            .subTask(Messages
                    .getString("MercurialSynchronizeSubscriber.refreshingIncoming")); //$NON-NLS-1$
            Set<IResource> incomingMembers = null;
            Set<IResource> outgoingMembers = null;
            if (repositoryLocation != null) {
                INCOMING_CACHE.clear(repositoryLocation);
                INCOMING_CACHE.refreshIncomingChangeSets(project,
                        repositoryLocation);

                monitor.worked(1);
                if (monitor.isCanceled()) {
                    return;
                }
                monitor
                .subTask(Messages
                        .getString("MercurialSynchronizeSubscriber.refreshingOutgoing")); //$NON-NLS-1$
                OUTGOING_CACHE.clear(repositoryLocation);
                OUTGOING_CACHE.refreshOutgoingChangeSets(project,
                        repositoryLocation);
                monitor.worked(1);
                if (monitor.isCanceled()) {
                    return;
                }
                incomingMembers = INCOMING_CACHE.getIncomingMembers(resource,
                        repositoryLocation);
                outgoingMembers = OUTGOING_CACHE.getOutgoingMembers(resource,
                        repositoryLocation);
            }
            monitor
            .subTask(Messages
                    .getString("MercurialSynchronizeSubscriber.refreshingLocal")); //$NON-NLS-1$
            MercurialStatusCache.getInstance().refreshStatus(project, monitor);
            monitor.worked(1);
            if (monitor.isCanceled()) {
                return;
            }
            refreshed.add(project);

            Set<IResource> resourcesToRefresh = new HashSet<IResource>();
            Set<IResource> localMembers = STATUS_CACHE.getLocalMembers(resource);
            resourcesToRefresh.addAll(localMembers);

            if (incomingMembers != null) {
                resourcesToRefresh.addAll(incomingMembers);
            }
            if (outgoingMembers != null) {
                resourcesToRefresh.addAll(outgoingMembers);
            }

            for (IResource res : resourcesToRefresh) {
                changeEvents.add(new SubscriberChangeEvent(this,
                        ISubscriberChangeEvent.SYNC_CHANGED, res));
            }
            monitor.worked(1);
        }

        if (monitor.isCanceled()) {
            return;
        }
        monitor
        .subTask(Messages
                .getString("MercurialSynchronizeSubscriber.triggeringStatusCalc")); //$NON-NLS-1$
        fireTeamResourceChange(changeEvents
                .toArray(new ISubscriberChangeEvent[changeEvents.size()]));
        monitor.worked(1);
        monitor.done();
    }

    @Override
    public IResource[] roots() {
        if (myRoots == null) {
            if (myScope != null && myScope.getRoots() != null) {
                myRoots = myScope.getRoots();
            } else {
                myRoots = MercurialStatusCache.getInstance().getAllManagedProjects();
            }
        }
        return myRoots;
    }

    public void update(Observable o, Object arg) {
        if(!(arg instanceof Set<?>)){
            return;
        }
        List<ISubscriberChangeEvent> changeEvents = new ArrayList<ISubscriberChangeEvent>();
        Set<?> resources = (Set<?>) arg;
        IResource[] roots = roots();
        for (Object res : resources) {
            if(!(res instanceof IResource)) {
                continue;
            }
            IResource resource = (IResource)res;
            for (IResource root : roots) {
                if(root.contains(resource)) {
                    changeEvents.add(new SubscriberChangeEvent(this,
                            ISubscriberChangeEvent.SYNC_CHANGED, resource));
                    break;
                }
            }
        }
        if (changeEvents.size() > 0) {
            fireTeamResourceChange(changeEvents
                    .toArray(new ISubscriberChangeEvent[changeEvents.size()]));
        }
    }

}
