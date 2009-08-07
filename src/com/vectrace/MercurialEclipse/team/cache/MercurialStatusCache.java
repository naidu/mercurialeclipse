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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.Team;
import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeWorkspaceJob;
import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.commands.HgClients;
import com.vectrace.MercurialEclipse.commands.HgResolveClient;
import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.commands.extensions.HgIMergeClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.FlaggedAdaptable;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.team.ResourceProperties;

/**
 * Caches the Mercurial Status of each file and offers methods for retrieving, clearing and refreshing repository state.
 *
 * @author Bastian Doetsch
 *
 */
public class MercurialStatusCache extends AbstractCache implements IResourceChangeListener {

    private static final int STATUS_BATCH_SIZE = 10;
    private static final int NUM_CHANGED_FOR_COMPLETE_STATUS = 50;

    /**
     * @author Andrei
     */
    private final class ProjectUpdateJob extends SafeWorkspaceJob {

        private final Set<IResource> removedSet;
        private final Set<IResource> changedSet;
        private final IProject project;
        private final Set<IResource> addedSet;

        private ProjectUpdateJob(Set<IResource> removedSet, Set<IResource> changedSet,
                IProject project, Set<IResource> addedSet) {
            super(Messages
                    .getString("MercurialStatusCache.RefreshStatus..."));
            this.removedSet = removedSet;
            this.changedSet = changedSet;
            this.project = project;
            this.addedSet = addedSet;
            setRule(ResourcesPlugin.getWorkspace().getRuleFactory().modifyRule(project));
        }


        @Override
        protected IStatus runSafe(IProgressMonitor monitor) {
            // now process gathered changes (they are in the
            // lists)
            try {
                updateProject(monitor);
            } catch (CoreException e) {
                MercurialEclipsePlugin.logError(e);
                return new Status(IStatus.ERROR, MercurialEclipsePlugin.ID, e.getLocalizedMessage(), e);
            }  finally {
                monitor.done();
            }
            return super.runSafe(monitor);
        }


        private void updateProject(IProgressMonitor monitor) throws HgException {
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
                monitor.beginTask(Messages.getString("MercurialStatusCache.RefreshingProjects"), //$NON-NLS-1$
                        2);
                monitor.subTask(Messages.getString("MercurialStatusCache.RefreshingProject") //$NON-NLS-1$
                        + project.getName() + Messages.getString("MercurialStatusCache....")); //$NON-NLS-1$
                refreshStatus(project, monitor);
                monitor.worked(1);
            } else {
                monitor.beginTask(
                        Messages.getString("MercurialStatusCache.RefreshingResources..."), 4); //$NON-NLS-1$
                // changed
                monitor.subTask(Messages
                        .getString("MercurialStatusCache.RefreshingChangedResources...")); //$NON-NLS-1$
                if (changedSet != null && changedSet.size() > 0) {
                    refreshStatus(changedSet, project);
                }
                monitor.worked(1);

                // added
                monitor.subTask(Messages
                        .getString("MercurialStatusCache.RefreshingAddedResources...")); //$NON-NLS-1$
                if (addedSet != null && addedSet.size() > 0) {
                    refreshStatus(addedSet, project);
                }
                monitor.worked(1);

                // removed not used right now
                // refreshStatus(removed);
            }
            // notify observers
            monitor.subTask(Messages
                    .getString("MercurialStatusCache.AddingResourcesForDecoratorUpdate...")); //$NON-NLS-1$
            monitor.worked(1);
            monitor
            .subTask(Messages
                    .getString("MercurialStatusCache.TriggeringDecoratorUpdate...")); //$NON-NLS-1$
            notifyChanged(resources);
            monitor.worked(1);
        }
    }

    private final class MemberStatusVisitor implements IResourceVisitor {

        private final BitSet bitSet;
        private final IResource parent;

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
     * Initialization On Demand Holder idiom, thread-safe and instance will not be created until getInstance is called
     * in the outer class.
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
    public static final char CHAR_UNRESOLVED = 'U';
    public static final char CHAR_RESOLVED = 'R';

    protected static final int INTERESTING_CHANGES = IResourceDelta.CONTENT | IResourceDelta.MOVED_FROM | IResourceDelta.MOVED_TO
    | IResourceDelta.OPEN | IResourceDelta.REPLACED | IResourceDelta.TYPE;

    private static final Object DUMMY = new Object();

    /** Used to store the last known status of a resource */
    private final Map<IPath, BitSet> statusMap = new ConcurrentHashMap<IPath, BitSet>();

    /** Used to store which projects have already been parsed */
    private final Map<IProject, Object> knownStatus = new ConcurrentHashMap<IProject, Object>();

    /* Access to this map must be protected with a synchronized lock itself */
    private final Map<IProject, ReentrantLock> locks = new HashMap<IProject, ReentrantLock>();

    private final Map<IProject, Set<IResource>> projectResources = new HashMap<IProject, Set<IResource>>();
    private boolean computeDeepStatus;
    private boolean completeStatus;
    private int statusBatchSize;

    private static final ReentrantLock DUMMY_LOCK = new ReentrantLock(){
        @Override
        public void lock() {}
        @Override
        public void unlock() {}
        @Override
        public boolean isLocked() {return false;}
    };

    private MercurialStatusCache() {
        initPreferences();
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
        MercurialEclipsePlugin.getDefault().getPreferenceStore().addPropertyChangeListener(
                new IPropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent event) {
                        initPreferences();
                    }
                });
        // new RefreshStatusJob("Initializing Mercurial plugin...").schedule();
    }

    public static final MercurialStatusCache getInstance() {
        return MercurialStatusCacheHolder.instance;
    }

    private void addToProjectResources(IProject project, IResource member) {
        if (member.getType() == IResource.PROJECT) {
            return;
        }
        synchronized (projectResources) {
            Set<IResource> set = projectResources.get(project);
            if (set == null) {
                set = new HashSet<IResource>();
                projectResources.put(project, set);
            }
            set.add(member);
        }
    }

    /**
     * Clears the known status of all resources and projects. and calls for an update of decoration
     */
    public synchronized void clear() {
        /*
         * While this clearing of status is a "naive" implementation, it is simple.
         */
        statusMap.clear();
        knownStatus.clear();
        projectResources.clear();
        synchronized (locks) {
            locks.clear();
        }
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
        IProject project = resource.getProject();
        synchronized (locks) {
            ReentrantLock lock = locks.get(project);
            if (lock == null) {
                if (!resource.isAccessible() || resource.isDerived() || resource.isLinked()
                        || !MercurialUtilities.hgIsTeamProviderFor(resource, false)) {
                    lock = DUMMY_LOCK;
                    // TODO we could put the dummy lock here, but then it would forever
                    // stay in the cache, because NOBODY refresh it today.
                    // so if a previously not managed project would became a hg project
                    // it would NOT have a lock anymore
                } else {
                    lock = new ReentrantLock();
                    locks.put(project, lock);
                }
            }
            return lock;
        }
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
            return knownStatus.containsKey(project);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Gets the status of the given resource from cache. The returned BitSet contains a BitSet of the status flags set.
     *
     * The flags correspond to the BIT_* constants in this class.
     *
     * @param resource
     *            the resource to get status for.
     * @return the BitSet with status flags, MAY RETURN NULL, if status is unknown yet
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

    public boolean isSupervised(IResource resource, IPath path) throws HgException {
        Assert.isNotNull(resource);
        Assert.isNotNull(path);

        // check for Eclipse ignore settings
        if (Team.isIgnoredHint(resource)) {
            return false;
        }

        ReentrantLock lock = getLock(resource);

        IProject project = resource.getProject();
        if (project.isAccessible() && null != RepositoryProvider.getProvider(project, MercurialTeamProvider.ID)) {
            if (path.equals(project.getLocation())) {
                return true;
            }
            try {
                lock.lock();
                BitSet status = statusMap.get(path);
                if (status != null) {
                    switch (status.length() - 1) {
                    case MercurialStatusCache.BIT_IGNORE:
                    case MercurialStatusCache.BIT_UNKNOWN:
                        File fileSystemResource = path.toFile();
                        if (fileSystemResource.isDirectory() && status.length() > 1) {
                            // a directory is still supervised if one of the
                            // following bits is set
                            boolean supervised = status.get(BIT_ADDED) || status.get(BIT_CLEAN)
                            || status.get(BIT_DELETED) || status.get(BIT_MODIFIED) || status.get(BIT_REMOVED);
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

    public boolean hasUncommittedChanges(IResource[] resources) throws HgException {
        if (resources != null && resources.length > 0) {
            for (IResource resource : resources) {
                BitSet status = getStatus(resource);
                if (status.length() - 1 > MercurialStatusCache.BIT_CLEAN) {
                    return true;
                }
            }
            return true;
        }
        return false;
    }

    public boolean isAdded(IResource resource, IPath path) throws HgException {
        Assert.isNotNull(resource);
        Assert.isNotNull(path);
        if (null != RepositoryProvider.getProvider(resource.getProject(), MercurialTeamProvider.ID)) {
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
                        if (fileSystemResource.isDirectory() && status.length() > 1) {
                            // a directory is still supervised if one of the
                            // following bits is set
                            boolean supervised = status.get(BIT_CLEAN) || status.get(BIT_DELETED)
                            || status.get(BIT_MODIFIED) || status.get(BIT_REMOVED) || status.get(BIT_CONFLICT)
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

    private static IProgressMonitor checkMonitor(IProgressMonitor monitor){
        if(monitor == null){
            return new NullProgressMonitor();
        }
        return monitor;
    }

    /**
     * Refreshes local repository status. No refresh of changesets.
     */
    public void refreshStatus(final IResource res, IProgressMonitor anyMonitor) throws HgException {
        Assert.isNotNull(res);
        IProgressMonitor monitor = checkMonitor(anyMonitor);
        monitor.subTask(Messages.getString("MercurialStatusCache.Refreshing") + res.getName()); //$NON-NLS-1$

        IProject project = res.getProject();
        if (!project.isOpen() || null == RepositoryProvider.getProvider(project, MercurialTeamProvider.ID)) {
            return;
        }
        if (res.isTeamPrivateMember() || res.isDerived()) {
            return;
        }
        // members should contain folders and project, so we clear
        // status for files, folders and project
        Set<IResource> resources = getLocalMembers(res);
        monitor.worked(1);
        ReentrantLock lock = getLock(res);
        Set<IResource> changed;
        try {
            lock.lock();
            for (IResource resource : resources) {
                statusMap.remove(resource.getLocation());
            }
            monitor.worked(1);

            statusMap.remove(res.getLocation());
            String output = HgStatusClient.getStatusWithoutIgnored(res);
            monitor.worked(1);

            File root = AbstractClient.getHgRoot(res);
            changed = parseStatus(root, res, output);
            monitor.worked(1);

            try {
                String mergeNode = HgStatusClient.getMergeStatus(res);
                project.setPersistentProperty(ResourceProperties.MERGING, mergeNode);
            } catch (CoreException e) {
                throw new HgException(Messages.getString("MercurialStatusCache.FailedToRefreshMergeStatus"), e); //$NON-NLS-1$
            }
            checkForConflict(project);
        } finally {
            lock.unlock();
        }
        monitor.worked(1);
        notifyChanged(changed);

        monitor.worked(1);
    }

    private void checkForConflict(final IProject project) throws HgException {
        try {
            if (project.getPersistentProperty(ResourceProperties.MERGING) == null) {
                return;
            }
        } catch (CoreException e) {
            MercurialEclipsePlugin.logError(e);
            return;
        }
        List<FlaggedAdaptable> status;
        if (HgResolveClient.checkAvailable()) {
            status = HgResolveClient.list(project);
        } else {
            status = HgIMergeClient.getMergeStatus(project);
        }
        Set<IResource> members = getLocalMembers(project);
        for (IResource res : members) {
            removeConflict(res);
        }
        removeConflict(project);
        for (FlaggedAdaptable flaggedAdaptable : status) {
            IFile file = (IFile) flaggedAdaptable.getAdapter(IFile.class);
            if (flaggedAdaptable.getFlag() == CHAR_UNRESOLVED) {
                addConflict(file);
            }
        }
    }

    private Set<IResource> parseStatus(File root, final IResource res, String output) throws HgException {
        IProject project = res.getProject();
        if (res.getType() == IResource.PROJECT) {
            knownStatus.put(project, DUMMY);
        }
        // we need the project for performance reasons - gotta hand it to
        // addToProjectResources
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
            boolean ignoredHint = Team.isIgnoredHint(member);
            if (ignoredHint) {
                bitSet.set(BIT_IGNORE);
            } else {
                bitSet.set(getBitIndex(status.charAt(0)));
                changed.add(member);
            }
            statusMap.put(member.getLocation(), bitSet);

            if (!ignoredHint && member.getType() == IResource.FILE && getBitIndex(status.charAt(0)) != BIT_IGNORE) {
                addToProjectResources(project, member);
            }

            changed.addAll(setStatusToAncestors(member, bitSet));
        }
        return changed;
    }

    private Set<IResource> setStatusToAncestors(IResource resource, BitSet resourceBitSet) {
        // ancestors
        IProject project = resource.getProject();
        Set<IResource> ancestors = new HashSet<IResource>();
        boolean computeDeep = isComputeDeepStatus();
        boolean complete = isCompleteStatus();
        IResource parent = resource.getParent();
        for (; parent != null && parent != project.getParent(); parent = parent.getParent()) {
            IPath location = parent.getLocation();
            BitSet parentBitSet = statusMap.get(location);
            BitSet cloneBitSet = (BitSet) resourceBitSet.clone();
            // should not propagate ignore to parents
            cloneBitSet.clear(BIT_IGNORE);
            cloneBitSet.clear(BIT_CLEAN);

            if (parentBitSet != null) {
                if (!complete && computeDeep && resource.getType() != IResource.PROJECT) {
                    if (parent.isAccessible() && !parent.isTeamPrivateMember() && !parent.isDerived()) {
                        IResourceVisitor visitor = new MemberStatusVisitor(parent, cloneBitSet);
                        try {
                            parent.accept(visitor, IResource.DEPTH_ONE, false);
                        } catch (CoreException e) {
                            MercurialEclipsePlugin.logError(e);
                        }
                    }
                } else {
                    cloneBitSet.or(parentBitSet);
                }
            }
            statusMap.put(location, cloneBitSet);
            ancestors.add(parent);
            addToProjectResources(project, parent);
        }
        return ancestors;
    }

    private boolean isComputeDeepStatus() {
        return computeDeepStatus;
    }

    private boolean isCompleteStatus() {
        return completeStatus;
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
            String msg = Messages.getString("MercurialStatusCache.UnknownStatus") + status + "'"; //$NON-NLS-1$ //$NON-NLS-2$
            MercurialEclipsePlugin.logWarning(msg, new HgException(msg));
            return BIT_IMPOSSIBLE;
        }
    }

    /**
     * Converts the given bit index to the status character Mercurial uses.
     */
    private char getStatusChar(int bitIndex) {
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
            String msg = Messages.getString("MercurialStatusCache.UnknownStatus") + bitIndex + "'"; //$NON-NLS-1$ //$NON-NLS-2$
            MercurialEclipsePlugin.logWarning(msg, new HgException(msg));
            return BIT_IMPOSSIBLE;
        }
    }

    /**
     * Returns the status character used by Mercurial that applies to this resource
     *
     * @param resource
     *            the resource to query the status for
     * @return ! (deleted), R (removed), I (ignored), C (clean), ? (unknown), A (added) or M (modified)
     * @throws HgException
     */
    public char getStatusChar(IResource resource) throws HgException {
        BitSet status = getStatus(resource);
        char statusChar = getStatusChar(status.length() - 1);
        return statusChar;
    }

    /**
     * Gets all Projects managed by Mercurial whose status is known.
     *
     * @return an IProject[] of the projects
     */
    public IProject[] getAllManagedProjects() {
        return knownStatus.keySet().toArray(new IProject[knownStatus.size()]);
    }

    public void resourceChanged(IResourceChangeEvent event) {
        if (event.getType() != IResourceChangeEvent.POST_CHANGE) {
            return;
        }
        IResourceDelta delta = event.getDelta();

        final Map<IProject, Set<IResource>> changed = new HashMap<IProject, Set<IResource>>();
        final Map<IProject, Set<IResource>> added = new HashMap<IProject, Set<IResource>>();
        final Map<IProject, Set<IResource>> removed = new HashMap<IProject, Set<IResource>>();

        IResourceDeltaVisitor visitor = new ResourceDeltaVisitor(removed, changed, added);

        try {
            // walk tree
            delta.accept(visitor);
        } catch (CoreException e) {
            MercurialEclipsePlugin.logError(e);
            return;
        }

        final Set<IProject> changedProjects = new HashSet<IProject>(changed.keySet());
        changedProjects.addAll(added.keySet());
        changedProjects.addAll(removed.keySet());
        for (final IProject project : changedProjects) {
            final Set<IResource> addedSet = added.get(project);
            final Set<IResource> removedSet = removed.get(project);
            final Set<IResource> changedSet = changed.get(project);

            projectChanged(project, addedSet, removedSet, changedSet);
        }

    }

    private void projectChanged(final IProject project, final Set<IResource> addedSet, final Set<IResource> removedSet,
            final Set<IResource> changedSet) {
        final ProjectUpdateJob job = new ProjectUpdateJob(removedSet, changedSet, project, addedSet);
        job.schedule(200);
    }

    /**
     * Refreshes Status of resources in batches
     */
    private void refreshStatus(final Set<IResource> resources, IProject project) throws HgException {
        if (resources == null) {
            return;
        }
        int batchSize = getStatusBatchSize();
        List<IResource> currentBatch = new ArrayList<IResource>();
        for (Iterator<IResource> iterator = resources.iterator(); iterator.hasNext();) {
            IResource resource = iterator.next();

            // project status wanted, no batching needed
            if (resource.getType() == IResource.PROJECT) {
                try {
                    refreshStatus(resource, null);
                } catch (Exception e) {
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
                String output = HgStatusClient.getStatusWithoutIgnored(resource.getLocation().toFile(), currentBatch);
                parseStatus(root, resource, output);
                currentBatch.clear();
            }
        }
        if(!resources.isEmpty()) {
            checkForConflict(project);
        }
    }

    private int getStatusBatchSize() {
        return statusBatchSize;
    }

    /**
     * Determines Members of given resource without adding itself.
     *
     * @param resource
     * @return never null
     * @throws HgException
     */
    public Set<IResource> getLocalMembers(IResource resource) throws HgException {
        Set<IResource> members = new HashSet<IResource>();
        ReentrantLock lock = getLock(resource);
        try {
            lock.lock();
            switch (resource.getType()) {
            case IResource.FILE:
                break;
            case IResource.PROJECT:
                synchronized (projectResources) {
                    Set<IResource> resources = projectResources.get(resource);
                    if (resources != null) {
                        members.addAll(resources);
                        members.remove(resource);
                    }
                }
                break;
            case IResource.FOLDER:
                for (IPath memberPath : statusMap.keySet()) {
                    if (memberPath.equals(resource.getLocation())) {
                        continue;
                    }

                    IContainer container = (IContainer) resource;
                    IResource foundMember = container.findMember(memberPath, false);
                    if (foundMember != null) {
                        members.add(foundMember);
                    }
                }
            }
            members.remove(resource);
            return members;
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
        if(status == null){
            status = new BitSet();
            status.set(BIT_CONFLICT);
            statusMap.put(local.getLocation(), status);
        } else {
            status.set(BIT_CONFLICT);
        }
        setStatusToAncestors(local, status);
        notifyChanged(local);
    }

    /**
     * Removes conflict marker on resource status
     *
     * @param local
     * @throws HgException
     */
    private void removeConflict(IResource local) throws HgException {
        BitSet status = getStatus(local);
        if(status != null && status.get(BIT_CONFLICT)) {
            status.clear(BIT_CONFLICT);
            notifyChanged(local);
        }
    }

    /**
     * @param resources
     */
    @Override
    public void notifyChanged(Set<IResource> resources) {
        setChanged();
        notifyObservers(resources);
    }

    private void initPreferences(){
        computeDeepStatus = Boolean.valueOf(
                HgClients.getPreference(MercurialPreferenceConstants.RESOURCE_DECORATOR_COMPUTE_DEEP_STATUS, "false"))
                .booleanValue();
        completeStatus = Boolean.valueOf(
                HgClients.getPreference(MercurialPreferenceConstants.RESOURCE_DECORATOR_COMPLETE_STATUS, "false"))
                .booleanValue();
        // TODO: group batches by repo root
        String pref = HgClients.getPreference(MercurialPreferenceConstants.STATUS_BATCH_SIZE, String
                .valueOf(STATUS_BATCH_SIZE));

        statusBatchSize = STATUS_BATCH_SIZE;
        if (pref.length() > 0) {
            try {
                statusBatchSize = Integer.parseInt(pref);
            } catch (NumberFormatException e) {
                MercurialEclipsePlugin.logWarning(Messages
                        .getString("MercurialStatusCache.BatchSizeForStatusCommandNotCorrect."), e); //$NON-NLS-1$
            }
        }
    }

}
