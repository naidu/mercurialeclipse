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

import java.io.File;
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
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.IStorageMercurialRevision;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.cache.AbstractCache;
import com.vectrace.MercurialEclipse.team.cache.IncomingChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.team.cache.OutgoingChangesetCache;

public class MercurialSynchronizeSubscriber extends Subscriber {

    private static final IncomingChangesetCache INCOMING_CACHE = IncomingChangesetCache
            .getInstance();

    private static final OutgoingChangesetCache OUTGOING_CACHE = OutgoingChangesetCache
            .getInstance();

    private static final MercurialStatusCache STATUS_CACHE = MercurialStatusCache
            .getInstance();

    private ISynchronizationScope myScope;
    private IResource[] myRoots;
    private HgRepositoryLocation repositoryLocation;
    private IResourceVariantComparator comparator;

    public MercurialSynchronizeSubscriber(
            ISynchronizationScope synchronizationScope,
            HgRepositoryLocation repositoryLocation) {
        this.myScope = synchronizationScope;
        this.repositoryLocation = repositoryLocation;
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
        try {
            if (resource != null
                    && null != RepositoryProvider.getProvider(resource
                            .getProject(), MercurialTeamProvider.ID)
                    && resource.getProject().isAccessible()
                    && (isSupervised(resource) || (!resource.exists()))) {

                // get newest outgoing changeset
                ChangeSet csOutgoing = OUTGOING_CACHE
                        .getNewestOutgoingChangeSet(resource,
                                repositoryLocation);

                // get newest incoming changeset
                ChangeSet csIncoming = INCOMING_CACHE
                        .getNewestIncomingChangeSet(resource,
                                repositoryLocation);

                IResourceVariant outgoing;
                IResourceVariant incoming;

                // determine outgoing revision
                IStorageMercurialRevision outgoingIStorage;
                if (csOutgoing != null) {
                    outgoingIStorage = new IStorageMercurialRevision(resource,
                            csOutgoing.getRevision().getRevision() + "", //$NON-NLS-1$
                            csOutgoing.getChangeset(), csOutgoing);

                    outgoing = new MercurialResourceVariant(outgoingIStorage);
                } else {
                    // if outgoing != null it's our base, else we gotta
                    // construct one
                    if (resource.exists()
                            && !STATUS_CACHE.isAdded(resource.getProject(),
                                    resource.getLocation())) {

                        // Find current working directory changeset (not head)
                        File root = new File(HgRootClient.getHgRoot(resource));
                        String nodeId = HgIdentClient
                                .getCurrentChangesetId(root);

                        // try to get from cache (without loading)
                        csOutgoing = LocalChangesetCache.getInstance()
                                .getChangeSet(nodeId);

                        // okay, we gotta load the changeset via hg log
                        if (csOutgoing == null) {
                            csOutgoing = LocalChangesetCache.getInstance()
                                    .getLocalChangeSet(resource, nodeId);
                        }

                        // construct base revision
                        outgoingIStorage = new IStorageMercurialRevision(
                                resource, String.valueOf(csOutgoing
                                        .getChangesetIndex()), csOutgoing
                                        .getChangeset(), csOutgoing);

                        outgoing = new MercurialResourceVariant(
                                outgoingIStorage);
                    } else {
                        // new incoming file - no local available
                        outgoingIStorage = null;
                        outgoing = null;
                    }
                }

                // determine incoming revision
                IStorageMercurialRevision incomingIStorage;
                if (csIncoming != null) {
                    incomingIStorage = getIncomingIStorage(resource, csIncoming);
                } else {
                    // if no incoming revision, incmoing = base/outgoing
                    incomingIStorage = outgoingIStorage;
                }

                if (incomingIStorage != null) {
                    incoming = new MercurialResourceVariant(incomingIStorage);
                } else {
                    // neither base nor outgoing nor incoming revision
                    incoming = null;
                }

                // now create the sync info object. everything may be null,
                // but resource and comparator

                SyncInfo info = new MercurialSyncInfo(resource, outgoing,
                        incoming, getResourceComparator());

                info.init();
                return info;

            }
        } catch (Exception e) {
            MercurialEclipsePlugin.logError(e);
        }
        return null;

    }

    /**
     * @param resource
     * @param csRemote
     * @return
     */
    private IStorageMercurialRevision getIncomingIStorage(IResource resource,
            ChangeSet csRemote) {
        IStorageMercurialRevision incomingIStorage = new IStorageMercurialRevision(
                resource, csRemote.getRevision().getRevision() + "", csRemote //$NON-NLS-1$
                        .getChangeset(), csRemote);
        return incomingIStorage;
    }

    @Override
    public boolean isSupervised(IResource resource) throws TeamException {

        return STATUS_CACHE.isSupervised(resource)
                && resource.getType() == IResource.FILE;
    }

    @Override
    public IResource[] members(IResource resource) throws TeamException {
        Set<IResource> members = new HashSet<IResource>();
        IResource[] localMembers = STATUS_CACHE.getLocalMembers(resource);
        IResource[] outgoingMembers = OUTGOING_CACHE.getOutgoingMembers(
                resource, repositoryLocation);
        IResource[] incomingMembers = INCOMING_CACHE.getIncomingMembers(
                resource, repositoryLocation);

        if (localMembers != null && localMembers.length > 0) {
            members.addAll(Arrays.asList(localMembers));
        }

        if (outgoingMembers != null && outgoingMembers.length > 0) {
            members.addAll(Arrays.asList(outgoingMembers));
        }

        if (incomingMembers != null && incomingMembers.length > 0) {
            members.addAll(Arrays.asList(incomingMembers));
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
            IResource[] incomingMembers = null;
            IResource[] outgoingMembers = null;
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
            IResource[] localMembers = STATUS_CACHE.getLocalMembers(resource);

            Set<IResource> resourcesToRefresh = new HashSet<IResource>();

            if (localMembers != null) {
                resourcesToRefresh.addAll(Arrays.asList(localMembers));
            }
            if (incomingMembers != null) {
                resourcesToRefresh.addAll(Arrays.asList(incomingMembers));
            }
            if (outgoingMembers != null) {
                resourcesToRefresh.addAll(Arrays.asList(outgoingMembers));
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
        super.fireTeamResourceChange(changeEvents
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
                myRoots = ResourcesPlugin.getWorkspace().getRoot()
                        .getProjects();
            }
        }
        return myRoots;
    }

    /**
     * @return
     */
    public static Subscriber getInstance() {
        MercurialSynchronizeSubscriber sub = new MercurialSynchronizeSubscriber();
        return sub;
    }

    /**
     * 
     */
    private MercurialSynchronizeSubscriber() {
    }

}
