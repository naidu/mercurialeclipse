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
import java.util.Collections;
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
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.Team;
import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.commands.HgClients;
import com.vectrace.MercurialEclipse.commands.HgResolveClient;
import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.commands.extensions.HgIMergeClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.FlaggedAdaptable;
import com.vectrace.MercurialEclipse.model.HgRoot;
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
    private final class ProjectUpdateJob extends Job {

        private final Set<IResource> removedSet;
        private final Set<IResource> changedSet;
        private final IProject project;
        private final Set<IResource> addedSet;

        private ProjectUpdateJob(Set<IResource> removedSet, Set<IResource> changedSet,
                IProject project, Set<IResource> addedSet) {
            super(Messages.mercurialStatusCache_RefreshStatus);
            if(removedSet != null) {
                this.removedSet = removedSet;
            } else {
                this.removedSet = Collections.emptySet();
            }
            if(changedSet != null) {
                this.changedSet = changedSet;
            } else {
                this.changedSet = Collections.emptySet();
            }
            if(addedSet != null) {
                this.addedSet = addedSet;
            } else {
                this.addedSet = Collections.emptySet();
            }

            this.project = project;
        }


        @Override
        protected IStatus run(IProgressMonitor monitor) {
            // now process gathered changes (they are in the lists)
            try {
                updateProject(monitor);
            } catch (CoreException e) {
                MercurialEclipsePlugin.logError(e);
                return new Status(IStatus.ERROR, MercurialEclipsePlugin.ID, e.getLocalizedMessage(), e);
            }  finally {
                monitor.done();
            }
            return Status.OK_STATUS;
        }


        private void updateProject(IProgressMonitor monitor) throws HgException {
            Set<IResource> resources = new HashSet<IResource>();
            resources.addAll(changedSet);
            resources.addAll(addedSet);
            resources.addAll(removedSet);

            if (resources.size() > NUM_CHANGED_FOR_COMPLETE_STATUS || resources.contains(project)) {
                monitor.beginTask(NLS.bind(Messages.mercurialStatusCache_RefreshingProject, project.getName()), 1);
                // do not need to call notifyChanged(resources): refreshStatus() does it already
                refreshStatus(project, monitor);
            } else if(!resources.isEmpty()) {
                monitor.beginTask(Messages.mercurialStatusCache_RefreshingResources, 1);
                // do not need to call notifyChanged(resources): refreshStatus() does it already
                refreshStatus(resources, project);
            }
            monitor.worked(1);
        }
    }

    private final class MemberStatusVisitor implements IResourceVisitor {

        private final BitSet bitSet;
        private final BitSet temp;
        private final IResource parent;

        public MemberStatusVisitor(IResource parent, BitSet bitSet) {
            this.bitSet = bitSet;
            this.parent = parent;
            temp = new BitSet(MAX_BITS_COUNT);
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
                    temp.clear();
                    temp.or(memberBitSet);
                    temp.andNot(IGNORED_MASK);
                    if(memberBitSet.intersects(MODIFIED_MASK)){
                        temp.set(BIT_MODIFIED);
                    }
                    bitSet.or(temp);
                }
            }
            return true;
        }

    }

    /**
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

    /** maximum bits count used in the cache */
    private final static int MAX_BITS_COUNT = BIT_CONFLICT + 1;

    public static final char CHAR_MODIFIED = 'M';
    public static final char CHAR_ADDED = 'A';
    public static final char CHAR_UNKNOWN = '?';
    public static final char CHAR_CLEAN = 'C';
    public static final char CHAR_IGNORED = 'I';
    public static final char CHAR_REMOVED = 'R';
    public static final char CHAR_DELETED = '!';
    public static final char CHAR_UNRESOLVED = 'U';
    public static final char CHAR_RESOLVED = 'R';

    /**
     * If the child file has any of the bits set in the range from {@link #BIT_IGNORE}
     * to {@link #BIT_ADDED}, we do not propagate this bits to the parent directory directly,
     * but propagate only bits covered by the {@link #MODIFIED_MASK}
     */
    private static final BitSet IGNORED_MASK = new BitSet(MAX_BITS_COUNT);

    /**
     * We propagate only {@link #BIT_MODIFIED} bit to the parent directory, if any of bits
     * in the range from {@link #BIT_DELETED} to {@link #BIT_MODIFIED} is set on the child file.
     */
    private static final BitSet MODIFIED_MASK = new BitSet(MAX_BITS_COUNT);

    static {
        IGNORED_MASK.set(BIT_IGNORE, BIT_ADDED);
        MODIFIED_MASK.set(BIT_DELETED, BIT_MODIFIED);
    }

    protected static final int INTERESTING_CHANGES = IResourceDelta.CONTENT | IResourceDelta.MOVED_FROM
        | IResourceDelta.MOVED_TO | IResourceDelta.OPEN | IResourceDelta.REPLACED | IResourceDelta.TYPE;

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
     */
    public ReentrantLock getLock(IResource resource) {
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
     */
    public boolean isStatusKnown(IProject project) {
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
     */
    public BitSet getStatus(IResource resource) {
        ReentrantLock lock = getLock(resource);
        try {
            lock.lock();
            return statusMap.get(resource.getLocation());
        } finally {
            lock.unlock();
        }
    }

    public boolean isSupervised(IResource resource) {
        return MercurialUtilities.hgIsTeamProviderFor(resource, false)
        && isSupervised(resource.getProject(), resource.getLocation());
    }

    public boolean isSupervised(IResource resource, IPath path) {
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

    public boolean hasUncommittedChanges(IResource[] resources) {
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

    public boolean isAdded(IResource resource, IPath path) {
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

    public boolean isRemoved(IResource resource) {
        Assert.isNotNull(resource);
        BitSet status = getStatus(resource);
        if(status == null){
            return false;
        }
        return status.get(BIT_REMOVED) || status.get(BIT_DELETED);
    }

    private static IProgressMonitor checkMonitor(IProgressMonitor monitor){
        if(monitor == null){
            return new NullProgressMonitor();
        }
        return monitor;
    }

    /**
     * Refreshes local repository status and notifies the listeners about changes. No refresh of changesets.
     */
    public Set<IResource> refreshStatus(final IResource res, IProgressMonitor anyMonitor) throws HgException {
        Assert.isNotNull(res);
        IProgressMonitor monitor = checkMonitor(anyMonitor);
        monitor.subTask(Messages.mercurialStatusCache_Refreshing + res.getName());

        IProject project = res.getProject();
        if (!project.isOpen() || null == RepositoryProvider.getProvider(project, MercurialTeamProvider.ID)) {
            return Collections.emptySet();
        }
        if (res.isTeamPrivateMember() || res.isDerived()) {
            return Collections.emptySet();
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
            HgRoot root = AbstractClient.getHgRoot(res);
            String output = HgStatusClient.getStatusWithoutIgnored(root, res);
            monitor.worked(1);

            changed = parseStatus(root, res, output);
            monitor.worked(1);

            try {
                String mergeNode = HgStatusClient.getMergeStatus(res);
                project.setPersistentProperty(ResourceProperties.MERGING, mergeNode);
            } catch (CoreException e) {
                throw new HgException(Messages.mercurialStatusCache_FailedToRefreshMergeStatus, e);
            }
            changed.addAll(checkForConflict(project));
        } finally {
            lock.unlock();
        }
        monitor.worked(1);
        notifyChanged(changed);

        monitor.worked(1);
        return changed;
    }

    private Set<IResource> checkForConflict(final IProject project) throws HgException {
        try {
            if (project.getPersistentProperty(ResourceProperties.MERGING) == null) {
                return Collections.emptySet();
            }
        } catch (CoreException e) {
            MercurialEclipsePlugin.logError(e);
            return Collections.emptySet();
        }
        List<FlaggedAdaptable> status;
        if (HgResolveClient.checkAvailable()) {
            status = HgResolveClient.list(project);
        } else {
            status = HgIMergeClient.getMergeStatus(project);
        }
        Set<IResource> changed = new HashSet<IResource>();
        Set<IResource> members = getLocalMembers(project);
        for (IResource res : members) {
            if(removeConflict(res)){
                changed.add(res);
            }
        }
        if(removeConflict(project)){
            changed.add(project);
        }
        for (FlaggedAdaptable flaggedAdaptable : status) {
            IFile file = (IFile) flaggedAdaptable.getAdapter(IFile.class);
            if (flaggedAdaptable.getFlag() == CHAR_UNRESOLVED) {
                changed.addAll(addConflict(file));
            }
        }
        return changed;
    }

    /**
     * @param output must contain file paths as paths relative to the hg root
     * @return
     */
    private Set<IResource> parseStatus(HgRoot root, final IResource res, String output) {
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

            // doesn't belong to our project (can happen if root is above project level)
            // or simply deleted, so can't be found...
            if (member == null) {
                if(getBitIndex(status.charAt(0)) == BIT_REMOVED){
                    IPath path = new Path(new File(root, localName).getAbsolutePath());
                    // creates a handle to non-existent file. This is ok.
                    member = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(path);
                    if(member == null || !member.getProject().equals(project)) {
                        continue;
                    }
                } else {
                    continue;
                }
            }

            BitSet bitSet = new BitSet(MAX_BITS_COUNT);
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

            // should not propagate ignores states to parents
            cloneBitSet.andNot(IGNORED_MASK);
            boolean intersects = resourceBitSet.intersects(MODIFIED_MASK);
            if(intersects) {
                cloneBitSet.set(BIT_MODIFIED);
            } else {
                cloneBitSet.set(BIT_CLEAN);
            }

            if (parentBitSet != null) {
                if (!complete && computeDeep && resource.getType() != IResource.PROJECT) {
                    if (parent.isAccessible() && !parent.isTeamPrivateMember() && !parent.isDerived()) {
                        IResourceVisitor visitor = new MemberStatusVisitor(parent, cloneBitSet);
                        try {
                            parent.accept(visitor, IResource.DEPTH_ONE, false);
                        } catch (CoreException e) {
                            MercurialEclipsePlugin.logError(e);
                        }
                        if(parentBitSet.intersects(MODIFIED_MASK)){
                            cloneBitSet.or(parentBitSet);
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
            String msg = Messages.mercurialStatusCache_UnknownStatus + status + "'"; //$NON-NLS-1$
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
            String msg = Messages.mercurialStatusCache_UnknownStatus + bitIndex + "'"; //$NON-NLS-1$
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
     */
    public char getStatusChar(IResource resource) {
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
        for (IProject project : changedProjects) {
            Set<IResource> addedSet = added.get(project);
            Set<IResource> removedSet = removed.get(project);
            Set<IResource> changedSet = changed.get(project);

            projectChanged(project, addedSet, removedSet, changedSet);
        }

    }

    private void projectChanged(final IProject project, final Set<IResource> addedSet, final Set<IResource> removedSet,
            final Set<IResource> changedSet) {
        new ProjectUpdateJob(removedSet, changedSet, project, addedSet).schedule();
    }

    /**
     * Refreshes Status of resources in batches and notifies the listeners about changes
     *
     * @param resources
     *            may be null. If not null, then all elements must be from the given project. If null, no refresh will
     *            happen. If the set contains a project, it is ignored
     * @param project
     *            not null. The project which resources state has to be updated
     */
    private Set<IResource> refreshStatus(final Set<IResource> resources, IProject project) throws HgException {
        if (resources == null || resources.isEmpty()) {
            return Collections.emptySet();
        }
        // project status wanted, no batching needed
        if(resources.contains(project)){
            resources.remove(project);
            if(resources.isEmpty()){
                return Collections.emptySet();
            }
        }
        int batchSize = getStatusBatchSize();
        List<IResource> currentBatch = new ArrayList<IResource>();
        Set<IResource> changed = new HashSet<IResource>();

        for (Iterator<IResource> iterator = resources.iterator(); iterator.hasNext();) {
            IResource resource = iterator.next();

            // status for single resource is batched
            if (!resource.isTeamPrivateMember()) {
                currentBatch.add(resource);
            }
            if (currentBatch.size() % batchSize == 0 || !iterator.hasNext()) {
                // call hg with batch
                HgRoot root = AbstractClient.getHgRoot(resource);
                String output = HgStatusClient.getStatusWithoutIgnored(root, currentBatch);
                changed.addAll(parseStatus(root, resource, output));
                currentBatch.clear();
            }
        }
        if(!resources.isEmpty()) {
            changed.addAll(checkForConflict(project));
        }
        notifyChanged(changed);
        return changed;
    }

    private int getStatusBatchSize() {
        return statusBatchSize;
    }

    /**
     * Determines Members of given resource without adding itself.
     *
     * @param resource
     * @return never null
     */
    public Set<IResource> getLocalMembers(IResource resource) {
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

    public void clear(IProject project) {
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
     */
    private Set<IResource> addConflict(IResource local) {
        BitSet status = getStatus(local);
        if(status == null){
            status = new BitSet(MAX_BITS_COUNT);
            status.set(BIT_CONFLICT);
            statusMap.put(local.getLocation(), status);
        } else {
            status.set(BIT_CONFLICT);
        }
        Set<IResource> changed = setStatusToAncestors(local, status);
        changed.add(local);
        return changed;
    }

    /**
     * Removes conflict marker on resource status
     *
     * @param local
     * @return true if there was a conflict and now it is removed
     */
    private boolean removeConflict(IResource local) {
        BitSet status = getStatus(local);
        if(status != null && status.get(BIT_CONFLICT)) {
            status.clear(BIT_CONFLICT);
            return true;
        }
        return false;
    }

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
                        .mercurialStatusCache_BatchSizeForStatusCommandNotCorrect, e);
            }
        }
    }

}
