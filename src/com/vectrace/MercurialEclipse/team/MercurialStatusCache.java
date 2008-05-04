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
package com.vectrace.MercurialEclipse.team;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeUiJob;
import com.vectrace.MercurialEclipse.commands.HgIncomingClient;
import com.vectrace.MercurialEclipse.commands.HgLogClient;
import com.vectrace.MercurialEclipse.commands.HgOutgoingClient;
import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

/**
 * Caches the Mercurial Status of each file and offers methods for retrieving,
 * clearing and refreshing repository state.
 * 
 * @author Bastian Doetsch
 * 
 */
public class MercurialStatusCache extends Observable implements
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

    private static Map<IResource, SortedSet<ChangeSet>> localChangeSets;

    /**
     * The Map has the following structure: RepositoryLocation -> IResource ->
     * Changeset-Set
     */
    private static Map<String, Map<IResource, SortedSet<ChangeSet>>> outgoingChangeSets;

    private static Map<String, ChangeSet> nodeMap = new TreeMap<String, ChangeSet>();

    private static Map<IProject, Set<IResource>> projectResources;

    /**
     * The Map has the following structure: RepositoryLocation -> IResource ->
     * Changeset-Set
     */
    private static Map<String, Map<IResource, SortedSet<ChangeSet>>> incomingChangeSets;

    private boolean localUpdateInProgress = false;
    private boolean incomingUpdateInProgress = false;
    private boolean statusUpdateInProgress = false;
    private boolean outgoingUpdateInProgress = false;

    private static Comparator<ChangeSet> changeSetIndexComparator;

    private MercurialStatusCache() {
        changeSetIndexComparator = new ChangeSetIndexComparator();
        knownStatus = new HashSet<IProject>();
        localChangeSets = new HashMap<IResource, SortedSet<ChangeSet>>();
        projectResources = new HashMap<IProject, Set<IResource>>();
        incomingChangeSets = new HashMap<String, Map<IResource, SortedSet<ChangeSet>>>();
        outgoingChangeSets = new HashMap<String, Map<IResource, SortedSet<ChangeSet>>>();
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
        projectResources.clear();
        incomingChangeSets.clear();
        localChangeSets.clear();
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
     * Checks if incoming status for given project is known for any location.
     * 
     * @param project
     *            the project to be checked
     * @return true if known, false if not.
     */
    public boolean isIncomingStatusKnown(IProject project) {
        if (incomingUpdateInProgress) {
            synchronized (incomingChangeSets) {
                // wait...
            }
        }
        if (incomingChangeSets != null && incomingChangeSets.size() > 0) {
            for (Iterator<String> iterator = incomingChangeSets.keySet()
                    .iterator(); iterator.hasNext();) {
                Map<IResource, SortedSet<ChangeSet>> currLocMap = incomingChangeSets
                        .get(iterator.next());
                if (currLocMap != null && currLocMap.get(project) != null) {
                    return true;
                }
            }
        }
        return false;
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

    /**
     * Checks whether version is known.
     * 
     * @param objectResource
     *            the resource to be checked.
     * @return true if known, false if not.
     */
    public boolean isLocallyKnown(IResource objectResource) {
        if (localUpdateInProgress) {
            synchronized (localChangeSets) {
                // wait...
            }
        }
        return localChangeSets.containsKey(objectResource);
    }

    /**
     * Gets version for given resource.
     * 
     * @param objectResource
     *            the resource to get status for.
     * @return a String with version information.
     * @throws HgException
     */
    public ChangeSet getNewestLocalChangeSet(IResource objectResource)
            throws HgException {
        if (localUpdateInProgress) {
            synchronized (localChangeSets) {
                // waiting for update...
            }
        }
        SortedSet<ChangeSet> revisions = getLocalChangeSets(objectResource);
        if (revisions != null && revisions.size() > 0) {
            return revisions.last();
        }
        return null;
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

    public SortedSet<ChangeSet> getLocalChangeSets(IResource objectResource)
            throws HgException {
        SortedSet<ChangeSet> revisions = localChangeSets.get(objectResource);
        if (revisions == null) {
            if (objectResource.getType() != IResource.FOLDER
                    && isSupervised(objectResource)) {
                refreshAllLocalRevisions(objectResource.getProject());
                revisions = localChangeSets.get(objectResource);
            }
        }
        if (revisions != null) {
            return Collections.unmodifiableSortedSet(revisions);
        }
        return null;
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
        if (incomingUpdateInProgress) {
            synchronized (incomingChangeSets) {
                // wait
            }
        }
        Set<HgRepositoryLocation> repos = MercurialEclipsePlugin
                .getRepoManager().getAllProjectRepoLocations(
                        objectResource.getProject());

        SortedSet<ChangeSet> allChanges = new TreeSet<ChangeSet>();
        for (Iterator<HgRepositoryLocation> iterator = repos.iterator(); iterator
                .hasNext();) {
            HgRepositoryLocation hgRepositoryLocation = iterator.next();
            SortedSet<ChangeSet> repoChanges = getIncomingChangeSets(
                    objectResource, hgRepositoryLocation.getUrl());
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
            String repositoryLocation) throws HgException {
        if (incomingUpdateInProgress) {
            synchronized (incomingChangeSets) {
                // wait...
            }
        }
        Map<IResource, SortedSet<ChangeSet>> repoIncoming = incomingChangeSets
                .get(repositoryLocation);

        SortedSet<ChangeSet> revisions = null;
        if (repoIncoming != null) {
            revisions = repoIncoming.get(objectResource);
        }
        if (revisions == null) {
            refreshIncomingChangeSets(objectResource.getProject(),
                    repositoryLocation);
        }

        if (revisions != null) {
            return Collections.unmodifiableSortedSet(incomingChangeSets.get(
                    repositoryLocation).get(objectResource));
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
    public void refresh(final IProject project, IProgressMonitor monitor,
            String repositoryLocation) throws TeamException {
        /* hg status on project (all files) instead of per file basis */
        try {
            if (null != RepositoryProvider.getProvider(project,
                    MercurialTeamProvider.ID)
                    && project.isOpen()) {
                // set status
                refreshStatus(project, monitor);

                if (monitor != null) {
                    monitor.subTask("Updating status and version cache...");
                }
                try {
                    if (monitor != null) {
                        monitor.subTask("Loading local revisions...");
                    }
                    refreshAllLocalRevisions(project);
                    if (monitor != null) {
                        monitor.worked(1);
                    }
                    // incoming
                    if (repositoryLocation != null) {
                        if (monitor != null) {
                            monitor.subTask("Loading incoming revisions for "
                                    + repositoryLocation);
                        }
                        refreshIncomingChangeSets(project, repositoryLocation);
                        if (monitor != null) {
                            monitor.worked(1);
                        }

                        if (monitor != null) {
                            monitor.subTask("Loading outgoing revisions for "
                                    + repositoryLocation);
                        }
                        refreshOutgoingChangeSets(project, repositoryLocation);
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
                            MercurialEclipsePlugin.logWarning("couldn't add repository to location manager",e);
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
            }
        } catch (HgException e) {
            throw new TeamException(e.getMessage(), e);
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
     * Gets all incoming changesets by querying Mercurial and adds them to the
     * caches.
     * 
     * @param project
     * @param repositoryLocation
     * @throws HgException
     */
    public void refreshIncomingChangeSets(IProject project,
            String repositoryLocation) throws HgException {

        // check if mercurial is team provider and if we're working on an
        // open project
        if (null != RepositoryProvider.getProvider(project,
                MercurialTeamProvider.ID)
                && project.isOpen()) {

            // lock the cache till update is complete
            synchronized (incomingChangeSets) {
                try {
                    incomingUpdateInProgress = true;

                    addResourcesToCache(project, repositoryLocation,
                            incomingChangeSets, Direction.INCOMING);

                } finally {
                    incomingUpdateInProgress = false;
                }
            }
        }
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

    /**
     * @param repositoryLocation
     * @param outgoing
     *            flag, which direction should be queried.
     * @param changeSetMap
     * @throws HgException
     */
    private void addResourcesToCache(IProject project,
            String repositoryLocation,
            Map<String, Map<IResource, SortedSet<ChangeSet>>> changeSetMap,
            Direction direction) throws HgException {
        // load latest outgoing changesets from repository given in
        // parameter
        HgRepositoryLocation hgRepositoryLocation = MercurialEclipsePlugin
                .getRepoManager().getRepoLocation(repositoryLocation);

        // clear cache of old members
        final Map<IResource, SortedSet<ChangeSet>> removeMap = changeSetMap
                .get(repositoryLocation);

        if (removeMap != null) {
            removeMap.clear();
            changeSetMap.remove(repositoryLocation);
        }

        if (hgRepositoryLocation != null) {

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
                                    synchronized (nodeMap) {
                                        nodeMap.put(changeSet.toString(),
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

    private void addToProjectResources(IResource member) {
        if (member.getType() == IResource.PROJECT) {
            return;
        }
        Set<IResource> set = projectResources.get(member.getProject());
        if (set == null) {
            set = new HashSet<IResource>();
        }
        set.add(member);
        projectResources.put(member.getProject(), set);
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
            Set<IResource> resources = projectResources.get(resource);
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

    /**
     * Gets all resources that are changed in incoming changesets of given
     * repository, even resources not known in local workspace.
     * 
     * @param resource
     * @param repositoryLocation
     * @return
     */
    public IResource[] getIncomingMembers(IResource resource,
            String repositoryLocation) {
        if (incomingUpdateInProgress) {
            synchronized (incomingChangeSets) {
                // wait...
            }
        }
        Map<IResource, SortedSet<ChangeSet>> changeSets = incomingChangeSets
                .get(repositoryLocation);
        if (changeSets != null) {
            return changeSets.keySet()
                    .toArray(new IResource[changeSets.size()]);
        }
        return new IResource[0];
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

    public ChangeSet getNewestIncomingChangeSet(IResource resource,
            String repositoryLocation) throws HgException {

        if (incomingUpdateInProgress) {
            synchronized (incomingChangeSets) {
                // wait for update...
            }
        }

        if (isSupervised(resource)) {

            Map<IResource, SortedSet<ChangeSet>> repoMap = incomingChangeSets
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

    public ChangeSet getNewestOutgoingChangeSet(IResource resource,
            String repositoryLocation) throws HgException {

        if (outgoingUpdateInProgress) {
            synchronized (outgoingChangeSets) {
                // wait for update...
            }
        }

        if (isSupervised(resource)) {

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
     * Refreshes all local revisions, uses default limit of revisions to get,
     * e.g. the top 50 revisions.
     * 
     * If a resource version can't be found in the topmost revisions, the last
     * revisions of this file (10% of limit number) are obtained via additional
     * calls.
     * 
     * @param project
     * @throws HgException
     */
    public void refreshAllLocalRevisions(IProject project) throws HgException {
        this.refreshAllLocalRevisions(project, true);
    }

    /**
     * Refreshes all local revisions. If limit is set, it looks up the default
     * number of revisions to get and fetches the topmost till limit is reached.
     * 
     * If a resource version can't be found in the topmost revisions, the last
     * revisions of this file (10% of limit number) are obtained via additional
     * calls.
     * 
     * @param project
     * @param limit
     *            whether to limit or to have full project log
     * @throws HgException
     */
    public void refreshAllLocalRevisions(IProject project, boolean limit)
            throws HgException {
        if (null != RepositoryProvider.getProvider(project,
                MercurialTeamProvider.ID)
                && project.isOpen()) {
            int defaultLimit = 2000;
            try {
                String result = project
                        .getPersistentProperty(MercurialTeamProvider.QUALIFIED_NAME_DEFAULT_REVISION_LIMIT);
                if (result != null) {
                    defaultLimit = Integer.parseInt(result);
                }
            } catch (CoreException e) {
                MercurialEclipsePlugin.logError(e);
            }
            this.refreshAllLocalRevisions(project, limit, defaultLimit);
        }
    }

    /**
     * Refreshes all local revisions. If limit is set, it looks up the default
     * number of revisions to get and fetches the topmost till limit is reached.
     * 
     * If a resource version can't be found in the topmost revisions, the last
     * revisions of this file (10% of limit number) are obtained via additional
     * calls.
     * 
     * @param project
     * @param limit
     *            whether to limit or to have full project log
     * @param limitNumber
     *            if limit is set, how many revisions should be fetched
     * @throws HgException
     */
    public void refreshAllLocalRevisions(IProject project, boolean limit,
            int limitNumber) throws HgException {
        if (null != RepositoryProvider.getProvider(project,
                MercurialTeamProvider.ID)
                && project.isOpen()) {
            synchronized (localChangeSets) {
                try {
                    localUpdateInProgress = true;

                    Map<IResource, SortedSet<ChangeSet>> revisions = null;

                    if (limit) {
                        revisions = HgLogClient.getRecentProjectLog(project,
                                limitNumber);
                    } else {
                        revisions = HgLogClient.getCompleteProjectLog(project);

                    }

                    Set<IResource> resources = new HashSet<IResource>(Arrays
                            .asList(getLocalMembers(project)));
                    for (IResource resource : resources) {
                        localChangeSets.remove(resource);
                    }
                    localChangeSets.remove(project);

                    Set<IResource> concernedResources = new HashSet<IResource>();

                    concernedResources.add(project);
                    concernedResources.addAll(resources);
                    if (revisions != null && revisions.size() > 0) {
                        concernedResources.addAll(revisions.keySet());

                        for (Iterator<IResource> iter = revisions.keySet()
                                .iterator(); iter.hasNext();) {
                            IResource res = iter.next();
                            SortedSet<ChangeSet> changes = revisions.get(res);
                            // if changes for resource not in top 50, get at least
                            // 10%
                            if (changes == null && limit) {
                                changes = HgLogClient.getRecentProjectLog(res,
                                        limitNumber / 10).get(res);
                            }
                            // add changes to cache
                            if (changes != null && changes.size() > 0) {
                                if (isSupervised(res)) {
                                    localChangeSets.put(res, changes);
                                    addToNodeMap(changes);
                                }
                            }
                        }
                    }
                } finally {
                    localUpdateInProgress = false;
                }
            }
        }
    }

    /**
     * @param changes
     */
    private void addToNodeMap(SortedSet<ChangeSet> changes) {
        for (ChangeSet changeSet : changes) {
            synchronized (nodeMap) {
                nodeMap.put(changeSet.toString(), changeSet);
            }
        }
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
     * Gets Changeset by its identifier
     * 
     * @param changeSet
     *            string in format rev:nodeshort
     * @return
     */
    public ChangeSet getChangeSet(String changeSet) {
        if (localUpdateInProgress || incomingUpdateInProgress) {
            synchronized (nodeMap) {
                // wait
            }
        }
        return nodeMap.get(changeSet);
    }

    public ChangeSet getChangeSet(IResource res, int changesetIndex)
            throws HgException {
        if (localUpdateInProgress || incomingUpdateInProgress) {
            synchronized (nodeMap) {
                // wait
            }
        }
        SortedSet<ChangeSet> locals = getLocalChangeSets(res);
        List<ChangeSet> list = new ArrayList<ChangeSet>(locals);
        int index = Collections.binarySearch(list, new ChangeSet(
                changesetIndex, "", "", ""), changeSetIndexComparator);
        if (index >= 0) {
            return list.get(index);
        }
        return null;
    }

    @Deprecated
    public String[] getParentsChangeSet(IResource res, ChangeSet cs)
            throws HgException {
        SortedSet<ChangeSet> changeSets = getLocalChangeSets(res);
        String[] parents = cs.getParents();
        if (parents == null || parents.length == 0) {
            ChangeSet candidate = cs;
            int currIndex = cs.getChangesetIndex() - 1;
            boolean found = false;
            do {
                candidate = getChangeSet(res, currIndex);
                if (candidate == null) {
                    currIndex--;
                } else {
                    found = changeSets.contains(candidate);
                }
            } while (currIndex >= 0 && !found);
            if (candidate != null && candidate != cs) {
                return new String[] { candidate.toString() };
            }
        }
        return parents;
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
                    hgRepositoryLocation.getUrl());
            if (candidate != null) {
                changeSets.add(candidate);
            }
        }
        if (changeSets.size() > 0) {
            return changeSets.last();
        }
        return null;
    }

}
