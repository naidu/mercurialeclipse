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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.ISubscriberChangeEvent;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.subscribers.SubscriberChangeEvent;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgIdentClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.Branch;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.MercurialRevisionStorage;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.cache.IncomingChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.team.cache.OutgoingChangesetCache;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class MercurialSynchronizeSubscriber extends Subscriber /*implements Observer*/ {


    private static final LocalChangesetCache LOCAL_CACHE = LocalChangesetCache.getInstance();

    private static final IncomingChangesetCache INCOMING_CACHE = IncomingChangesetCache.getInstance();

    private static final OutgoingChangesetCache OUTGOING_CACHE = OutgoingChangesetCache.getInstance();

    private static final MercurialStatusCache STATUS_CACHE = MercurialStatusCache.getInstance();

    private final boolean debug;
    private final RepositorySynchronizationScope scope;
    private IResourceVariantComparator comparator;
    private final Semaphore sema;

    /** key is hg root, value is the *current* changeset of this root */
    private final Map<HgRoot, String> currentCsMap;

    private ISubscriberChangeEvent[] lastEvents;

    public MercurialSynchronizeSubscriber(RepositorySynchronizationScope synchronizationScope) {
        Assert.isNotNull(synchronizationScope);
        currentCsMap = new ConcurrentHashMap<HgRoot, String>();
        debug = MercurialEclipsePlugin.getDefault().isDebugging();
        scope = synchronizationScope;
        sema = new Semaphore(1, true);
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
    public SyncInfo getSyncInfo(IResource resource) {
        if (!isInteresting(resource)) {
            return null;
        }

        try {
            if(!sema.tryAcquire(60 * 5, TimeUnit.SECONDS)){
                // waiting didn't worked for us...
                return null;
            }
        } catch (InterruptedException e) {
            MercurialEclipsePlugin.logError(e);
            return null;
        }

        ChangeSet csOutgoing;
        try {
            // this can trigger a refresh and a call to the remote server...
            csOutgoing = OUTGOING_CACHE.getNewestChangeSet(resource, getRepo());
        } catch (HgException e) {
            MercurialEclipsePlugin.logError(e);
            return null;
        } finally {
            sema.release();
        }

        String currentBranch = MercurialTeamProvider.getCurrentBranch(resource);

        MercurialRevisionStorage outgoingIStorage;
        IResourceVariant outgoing;
        // determine outgoing revision
        if (csOutgoing != null) {
            if(!Branch.same(csOutgoing.getBranch(), currentBranch)){
                return null;
            }
            outgoingIStorage = new MercurialRevisionStorage(resource,
                    csOutgoing.getRevision().getRevision(),
                    csOutgoing.getChangeset(), csOutgoing);

            outgoing = new MercurialResourceVariant(outgoingIStorage);
        } else {
            // if outgoing != null it's our base, else we gotta construct one
            boolean exists = resource.exists();
            if (exists && !STATUS_CACHE.isAdded(resource.getLocation())
                    || (!exists && STATUS_CACHE.isRemoved(resource))) {

                try {
                    // Find current working directory changeset (not head)
                    HgRoot root = MercurialTeamProvider.getHgRoot(resource);

                    String nodeId = currentCsMap.get(root);
                    if(nodeId == null){
                        nodeId = HgIdentClient.getCurrentChangesetId(root);
                        currentCsMap.put(root, nodeId);
                    }

                    // try to get from cache (without loading)
                    csOutgoing = LOCAL_CACHE.getChangesetById(resource.getProject(), nodeId);

                    // okay, we gotta load the changeset via hg log
                    if (csOutgoing == null) {
                        csOutgoing = LOCAL_CACHE.getOrFetchChangeSetById(resource, nodeId);
                    }
                } catch (HgException e) {
                    MercurialEclipsePlugin.logError(e);
                    return null;
                }

                if(!Branch.same(csOutgoing.getBranch(), currentBranch)){
                    return null;
                }
                // construct base revision
                outgoingIStorage = new MercurialRevisionStorage(resource,
                        csOutgoing.getChangesetIndex(), csOutgoing.getChangeset(), csOutgoing);

                outgoing = new MercurialResourceVariant(outgoingIStorage);
            } else {
                // new incoming file - no local available
                outgoingIStorage = null;
                outgoing = null;
            }
        }

        // determine incoming revision get newest incoming changeset
        try {
            if(!sema.tryAcquire(60 * 5, TimeUnit.SECONDS)){
                // waiting didn't worked for us...
                return null;
            }
        } catch (InterruptedException e) {
            MercurialEclipsePlugin.logError(e);
            return null;
        }
        ChangeSet csIncoming;
        try {
            // this can trigger a refresh and a call to the remote server...
            csIncoming = INCOMING_CACHE.getNewestChangeSet(resource, getRepo());
        } catch (HgException e) {
            MercurialEclipsePlugin.logError(e);
            return null;
        } finally {
            sema.release();
        }

        MercurialRevisionStorage incomingIStorage;
        if (csIncoming != null) {
            if(!Branch.same(csIncoming.getBranch(), currentBranch)){
                return null;
            }
            boolean fileRemoved = csIncoming.isRemoved(resource);
            if(fileRemoved){
                incomingIStorage = null;
            } else {
                incomingIStorage = getIncomingIStorage(resource, csIncoming);
            }
        } else {
            // if no incoming revision, incoming = base/outgoing
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

        try {
            info.init();
            return info;
        } catch (CoreException e) {
            MercurialEclipsePlugin.logError(e);
            return null;
        }
    }

    private boolean isInteresting(IResource resource) {
        return resource != null
                && MercurialTeamProvider.isHgTeamProviderFor(resource.getProject())
                && (isSupervised(resource) || (!resource.exists()));
    }

    private MercurialRevisionStorage getIncomingIStorage(IResource resource,
            ChangeSet csRemote) {
        MercurialRevisionStorage incomingIStorage = new MercurialRevisionStorage(
                resource, csRemote.getRevision().getRevision(), csRemote
                .getChangeset(), csRemote);
        return incomingIStorage;
    }

    @Override
    public boolean isSupervised(IResource resource) {
        boolean result = resource.getType() == IResource.FILE && !resource.isTeamPrivateMember()
            /* && MercurialUtilities.isPossiblySupervised(resource)*/;
        if(!result){
            return false;
        }
        // fix for issue 10153: Resources ignored in .hgignore are still shown in Synchronize view
        if(STATUS_CACHE.isIgnored(resource)){
            return false;
        }
        return true;
    }

    @Override
    public IResource[] members(IResource resource) throws TeamException {
        return new IResource[0];
    }

    /**
     * @param flag one of {@link HgSubscriberScopeManager} constants, if the value is negative,
     * otherwise some depth hints from the Team API (which are ignored here).
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void refresh(IResource[] resources, int flag, IProgressMonitor monitor) throws TeamException {
        if (resources == null) {
            return;
        }

        Map<IProject, List<IResource>> byProject = ResourceUtils.groupByProject(Arrays.asList(resources));
        Set<IProject> projects = byProject.keySet();
        if(projects.isEmpty()){
            return;
        }

        Set<IResource> resourcesToRefresh = new HashSet<IResource>();

        HgRepositoryLocation repositoryLocation = getRepo();
        Set<IProject> repoLocationProjects = MercurialEclipsePlugin.getRepoManager()
                .getAllRepoLocationProjects(repositoryLocation);

        for (IProject project : projects) {
            if (!repoLocationProjects.contains(project)) {
                continue;
            }
            monitor.beginTask(getName(), 5);
            // clear caches in any case, but refresh them only if project exists
            boolean forceRefresh = project.exists();
            try {
                sema.acquire();
                if(debug) {
                    System.out.println("going to refresh local/in/out: " + project + ", depth: " + flag);
                }
                currentCsMap.remove(MercurialTeamProvider.getHgRoot(project));

                monitor.subTask(Messages.getString("MercurialSynchronizeSubscriber.refreshingLocal")); //$NON-NLS-1$
                refreshLocal(flag, monitor, project, forceRefresh);
                monitor.worked(1);
                if (monitor.isCanceled()) {
                    return;
                }
                monitor.subTask(Messages.getString("MercurialSynchronizeSubscriber.refreshingIncoming")); //$NON-NLS-1$
                refreshIncoming(flag, resourcesToRefresh, project, repositoryLocation, forceRefresh);
                monitor.worked(1);
                if (monitor.isCanceled()) {
                    return;
                }
                monitor.subTask(Messages.getString("MercurialSynchronizeSubscriber.refreshingOutgoing")); //$NON-NLS-1$
                refreshOutgoing(flag, resourcesToRefresh, project, repositoryLocation, forceRefresh);
                monitor.worked(1);
                if (monitor.isCanceled()) {
                    return;
                }
            } catch (InterruptedException e) {
                MercurialEclipsePlugin.logError(e);
            } finally {
                sema.release();
            }
        }

        List<ISubscriberChangeEvent> changeEvents = createEvents(resources, resourcesToRefresh);
        monitor.worked(1);
        if (monitor.isCanceled()) {
            return;
        }

        monitor.subTask(Messages.getString("MercurialSynchronizeSubscriber.triggeringStatusCalc")); //$NON-NLS-1$
        lastEvents = changeEvents.toArray(new ISubscriberChangeEvent[changeEvents.size()]);
        fireTeamResourceChange(lastEvents);
        monitor.worked(1);
        monitor.done();
    }

    private List<ISubscriberChangeEvent> createEvents(IResource[] resources,
            Set<IResource> resourcesToRefresh) {
        for (IResource resource : resources) {
            if(resource.getType() == IResource.FILE) {
                resourcesToRefresh.add(resource);
            } else {
                Set<IResource> localMembers = STATUS_CACHE.getLocalMembers(resource);
                resourcesToRefresh.addAll(localMembers);
            }
        }
        List<ISubscriberChangeEvent> changeEvents = new ArrayList<ISubscriberChangeEvent>();
        for (IResource res : resourcesToRefresh) {
            changeEvents.add(new SubscriberChangeEvent(this, ISubscriberChangeEvent.SYNC_CHANGED, res));
        }
        return changeEvents;
    }

    private void refreshLocal(int flag, IProgressMonitor monitor, IProject project,
            boolean forceRefresh) throws HgException {
        if(flag == HgSubscriberScopeManager.LOCAL || flag >= 0) {
            STATUS_CACHE.clear(project, false);
            if(forceRefresh) {
                STATUS_CACHE.refreshStatus(project, monitor);
            }
//            if(!forceRefresh) {
//                LOCAL_CACHE.clear(project, false);
//            }
//            if(forceRefresh) {
//                LOCAL_CACHE.refreshAllLocalRevisions(project, true);
//            }
        }
    }

    private void refreshIncoming(int flag, Set<IResource> resourcesToRefresh, IProject project,
            HgRepositoryLocation repositoryLocation, boolean forceRefresh) throws HgException {
        if(flag == HgSubscriberScopeManager.INCOMING || flag >= 0) {
            if(debug) {
                System.out.println("\nclear incoming: " + project + ", depth: " + flag);
            }
            INCOMING_CACHE.clear(repositoryLocation, project, false);
        }
        if(forceRefresh && flag != HgSubscriberScopeManager.OUTGOING){
            if(debug) {
                System.out.println("\nget incoming: " + project + ", depth: " + flag);
            }
            // this can trigger a refresh and a call to the remote server...
            Set<IResource> incomingMembers = INCOMING_CACHE.getMembers(project, repositoryLocation);
            resourcesToRefresh.addAll(incomingMembers);
        }
    }

    private void refreshOutgoing(int flag, Set<IResource> resourcesToRefresh, IProject project,
            HgRepositoryLocation repositoryLocation, boolean forceRefresh) throws HgException {
        if(flag == HgSubscriberScopeManager.OUTGOING || flag >= 0) {
            if(debug) {
                System.out.println("\nclear outgoing: " + project + ", depth: " + flag);
            }
            OUTGOING_CACHE.clear(repositoryLocation, project, false);
        }
        if(forceRefresh && flag != HgSubscriberScopeManager.INCOMING){
            if(debug) {
                System.out.println("\nget outgoing: " + project + ", depth: " + flag);
            }
            // this can trigger a refresh and a call to the remote server...
            Set<IResource> outgoingMembers = OUTGOING_CACHE.getMembers(project, repositoryLocation);
            resourcesToRefresh.addAll(outgoingMembers);
        }
    }

    private HgRepositoryLocation getRepo(){
        return scope.getRepositoryLocation();
    }

    @Override
    public IResource[] roots() {
        return scope.getRoots();
    }

    public void branchChanged(final IProject project){
        IResource[] roots = roots();
        boolean related = false;
        for (IResource resource : roots) {
            if(resource.getProject().equals(project)){
                related = true;
                break;
            }
        }
        if(!related){
            return;
        }
        Job job = new Job("Updating branch info for " + project.getName()){
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    currentCsMap.remove(MercurialTeamProvider.getHgRoot(project));
                } catch (HgException e) {
                    MercurialEclipsePlugin.logError(e);
                }
                if(lastEvents != null) {
                    fireTeamResourceChange(lastEvents);
                }
                return Status.OK_STATUS;
            }
        };
        job.schedule(100);
    }

    /**
     * Overriden to made it accessible from {@link HgSubscriberScopeManager#update(java.util.Observable, Object)}
     * {@inheritDoc}
     */
    @Override
    public void fireTeamResourceChange(ISubscriberChangeEvent[] deltas) {
        super.fireTeamResourceChange(deltas);
    }


}
