/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Software Balm Consulting Inc (Peter Hunnisett <peter_hge at softwarebalm dot com>) - some updates
 *     StefanC                   - large contribution
 *     Jerome Negre              - fixing folders' state
 *     Bastian Doetsch	         - extraction from DecoratorStatus + additional methods
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team.cache;

import java.net.MalformedURLException;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeUiJob;
import com.vectrace.MercurialEclipse.SafeWorkspaceJob;
import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

/**
 * Caches the Mercurial Status of each file and offers methods for retrieving,
 * clearing and refreshing repository state.
 * 
 * @author Bastian Doetsch
 * 
 */
public class MercurialStatusCache extends AbstractCache implements
        IResourceChangeListener {

    private final class ChangeSetIndexComparator implements
            Comparator<ChangeSet> {
        public int compare(ChangeSet arg0, ChangeSet arg1) {
            return arg0.getChangesetIndex() - arg1.getChangesetIndex();
        }
    }

    public final static int BIT_IGNORE = 0;
    public final static int BIT_CLEAN = 1;
    public final static int BIT_DELETED = 2;
    public final static int BIT_REMOVED = 3;
    public final static int BIT_UNKNOWN = 4;
    public final static int BIT_ADDED = 5;
    public final static int BIT_MODIFIED = 6;
    public final static int BIT_IMPOSSIBLE = 7;

    private static MercurialStatusCache instance;

    /** Used to store the last known status of a resource */
    private static Map<IResource, BitSet> statusMap = new HashMap<IResource, BitSet>();

    /** Used to store which projects have already been parsed */
    private static Set<IProject> knownStatus;

    private boolean statusUpdateInProgress = false;

    private MercurialStatusCache() {
        AbstractCache.changeSetIndexComparator = new ChangeSetIndexComparator();
        knownStatus = new HashSet<IProject>();
        AbstractCache.projectResources = new HashMap<IProject, Set<IResource>>();
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
        new SafeUiJob("Initializing Mercurial") {
            @Override
            protected IStatus runSafe(IProgressMonitor monitor) {
                try {
                    monitor.beginTask(
                            "Obtaining Mercurial Status information.", 5);
                    refreshStatus(monitor);
                } catch (TeamException e) {
                    MercurialEclipsePlugin.logError(e);
                }
                return super.runSafe(monitor);
            }
        }.schedule();
    }

    public static MercurialStatusCache getInstance() {
        if (instance == null) {
            instance = new MercurialStatusCache();
        }
        return instance;
    }

    /**
     * Clears the known status of all resources and projects. and calls for an
     * update of decoration
     */
    public synchronized void clear() {
        /*
         * While this clearing of status is a "naive" implementation, it is
         * simple.
         */
        statusMap.clear();
        knownStatus.clear();
        AbstractCache.projectResources.clear();
        setChanged();
        notifyObservers(knownStatus.toArray(new IProject[knownStatus.size()]));
    }

    /**
     * Checks if status for given project is known.
     * 
     * @param project
     *            the project to be checked
     * @return true if known, false if not.
     */
    public boolean isStatusKnown(IProject project) {
        if (statusUpdateInProgress) {
            synchronized (statusMap) {
                // wait...
            }
        }
        return knownStatus.contains(project);
    }

    /**
     * Gets the status of the given resource from cache. The returned BitSet
     * contains a BitSet of the status flags set.
     * 
     * The flags correspond to the BIT_* constants in this class.
     * 
     * @param objectResource
     *            the resource to get status for.
     * @return the BitSet with status flags.
     */
    public BitSet getStatus(IResource objectResource) {
        if (statusUpdateInProgress) {
            synchronized (statusMap) {
                // wait...
            }
        }
        return statusMap.get(objectResource);
    }

    public boolean isSupervised(IResource resource) {
        BitSet status = getStatus(resource);
        if (status != null) {
            switch (status.length() - 1) {
            case MercurialStatusCache.BIT_IGNORE:
            case MercurialStatusCache.BIT_UNKNOWN:
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Refreshes local repository status. No refresh of incoming changesets.
     * 
     * @param project
     * @throws TeamException
     */
    public void refresh(final IProject project) throws TeamException {
        refresh(project, null, null);
    }

    /**
     * Refreshes sync status of given project by questioning Mercurial.
     * 
     * @param project
     *            the project to refresh
     * @param monitor
     *            the progress monitor
     * @param repositoryLocation
     *            the remote repository to get the incoming changesets from. If
     *            null, no incoming changesets will be retrieved.
     * @throws TeamException
     */
    public void refresh(final IProject project, final IProgressMonitor moni,
            final String repositoryLocation) throws TeamException {
        /* hg status on project (all files) instead of per file basis */
        if (null != RepositoryProvider.getProvider(project,
                MercurialTeamProvider.ID)
                && project.isOpen()) {
            // set status
            new SafeWorkspaceJob("Refreshing caches and status.") {

                @Override
                protected IStatus runSafe(IProgressMonitor monitor) {                    
                    try {
                        refreshStatus(project, monitor);
                    } catch (HgException e1) {
                        MercurialEclipsePlugin.logError(e1);
                    }

                    if (monitor != null) {
                        monitor.subTask("Updating status and version cache...");
                    }
                    try {
                        if (monitor != null) {
                            monitor.subTask("Loading local revisions...");
                        }
                        LocalChangesetCache.getInstance()
                                .refreshAllLocalRevisions(project);
                        if (monitor != null) {
                            monitor.worked(1);
                        }
                        // incoming
                        if (repositoryLocation != null) {
                            if (monitor != null) {
                                monitor
                                        .subTask("Loading incoming revisions for "
                                                + repositoryLocation);
                            }
                            IncomingChangesetCache.getInstance()
                                    .refreshIncomingChangeSets(project,
                                            repositoryLocation);
                            if (monitor != null) {
                                monitor.worked(1);
                            }

                            if (monitor != null) {
                                monitor
                                        .subTask("Loading outgoing revisions for "
                                                + repositoryLocation);
                            }
                            OutgoingChangesetCache.getInstance()
                                    .refreshOutgoingChangeSets(project,
                                            repositoryLocation);
                            if (monitor != null) {
                                monitor.worked(1);
                            }

                            if (monitor != null) {
                                monitor
                                        .subTask("Adding remote repository to project repositories...");
                            }
                            try {
                                MercurialEclipsePlugin.getRepoManager()
                                        .addRepoLocation(
                                                project,
                                                new HgRepositoryLocation(
                                                        repositoryLocation));
                            } catch (MalformedURLException e) {
                                MercurialEclipsePlugin
                                        .logWarning(
                                                "couldn't add repository to location manager",
                                                e);
                            }
                            if (monitor != null) {
                                monitor.worked(1);
                            }
                        }
                        setChanged();
                        notifyObservers(project);
                    } catch (HgException e) {
                        MercurialEclipsePlugin.logError(e);
                    }
                    return super.runSafe(monitor);
                }
            }.schedule();
        }
    }

    /**
     * @param project
     * @throws HgException
     */
    public void refreshStatus(final IResource res, IProgressMonitor monitor)
            throws HgException {
        try {
            if (monitor != null) {
                monitor.beginTask("Refreshing " + res.getName(), 50);
            }
            if (null != RepositoryProvider.getProvider(res.getProject(),
                    MercurialTeamProvider.ID)
                    && res.getProject().isOpen()) {
                synchronized (statusMap) {
                    statusUpdateInProgress = true;
                    // members should contain folders and project, so we clear
                    // status for files, folders and project
                    IResource[] resources = getLocalMembers(res);
                    for (IResource resource : resources) {
                        statusMap.remove(resource);
                    }
                    statusMap.remove(res);
                    String output = HgStatusClient.getStatus(res);
                    parseStatus(res, output);
                }
            }
        } finally {
            statusUpdateInProgress = false;
        }
        setChanged();
        notifyObservers(res);
    }

    /**
     * @param res
     * @param output
     * @param ctrParent
     */
    private void parseStatus(IResource res, String output) {
        if (res.getType() == IResource.PROJECT) {
            knownStatus.add(res.getProject());
        }
        Scanner scanner = new Scanner(output);
        while (scanner.hasNext()) {
            String status = scanner.next();
            String localName = scanner.nextLine();

            IResource member = res.getProject().getFile(localName.trim());

            BitSet bitSet = new BitSet();
            bitSet.set(getBitIndex(status.charAt(0)));
            statusMap.put(member, bitSet);

            if (member.getType() == IResource.FILE
                    && getBitIndex(status.charAt(0)) != BIT_IGNORE) {
                addToProjectResources(member);
            }

            // ancestors
            for (IResource parent = member.getParent(); parent != res
                    .getParent(); parent = parent.getParent()) {
                BitSet parentBitSet = statusMap.get(parent);
                if (parentBitSet != null) {
                    bitSet = (BitSet) bitSet.clone();
                    bitSet.or(parentBitSet);
                }
                statusMap.put(parent, bitSet);
                addToProjectResources(parent);
            }
        }
    }

    public int getBitIndex(char status) {
        switch (status) {
        case '!':
            return BIT_DELETED;
        case 'R':
            return BIT_REMOVED;
        case 'I':
            return BIT_IGNORE;
        case 'C':
            return BIT_CLEAN;
        case '?':
            return BIT_UNKNOWN;
        case 'A':
            return BIT_ADDED;
        case 'M':
            return BIT_MODIFIED;
        default:
            MercurialEclipsePlugin.logWarning("Unknown status: '" + status
                    + "'", null);
            return BIT_IMPOSSIBLE;
        }
    }

    /**
     * Refreshes the sync status for each project in Workspace by questioning
     * Mercurial. No refresh of incoming changesets.
     * 
     * @throws TeamException
     *             if status check encountered problems.
     */
    public void refresh() throws TeamException {
        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
                .getProjects();
        for (IProject project : projects) {
            refresh(project, null, null);
        }
    }

    /**
     * Refreshes the status for each project in Workspace by questioning
     * Mercurial.
     * 
     * @throws TeamException
     *             if status check encountered problems.
     */
    public void refreshStatus(IProgressMonitor monitor) throws TeamException {
        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
                .getProjects();
        for (IProject project : projects) {
            refreshStatus(project, monitor);
        }
    }

    /**
     * Checks whether Status of given resource is known.
     * 
     * @param resource
     *            the resource to be checked
     * @return true if known, false if not
     */
    public boolean isStatusKnown(IResource resource) {
        return getStatus(resource) != null;
    }

    /**
     * Gets all Projects managed by Mercurial whose status is known.
     * 
     * @return an IProject[] of the projects
     */
    public IProject[] getAllManagedProjects() {
        return knownStatus.toArray(new IProject[knownStatus.size()]);
    }

    public void resourceChanged(IResourceChangeEvent event) {
        // only refresh after a change - we aren't interested in build outputs,
        // are we?
        if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
            // workspace childs
            IResourceDelta[] wsChildren = event.getDelta()
                    .getAffectedChildren();
            for (IResourceDelta wsChild : wsChildren) {

                // update whole project :-(. else we'd have to walk the project
                // tree.
                final IResource res = wsChild.getResource();
                if (null != RepositoryProvider.getProvider(res.getProject(),
                        MercurialTeamProvider.ID)
                        && res.getProject().isOpen()) {

                    new SafeUiJob("Refreshing status of resource "
                            + res.getName()) {
                        @Override
                        protected IStatus runSafe(IProgressMonitor monitor) {
                            try {
                                monitor.beginTask(
                                        "Starting to refresh status of "
                                                + res.getName(), 10);
                                refreshStatus(res, monitor);
                                return super.runSafe(monitor);
                            } catch (HgException e) {
                                MercurialEclipsePlugin.logError(e);
                                return new Status(IStatus.ERROR,
                                        MercurialEclipsePlugin.ID,
                                        "Couldn't refresh status of "
                                                + res.getName() + ". E: "
                                                + e.getMessage());
                            }
                        }
                    }.schedule();

                }
            }
        }

    }

    /**
     * Determines Members of given resource without adding itself.
     * 
     * @param resource
     * @return
     */
    public IResource[] getLocalMembers(IResource resource) {
        if (statusUpdateInProgress) {
            synchronized (statusMap) {
                // wait...
            }
        }
        IContainer container = (IContainer) resource;

        Set<IResource> members = new HashSet<IResource>();

        switch (resource.getType()) {
        case IResource.FILE:
            break;
        case IResource.PROJECT:
            Set<IResource> resources = AbstractCache.projectResources
                    .get(resource);
            if (resources != null) {
                members.addAll(resources);
                members.remove(resource);
            }
            break;
        case IResource.FOLDER:
            for (Iterator<IResource> iterator = new HashMap<IResource, BitSet>(
                    statusMap).keySet().iterator(); iterator.hasNext();) {
                IResource member = iterator.next();
                if (member.equals(resource)) {
                    continue;
                }

                IResource foundMember = container.findMember(member.getName());
                if (foundMember != null && foundMember.equals(member)) {
                    members.add(member);
                }
            }
        }
        members.remove(resource);
        return members.toArray(new IResource[members.size()]);
    }

}
