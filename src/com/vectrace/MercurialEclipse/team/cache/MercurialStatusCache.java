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

import java.io.File;
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
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.Team;
import org.eclipse.team.core.TeamException;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeWorkspaceJob;
import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.commands.HgClients;
import com.vectrace.MercurialEclipse.commands.HgIMergeClient;
import com.vectrace.MercurialEclipse.commands.HgResolveClient;
import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.FlaggedAdaptable;
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

        private final Map<IProject, Set<IResource>> removed;
        private final Map<IProject, Set<IResource>> changed;
        private final Map<IProject, Set<IResource>> added;
        private boolean completeStatus;

        /**
         * @param removed
         * @param changed
         * @param added
         */
        private ResourceDeltaVisitor(Map<IProject, Set<IResource>> removed,
                Map<IProject, Set<IResource>> changed,
                Map<IProject, Set<IResource>> added) {
            this.removed = removed;
            this.changed = changed;
            this.added = added;

            completeStatus = Boolean
                    .valueOf(
                            HgClients
                                    .getPreference(
                                            MercurialPreferenceConstants.RESOURCE_DECORATOR_COMPLETE_STATUS,
                                            "false")).booleanValue(); //$NON-NLS-1$
        }

        private IResource getResource(IResource res) {
            IResource myRes = res;
            if (completeStatus) {
                myRes = res.getProject();
            }
            return myRes;
        }

        public boolean visit(IResourceDelta delta) throws CoreException {
            IResource res = delta.getResource();
            // System.out.println("[ME-RV] Flags: "
            // + Integer.toHexString(delta.getFlags()));
            // System.out.println("[ME-RV] Kind: "
            // + Integer.toHexString(delta.getKind()));
            // System.out.println("[ME-RV] Resource: " + res.getFullPath());

            if (res.isAccessible()
                    && !Team.isIgnoredHint(res)
                    && (res.getType() == IResource.ROOT || RepositoryProvider
                            .getProvider(res.getProject(),
                                    MercurialTeamProvider.ID) != null)) {

                if (res.getType() == IResource.ROOT) {
                    return true;
                }

                IResource resource = getResource(res);
                IProject project = resource.getProject();
                Set<IResource> addSet = added.get(project);
                if (addSet == null) {
                    addSet = new HashSet<IResource>();
                }

                Set<IResource> removeSet = removed.get(project);
                if (removeSet == null) {
                    removeSet = new HashSet<IResource>();
                }

                Set<IResource> changeSet = changed.get(project);
                if (changeSet == null) {
                    changeSet = new HashSet<IResource>();
                }
                if (!res.isTeamPrivateMember() && !res.isDerived()
                        && res.getType() == IResource.FILE) {
                    int flag = delta.getFlags() & INTERESTING_CHANGES;
                    // System.out.println("[ME-RV] " + res.getFullPath()
                    // + " interesting? Result: "
                    // + Integer.toHexString(flag));
                    switch (delta.getKind()) {
                    case IResourceDelta.ADDED:
                        addSet.add(resource);
                        added.put(project, addSet);
                        break;
                    case IResourceDelta.CHANGED:
                        if (isSupervised(res) && flag != 0) {
                            changeSet.add(resource);
                            changed.put(project, changeSet);
                        }
                        break;
                    case IResourceDelta.REMOVED:
                        if (isSupervised(res)) {
                            removeSet.add(getResource(res));
                            removed.put(project, removeSet);
                        }
                        break;
                    }
                }
                // System.out
                // .println("[ME-RV] Descending to next level (returning with true)");
                return true;
            }
            // System.out.println("[ME-RV] Not descending (returning with false)");
            return false;
        }
    }

    private final class ChangeSetIndexComparator implements
            Comparator<ChangeSet> {
        public int compare(ChangeSet arg0, ChangeSet arg1) {
            return arg0.getChangesetIndex() - arg1.getChangesetIndex();
        }
    }

    private final class MemberStatusVisitor implements IResourceVisitor {

        private BitSet bitSet;
        private IResource parent;

        public MemberStatusVisitor(IResource parent, BitSet bitSet) {
            this.bitSet = bitSet;
            this.parent = parent;
        }

        public boolean visit(IResource resource) throws CoreException {
            if (parent.isTeamPrivateMember()) {
                resource.setTeamPrivateMember(true);
                return false;
            }

            if (parent.isDerived()) {
                resource.setDerived(true);
                return false;
            }

            if (!resource.equals(parent)) {
                BitSet memberBitSet = statusMap.get(resource.getLocation());
                if (memberBitSet != null) {
                    bitSet.or(memberBitSet);
                }
            }
            return true;
        }

    }
    
    /*
     * Initialization On Demand Holder idiom, thread-safe and instance will not be created
     * until getInstance is called in the outer class.
     */
    private static final class MercurialStatusCacheHolder {
        public static final MercurialStatusCache instance = new MercurialStatusCache();
    }

    public final static int BIT_IGNORE = 0;
    public final static int BIT_CLEAN = 1;
    public final static int BIT_DELETED = 2;
    public final static int BIT_REMOVED = 3;
    public final static int BIT_UNKNOWN = 4;
    public final static int BIT_ADDED = 5;
    public final static int BIT_MODIFIED = 6;
    public final static int BIT_IMPOSSIBLE = 7;
    public final static int BIT_CONFLICT = 8;

    public static final char CHAR_MODIFIED = 'M';
    public static final char CHAR_ADDED = 'A';
    public static final char CHAR_UNKNOWN = '?';
    public static final char CHAR_CLEAN = 'C';
    public static final char CHAR_IGNORED = 'I';
    public static final char CHAR_REMOVED = 'R';
    public static final char CHAR_DELETED = '!';

    protected int INTERESTING_CHANGES = IResourceDelta.CONTENT
            | IResourceDelta.MOVED_FROM | IResourceDelta.MOVED_TO
            | IResourceDelta.OPEN | IResourceDelta.REPLACED
            | IResourceDelta.TYPE;

    /** Used to store the last known status of a resource */
    private static Map<IPath, BitSet> statusMap = new HashMap<IPath, BitSet>();

    /** Used to store which projects have already been parsed */
    private static Set<IProject> knownStatus;

    private static Map<IProject, ReentrantLock> locks = new HashMap<IProject, ReentrantLock>();

    private MercurialStatusCache() {
        AbstractCache.changeSetIndexComparator = new ChangeSetIndexComparator();
        knownStatus = new HashSet<IProject>();
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
        // new RefreshStatusJob("Initializing Mercurial plugin...").schedule();
    }

    public static final MercurialStatusCache getInstance() {
        return MercurialStatusCacheHolder.instance;
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
        locks.clear();
        getInstance().setChanged();
        getInstance().notifyObservers(knownStatus);
    }

    /**
     * Sets lock on HgRoot of given resource
     * 
     * @param resource
     * @return
     * @throws HgException
     */
    public ReentrantLock getLock(IResource resource) throws HgException {
        if (!resource.isAccessible() || resource.isDerived()
                || resource.isLinked()
                || !MercurialUtilities.hgIsTeamProviderFor(resource, false)) {
            return new ReentrantLock();
        }
        // if (resource.isAccessible()) {
        // hgRoot = new Path(MercurialTeamProvider.getHgRoot(resource)
        // .getAbsolutePath());
        // } else {
        // hgRoot = new Path(resource.getProject().getLocation().toOSString());
        // }
        ReentrantLock lock = locks.get(resource.getProject());
        if (lock == null) {
            lock = new ReentrantLock();
            locks.put(resource.getProject(), lock);
        }
        return lock;

    }

    /**
     * Checks if status for given project is known.
     * 
     * @param project
     *            the project to be checked
     * @return true if known, false if not.
     * @throws HgException
     */
    public boolean isStatusKnown(IProject project) throws HgException {
        ReentrantLock lock = getLock(project);
        try {
            lock.lock();
            return knownStatus.contains(project);
        } finally {
            lock.unlock();
        }
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
     * @throws HgException
     */
    public BitSet getStatus(IResource resource) throws HgException {
        ReentrantLock lock = getLock(resource);
        try {
            lock.lock();
            return statusMap.get(resource.getLocation());
        } finally {
            lock.unlock();
        }
    }

    public boolean isSupervised(IResource resource) throws HgException {
        return MercurialUtilities.hgIsTeamProviderFor(resource, false)
                && isSupervised(resource.getProject(), resource.getLocation());
    }

    public boolean isSupervised(IResource resource, IPath path)
            throws HgException {
        Assert.isNotNull(resource);
        Assert.isNotNull(path);

        // check for Eclipse ignore settings
        if (Team.isIgnoredHint(resource)) {
            return false;
        }

        ReentrantLock lock = getLock(resource);

        if (resource.getProject().isAccessible()
                && null != RepositoryProvider.getProvider(
                        resource.getProject(), MercurialTeamProvider.ID)) {
            try {
                lock.lock();
                if (path.equals(resource.getProject().getLocation())) {
                    return true;
                }
                BitSet status = statusMap.get(path);
                if (status != null) {
                    switch (status.length() - 1) {
                    case MercurialStatusCache.BIT_IGNORE:
                    case MercurialStatusCache.BIT_UNKNOWN:
                        File fileSystemResource = path.toFile();
                        if (fileSystemResource.isDirectory()
                                && status.length() > 1) {
                            // a directory is still supervised if one of the
                            // following bits is set
                            boolean supervised = status.get(BIT_ADDED)
                                    || status.get(BIT_CLEAN)
                                    || status.get(BIT_DELETED)
                                    || status.get(BIT_MODIFIED)
                                    || status.get(BIT_REMOVED);
                            return supervised;
                        }
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

    public boolean isAdded(IResource resource, IPath path) throws HgException {
        Assert.isNotNull(resource);
        Assert.isNotNull(path);
        if (null != RepositoryProvider.getProvider(resource.getProject(),
                MercurialTeamProvider.ID)) {
            // if (path.equals(project.getLocation())) {
            // // FIX ME: This breaks on new projects without changelog
            // return false;
            // }
            ReentrantLock lock = getLock(resource);
            try {
                lock.lock();
                BitSet status = statusMap.get(path);
                if (status != null) {
                    switch (status.length() - 1) {
                    case MercurialStatusCache.BIT_ADDED:
                        File fileSystemResource = path.toFile();
                        if (fileSystemResource.isDirectory()
                                && status.length() > 1) {
                            // a directory is still supervised if one of the
                            // following bits is set
                            boolean supervised = status.get(BIT_CLEAN)
                                    || status.get(BIT_DELETED)
                                    || status.get(BIT_MODIFIED)
                                    || status.get(BIT_REMOVED)
                                    || status.get(BIT_CONFLICT)
                                    || status.get(BIT_IGNORE);
                            return !supervised;
                        }
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
        Assert.isNotNull(res);
        if (monitor != null) {
            monitor
                    .subTask(Messages
                            .getString("MercurialStatusCache.Refreshing") + res.getName()); //$NON-NLS-1$
        }

        if (null != RepositoryProvider.getProvider(res.getProject(),
                MercurialTeamProvider.ID)
                && res.getProject().isOpen()) {
            Set<IResource> changed;
            if (res.isTeamPrivateMember() || res.isDerived()) {
                return;
            }
            ReentrantLock lock = getLock(res);
            try {
                lock.lock();
                // members should contain folders and project, so we clear
                // status for files, folders and project
                IResource[] resources = getLocalMembers(res);
                if (monitor != null) {
                    monitor.worked(1);
                }
                for (IResource resource : resources) {
                    statusMap.remove(resource.getLocation());
                }
                if (monitor != null) {
                    monitor.worked(1);
                }
                statusMap.remove(res.getLocation());
                String output = HgStatusClient.getStatus(res);
                if (monitor != null) {
                    monitor.worked(1);
                }
                File root = AbstractClient.getHgRoot(res);
                changed = parseStatus(root, res, output);
                if (monitor != null) {
                    monitor.worked(1);
                }
                try {
                    String mergeNode = HgStatusClient.getMergeStatus(res);
                    res.getProject().setPersistentProperty(
                            ResourceProperties.MERGING, mergeNode);
                    if (monitor != null) {
                        monitor.worked(1);
                    }
                } catch (CoreException e) {
                    throw new HgException(
                            Messages
                                    .getString("MercurialStatusCache.FailedToRefreshMergeStatus"), e); //$NON-NLS-1$
                }
            } finally {
                lock.unlock();
            }
            notifyChanged(changed);
        }

        if (monitor != null) {
            monitor.worked(1);
        }
    }

    /**
     * @param res
     * @throws HgException
     */
    private void checkForConflict(final IResource res) throws HgException {
        List<FlaggedAdaptable> status;
        if (HgResolveClient.checkAvailable()) {
            status = HgResolveClient.list(res);
        } else {
            status = HgIMergeClient.getMergeStatus(res);
        }
        for (FlaggedAdaptable flaggedAdaptable : status) {
            IFile file = (IFile) flaggedAdaptable.getAdapter(IFile.class);
            if (flaggedAdaptable.getFlag() == 'U') {
                addConflict(file);
            } else {
                removeConflict(file);
            }
        }
    }

    /**
     * @param root
     * @param res
     * @param output
     * @param ctrParent
     * @throws HgException
     */
    private Set<IResource> parseStatus(File root, IResource res, String output)
            throws HgException {
        if (res.getType() == IResource.PROJECT) {
            knownStatus.add(res.getProject());
        }
        // we need the project for performance reasons - gotta hand it to
        // addToProjectResources
        IProject project = res.getProject();
        Set<IResource> changed = new HashSet<IResource>();
        Scanner scanner = new Scanner(output);
        while (scanner.hasNext()) {
            String status = scanner.next();
            String localName = scanner.nextLine().trim();

            IResource member = convertRepoRelPath(root, project, localName);

            // doesn't belong to our project (can happen if root is above
            // project level)
            if (member == null) {
                continue;
            }

            BitSet bitSet = new BitSet();
            if (Team.isIgnoredHint(member)) {
                bitSet.set(BIT_IGNORE);
            } else {
                bitSet.set(getBitIndex(status.charAt(0)));
                changed.add(member);
            }
            statusMap.put(member.getLocation(), bitSet);

            if (member.getType() == IResource.FILE
                    && getBitIndex(status.charAt(0)) != BIT_IGNORE
                    && !Team.isIgnoredHint(member)) {
                addToProjectResources(project, member);
            }

            changed.addAll(setStatusToAncestors(member, bitSet));
        }
        // add conflict status if merging
        try {
            if (res.getProject().getPersistentProperty(
                    ResourceProperties.MERGING) != null) {
                checkForConflict(res);
            }
        } catch (Exception e) {
            MercurialEclipsePlugin.logError(e);
        }
        return changed;
    }

    /**
     * @param upperLimitAncestor
     * @param resource
     * @param resourceBitSet
     * @return
     */
    private Set<IResource> setStatusToAncestors(IResource resource,
            BitSet resourceBitSet) {
        // ancestors
        IProject project = resource.getProject();
        Set<IResource> ancestors = new HashSet<IResource>();
        boolean computeDeep = isComputeDeepStatus();
        boolean completeStatus = Boolean
                .valueOf(
                        HgClients
                                .getPreference(
                                        MercurialPreferenceConstants.RESOURCE_DECORATOR_COMPLETE_STATUS,
                                        "false")).booleanValue(); //$NON-NLS-1$

        for (IResource parent = resource.getParent(); parent != null
                && parent != resource.getProject().getParent(); parent = parent
                .getParent()) {
            BitSet parentBitSet = statusMap.get(parent.getLocation());
            BitSet cloneBitSet = (BitSet) resourceBitSet.clone();
            if (parentBitSet != null) {
                if (resource.getType() != IResource.PROJECT && computeDeep
                        && !completeStatus) {
                    IResourceVisitor visitor = new MemberStatusVisitor(parent,
                            cloneBitSet);
                    try {
                        if (parent.isAccessible() && !parent.isDerived()
                                && !parent.isTeamPrivateMember()) {
                            parent.accept(visitor, IResource.DEPTH_ONE, false);
                        }
                    } catch (CoreException e) {
                        MercurialEclipsePlugin.logError(e);
                    }
                } else {
                    cloneBitSet.or(parentBitSet);
                }
            }
            statusMap.put(parent.getLocation(), cloneBitSet);
            ancestors.add(parent);
            addToProjectResources(project, parent);
        }
        return ancestors;
    }

    /**
     * @return
     */
    private boolean isComputeDeepStatus() {
        boolean computeDeep = Boolean
                .valueOf(
                        HgClients
                                .getPreference(
                                        MercurialPreferenceConstants.RESOURCE_DECORATOR_COMPUTE_DEEP_STATUS,
                                        "false")).booleanValue(); //$NON-NLS-1$
        return computeDeep;
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
            String msg = Messages
                    .getString("MercurialStatusCache.UnknownStatus") + status + "'"; //$NON-NLS-1$ //$NON-NLS-2$
            MercurialEclipsePlugin.logWarning(msg, new HgException(msg));
            return BIT_IMPOSSIBLE;
        }
    }

    /**
     * Converts the given bit index to the status character Mercurial uses.
     * 
     * @param bitIndex
     * @return
     */
    public char getStatusChar(int bitIndex) {
        switch (bitIndex) {
        case BIT_DELETED:
            return CHAR_DELETED;
        case BIT_REMOVED:
            return CHAR_REMOVED;
        case BIT_IGNORE:
            return CHAR_IGNORED;
        case BIT_CLEAN:
            return CHAR_CLEAN;
        case BIT_UNKNOWN:
            return CHAR_UNKNOWN;
        case BIT_ADDED:
            return CHAR_ADDED;
        case BIT_MODIFIED:
            return CHAR_MODIFIED;
        default:
            String msg = Messages
                    .getString("MercurialStatusCache.UnknownStatus") + bitIndex + "'"; //$NON-NLS-1$ //$NON-NLS-2$
            MercurialEclipsePlugin.logWarning(msg, new HgException(msg));
            return BIT_IMPOSSIBLE;
        }
    }

    /**
     * Returns the status character used by Mercurial that applies to this
     * resource
     * 
     * @param resource
     *            the resource to query the status for
     * @return ! (deleted), R (removed), I (ignored), C (clean), ? (unknown), A
     *         (added) or M (modified)
     * @throws HgException
     */
    public char getStatusChar(IResource resource) throws HgException {
        BitSet status = getStatus(resource);
        char statusChar = getStatusChar(status.length() - 1);
        return statusChar;
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
     * @throws HgException
     */
    public boolean isStatusKnown(IResource resource) throws HgException {
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
     * @see
     * org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org
     * .eclipse.core.resources.IResourceChangeEvent)
     */
    public void resourceChanged(IResourceChangeEvent event) {
        // FIXME: this is strange: one edit in a file triggers two post_change
        // events,
        // auto-build triggers another two. how to filter duplicate events?
        if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
            try {
                IResourceDelta delta = event.getDelta();

                final Map<IProject, Set<IResource>> changed = new HashMap<IProject, Set<IResource>>();
                final Map<IProject, Set<IResource>> added = new HashMap<IProject, Set<IResource>>();
                final Map<IProject, Set<IResource>> removed = new HashMap<IProject, Set<IResource>>();

                IResourceDeltaVisitor visitor = new ResourceDeltaVisitor(
                        removed, changed, added);

                // walk tree
                delta.accept(visitor);
                final IWorkspace workspace = ResourcesPlugin.getWorkspace();
                final Set<IProject> changedProjects = new HashSet<IProject>(
                        changed.keySet());
                changedProjects.addAll(added.keySet());
                changedProjects.addAll(removed.keySet());
                for (final IProject project : changedProjects) {

                    final IWorkspaceRunnable job = new IWorkspaceRunnable() {

                        public void run(IProgressMonitor monitor)
                                throws CoreException {

                            // now process gathered changes (they are in the
                            // lists)
                            try {
                                Set<IResource> addedSet = added.get(project);
                                Set<IResource> removedSet = removed
                                        .get(project);
                                Set<IResource> changedSet = changed
                                        .get(project);
                                Set<IResource> resources = new HashSet<IResource>();
                                if (changedSet != null) {
                                    resources.addAll(changedSet);
                                }
                                if (addedSet != null) {
                                    resources.addAll(addedSet);
                                }
                                if (removedSet != null) {
                                    resources.addAll(removedSet);
                                }

                                if (resources.size() > NUM_CHANGED_FOR_COMPLETE_STATUS) {
                                    monitor
                                            .beginTask(
                                                    Messages
                                                            .getString("MercurialStatusCache.RefreshingProjects"), //$NON-NLS-1$
                                                    2);
                                    monitor
                                            .subTask(Messages
                                                    .getString("MercurialStatusCache.RefreshingProject") //$NON-NLS-1$
                                                    + project.getName()
                                                    + Messages
                                                            .getString("MercurialStatusCache....")); //$NON-NLS-1$
                                    refreshStatus(project, monitor);
                                    monitor.worked(1);
                                } else {
                                    monitor
                                            .beginTask(
                                                    Messages
                                                            .getString("MercurialStatusCache.RefreshingResources..."), 4); //$NON-NLS-1$
                                    // changed
                                    monitor
                                            .subTask(Messages
                                                    .getString("MercurialStatusCache.RefreshingChangedResources...")); //$NON-NLS-1$
                                    if (changedSet != null
                                            && changedSet.size() > 0) {
                                        refreshStatus(changedSet);
                                    }
                                    monitor.worked(1);

                                    // added
                                    monitor
                                            .subTask(Messages
                                                    .getString("MercurialStatusCache.RefreshingAddedResources...")); //$NON-NLS-1$
                                    if (addedSet != null && addedSet.size() > 0) {
                                        refreshStatus(addedSet);
                                    }
                                    monitor.worked(1);

                                    // removed not used right now
                                    // refreshStatus(removed);
                                }
                                // notify observers
                                monitor
                                        .subTask(Messages
                                                .getString("MercurialStatusCache.AddingResourcesForDecoratorUpdate...")); //$NON-NLS-1$
                                monitor.worked(1);
                                monitor
                                        .subTask(Messages
                                                .getString("MercurialStatusCache.TriggeringDecoratorUpdate...")); //$NON-NLS-1$
                                notifyChanged(resources);
                                monitor.worked(1);
                            } finally {
                                monitor.done();
                            }
                        }
                    };
                    final ISchedulingRule rule = workspace.getRuleFactory()
                            .modifyRule(project);
                    SafeWorkspaceJob wsJob = new SafeWorkspaceJob(Messages
                            .getString("MercurialStatusCache.RefreshStatus...")) { //$NON-NLS-1$
                        @Override
                        protected IStatus runSafe(IProgressMonitor monitor) {
                            try {
                                workspace.run(job, rule,
                                        IWorkspace.AVOID_UPDATE, monitor);
                            } catch (CoreException e) {
                                MercurialEclipsePlugin.logError(e);
                                return new Status(IStatus.ERROR,
                                        MercurialEclipsePlugin.ID, e
                                                .getLocalizedMessage(), e);
                            }
                            return super.runSafe(monitor);
                        }
                    };
                    wsJob.setRule(rule);
                    wsJob.schedule(200);
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
        if (resources == null) {
            return;
        }
        // TODO: group batches by repo root
        String pref = HgClients.getPreference(
                MercurialPreferenceConstants.STATUS_BATCH_SIZE, String
                        .valueOf(STATUS_BATCH_SIZE));

        int batchSize = STATUS_BATCH_SIZE;
        if (pref.length() > 0) {
            try {
                batchSize = Integer.parseInt(pref);
            } catch (NumberFormatException e) {
                MercurialEclipsePlugin
                        .logWarning(
                                Messages
                                        .getString("MercurialStatusCache.BatchSizeForStatusCommandNotCorrect."), e); //$NON-NLS-1$
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
            if (!resource.isTeamPrivateMember()) {
                currentBatch.add(resource);
            }
            if (currentBatch.size() % batchSize == 0 || !iterator.hasNext()) {
                // call hg with batch
                File root = AbstractClient.getHgRoot(resource);
                String output = HgStatusClient.getStatus(resource.getLocation()
                        .toFile(), currentBatch);
                parseStatus(root, resource, output);
                currentBatch.clear();
            }
        }
    }

    /**
     * Determines Members of given resource without adding itself.
     * 
     * @param resource
     * @return
     * @throws HgException
     */
    public IResource[] getLocalMembers(IResource resource) throws HgException {
        ReentrantLock lock = getLock(resource);
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
                    IResource foundMember = container.findMember(memberPath,
                            false);
                    if (foundMember != null
                            && foundMember.getLocation().equals(memberPath)) {
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
     * @throws HgException
     */
    public void clear(IProject project) throws HgException {
        ReentrantLock lock = getLock(project);
        try {
            lock.lock();
            Set<IResource> members = getMembers(project);
            for (IResource resource : members) {
                statusMap.remove(resource.getLocation());
            }
            statusMap.remove(project.getLocation());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Sets conflict marker on resource status
     * 
     * @param local
     * @throws HgException
     */
    public void addConflict(IResource local) throws HgException {
        BitSet status = getStatus(local);
        status.set(BIT_CONFLICT);
        setStatusToAncestors(local, status);
        notifyChanged(local);
    }

    /**
     * Removes conflict marker on resource status
     * 
     * @param local
     * @throws HgException
     */
    public void removeConflict(IResource local) throws HgException {
        BitSet status = getStatus(local);
        status.clear(BIT_CONFLICT);
        setStatusToAncestors(local, status);
        notifyChanged(local);
    }

    /**
     * @param resources
     */
    @Override
    protected void notifyChanged(Set<IResource> resources) {
        setChanged();
        notifyObservers(resources);
    }

}
