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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.team.ResourceProperties;

/**
 * Caches the Mercurial Status of each file and offers methods for retrieving,
 * clearing and refreshing repository state.
 * 
 * @author Bastian Doetsch
 * 
 */
public class MercurialStatusCache extends AbstractCache implements
        IResourceChangeListener {

    private static final int STATUS_BATCH_SIZE = 10;
    private static final int NUM_CHANGED_FOR_COMPLETE_STATUS = 50;

    /**
     * @author bastian
     * 
     */
    private final class ResourceDeltaVisitor implements IResourceDeltaVisitor {

        private final Set<IResource> removed;
        private final Set<IResource> changed;
        private final Set<IResource> added;
        private boolean deepComputation;

        /**
         * @param removed
         * @param changed
         * @param added
         */
        private ResourceDeltaVisitor(Set<IResource> removed,
                Set<IResource> changed, Set<IResource> added) {
            this.removed = removed;
            this.changed = changed;
            this.added = added;
            deepComputation = Boolean
                    .valueOf(MercurialUtilities
                            .getPreference(
                                    MercurialPreferenceConstants.RESOURCE_DECORATOR_DEEP_COMPUTATION,
                                    "false"));
        }

        private IResource getResource(IResource res) {
            IResource myRes = res;
            if (deepComputation) {
                myRes = res.getProject();
            }
            return myRes;
        }

        public boolean visit(IResourceDelta delta) throws CoreException {
            IResource res = delta.getResource();
            if (res.isAccessible()
                    && res.getProject().isOpen()
                    && RepositoryProvider.getProvider(res.getProject(),
                            MercurialTeamProvider.ID) != null) {

                switch (delta.getKind()) {
                case IResourceDelta.ADDED:
                    if (res.getType() == IResource.FILE) {
                        added.add(getResource(res));
                    }
                    break;
                case IResourceDelta.CHANGED:
                    if ((isSupervised(res) && res.getType() == IResource.FILE)) {
                        changed.add(getResource(res));
                    }
                    break;
                case IResourceDelta.REMOVED:
                    if ((isSupervised(res) && res.getType() == IResource.FILE)) {
                        removed.add(getResource(res));
                    }
                    break;
                }
                return true;
            }
            return false;
        }
    }

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
    private static Map<IPath, BitSet> statusMap = new HashMap<IPath, BitSet>();

    /** Used to store which projects have already been parsed */
    private static Set<IProject> knownStatus;

    private static Map<IPath, ReentrantLock> locks = new HashMap<IPath, ReentrantLock>();

    private MercurialStatusCache() {
        AbstractCache.changeSetIndexComparator = new ChangeSetIndexComparator();
        knownStatus = new HashSet<IProject>();
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
        // new RefreshStatusJob("Initializing Mercurial plugin...").schedule();
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
        setChanged();
        notifyObservers(knownStatus);
    }

    public ReentrantLock getLock(IPath path) {
        ReentrantLock lock = locks.get(path);
        if (lock == null) {
            lock = new ReentrantLock();
            locks.put(path, lock);
        }
        return lock;
    }

    /**
     * Checks if status for given project is known.
     * 
     * @param project
     *            the project to be checked
     * @return true if known, false if not.
     */
    public boolean isStatusKnown(IProject project) {
        ReentrantLock lock = getLock(project.getLocation());
        try {
            lock.lock();
            return knownStatus.contains(project);
        } finally {
            lock.unlock();
        }
    }

    public BitSet getStatus(IResource res) {
        return getStatus(res.getLocation());
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
    public BitSet getStatus(IPath path) {
        ReentrantLock lock = getLock(path);
        try {
            lock.lock();
            return statusMap.get(path);
        } finally {
            lock.unlock();
        }
    }

    public boolean isSupervised(IResource resource) {
        return isSupervised(resource.getProject(), resource.getLocation());
    }

    public boolean isSupervised(IProject project, IPath path) {
        ReentrantLock lock = getLock(project.getLocation());

        if (null != RepositoryProvider.getProvider(project,
                MercurialTeamProvider.ID)) {
            try {
                lock.lock();
                if (path.equals(project.getLocation())) {
                    return true;
                }
                BitSet status = getStatus(path);
                if (status != null) {
                    switch (status.length() - 1) {
                    case MercurialStatusCache.BIT_IGNORE:
                    case MercurialStatusCache.BIT_UNKNOWN:
                        return false;
                    }
                    return true;
                }
            } finally {
                lock.unlock();
            }
        }
        return false;

    }

    public boolean isAdded(IProject project, IPath path) {
        if (null != RepositoryProvider.getProvider(project,
                MercurialTeamProvider.ID)) {
            if (path.equals(project.getLocation())) {
                return false;
            }
            ReentrantLock lock = getLock(path);
            try {
                lock.lock();
                BitSet status = getStatus(path);
                if (status != null) {
                    switch (status.length() - 1) {
                    case MercurialStatusCache.BIT_ADDED:
                        return true;
                    }
                    return false;
                }
            } finally {
                lock.unlock();
            }
        }
        return false;
    }

    /**
     * Refreshes local repository status. No refresh of changesets.
     * 
     * @param project
     * @throws TeamException
     */
    public void refresh(final IProject project) throws TeamException {
        refreshStatus(project, new NullProgressMonitor());
    }

    /**
     * @param project
     * @throws HgException
     */
    public void refreshStatus(final IResource res, IProgressMonitor monitor)
            throws HgException {
        if (monitor != null) {
            monitor.beginTask("Refreshing " + res.getName(), 50);
        }
        if (null != RepositoryProvider.getProvider(res.getProject(),
                MercurialTeamProvider.ID)
                && res.getProject().isOpen()) {
            ReentrantLock lock = getLock(res.getLocation());
            try {
                lock.lock();
                // members should contain folders and project, so we clear
                // status for files, folders and project
                IResource[] resources = getLocalMembers(res);
                for (IResource resource : resources) {
                    statusMap.remove(resource.getLocation());
                }
                statusMap.remove(res.getLocation());
                String output = HgStatusClient.getStatus(res);
                parseStatus(res, output);
                try {
                    res.getProject().setPersistentProperty(
                            ResourceProperties.MERGING,
                            HgStatusClient.getMergeStatus(res));
                } catch (CoreException e) {
                    throw new HgException("Failed to refresh merge status", e);
                }
            } finally {
                lock.unlock();
            }
        }
        notifyChanged(res);
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
            statusMap.put(member.getLocation(), bitSet);

            if (member.getType() == IResource.FILE
                    && getBitIndex(status.charAt(0)) != BIT_IGNORE) {
                addToProjectResources(member);
            }

            // ancestors
            for (IResource parent = member.getParent(); parent != null
                    && parent != res.getParent(); parent = parent.getParent()) {
                BitSet parentBitSet = statusMap.get(parent.getLocation());
                if (parentBitSet != null) {
                    bitSet = (BitSet) bitSet.clone();
                    bitSet.or(parentBitSet);
                }
                statusMap.put(parent.getLocation(), bitSet);
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

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
     */
    public void resourceChanged(IResourceChangeEvent event) {
        // FIXME: this is strange: one edit in a file triggers two post_change
        // events,
        // auto-build triggers another two. how to filter duplicate events?
        if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
            try {
                for (IResourceDelta delta : event.getDelta()
                        .getAffectedChildren()) {

                    final Set<IResource> changed = new HashSet<IResource>();
                    final Set<IResource> added = new HashSet<IResource>();
                    final Set<IResource> removed = new HashSet<IResource>();

                    IResourceDeltaVisitor visitor = new ResourceDeltaVisitor(
                            removed, changed, added);

                    // walk tree
                    delta.accept(visitor);

                    // now process gathered changes (they are in the lists)
                    if (changed.size() + added.size() + removed.size() > NUM_CHANGED_FOR_COMPLETE_STATUS) {
                        changed.addAll(added);
                        changed.addAll(removed);
                        Set<IProject> projects = new HashSet<IProject>();
                        for (IResource resource : changed) {
                            projects.add(resource.getProject());
                        }
                        for (IProject project : projects) {
                            refreshStatus(project, new NullProgressMonitor());
                        }
                    } else {
                        // changed
                        refreshStatus(changed);

                        // added
                        refreshStatus(added);

                        // removed not used right now
                        // refreshStatus(removed);
                    }
                    // notify observers
                    Set<IResource> resources = new HashSet<IResource>();
                    resources.addAll(changed);
                    resources.addAll(added);
                    resources.addAll(removed);
                    notifyChanged(resources);

                }
            } catch (CoreException e) {
                MercurialEclipsePlugin.logError(e);
            }
        }
    }

    /**
     * Refreshes Status of resources in batches
     * 
     * @param resources
     * @return
     * @throws HgException
     */
    private void refreshStatus(final Set<IResource> resources)
            throws HgException {

        String pref = MercurialUtilities.getPreference(
                MercurialPreferenceConstants.STATUS_BATCH_SIZE, String
                        .valueOf(STATUS_BATCH_SIZE));

        int batchSize = STATUS_BATCH_SIZE;
        if (pref.length() > 0) {
            try {
                batchSize = Integer.parseInt(pref);
            } catch (NumberFormatException e) {
                MercurialEclipsePlugin.logWarning(
                        "Batch size for status command not correct.", e);
            }
        }
        List<IResource> currentBatch = new ArrayList<IResource>();
        for (Iterator<IResource> iterator = resources.iterator(); iterator
                .hasNext();) {
            IResource resource = iterator.next();

            // project status wanted, no batching needed
            if (resource.getType() == IResource.PROJECT) {
                try {
                    refreshStatus(resource, null);
                } catch (Exception e) {
                    MercurialEclipsePlugin.logError(e);
                    throw new HgException(e.getMessage(), e);
                }
                continue;
            }

            // status for single resource is batched
            currentBatch.add(resource);
            if (currentBatch.size() % batchSize == 0 || !iterator.hasNext()) {
                // call hg with batch
                String output = HgStatusClient.getStatus(resource.getProject(),
                        currentBatch);
                parseStatus(resource.getProject(), output);
                currentBatch.clear();
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
        ReentrantLock lock = getLock(resource.getLocation());
        try {
            lock.lock();

            Set<IResource> members = new HashSet<IResource>();

            switch (resource.getType()) {
            case IResource.FILE:
                break;
            case IResource.PROJECT:
                synchronized (projectResources) {
                    Set<IResource> resources = AbstractCache.projectResources
                            .get(resource);
                    if (resources != null) {
                        members.addAll(resources);
                        members.remove(resource);
                    }
                }
                break;
            case IResource.FOLDER:
                for (Iterator<IPath> iterator = new HashMap<IPath, BitSet>(
                        statusMap).keySet().iterator(); iterator.hasNext();) {
                    IPath memberPath = iterator.next();
                    if (memberPath.equals(resource.getLocation())) {
                        continue;
                    }

                    IContainer container = (IContainer) resource;
                    IResource foundMember = container.findMember(memberPath, false);
                    if (foundMember != null && foundMember.getLocation().equals(memberPath)) {
                        members.add(foundMember);
                    }
                }
            }
            members.remove(resource);
            return members.toArray(new IResource[members.size()]);
        } finally {
            lock.unlock();
        }
    }

    /**
     * @param project
     */
    public void clear(IProject project) {
        ReentrantLock lock = getLock(project.getLocation());
        try {
            lock.lock();
            Set<IResource> members = getMembers(project);
            for (IResource resource : members) {
                statusMap.remove(resource);
            }
            statusMap.remove(project);
        } finally {
            lock.unlock();
        }
    }

}
