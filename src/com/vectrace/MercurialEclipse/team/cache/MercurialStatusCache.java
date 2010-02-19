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
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team.cache;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.Team;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.commands.HgResolveClient;
import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.FlaggedAdaptable;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.utils.Bits;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * Caches the Mercurial Status of each file and offers methods for retrieving, clearing and
 * refreshing repository state.
 *
 * @author Bastian Doetsch
 */
public class MercurialStatusCache extends AbstractCache implements IResourceChangeListener {

	private static final int STATUS_BATCH_SIZE = 10;
	static final int NUM_CHANGED_FOR_COMPLETE_STATUS = 50;

	/**
	 * @author Andrei
	 */
	private final class ProjectUpdateJob extends Job {

		private final IProject project;
		private final Set<IResource> resources;

		private ProjectUpdateJob(Set<IResource> removedSet, Set<IResource> changedSet,
				IProject project, Set<IResource> addedSet) {
			super(Messages.mercurialStatusCache_RefreshStatus);
			this.project = project;
			resources = new HashSet<IResource>();
			if(removedSet != null) {
				resources.addAll(removedSet);
			}
			if(changedSet != null) {
				resources.addAll(changedSet);
			}
			if(addedSet != null) {
				resources.addAll(addedSet);
			}
			if(resources.contains(project) || resources.size() > NUM_CHANGED_FOR_COMPLETE_STATUS){
				resources.clear();
				resources.add(project);
			}
		}


		@Override
		protected IStatus run(IProgressMonitor monitor) {
			// now process gathered changes (they are in the lists)
			try {
				updateProject(monitor);
			} catch (CoreException e) {
				MercurialEclipsePlugin.logError(e);
				return e.getStatus();
			}  finally {
				monitor.done();
			}
			return Status.OK_STATUS;
		}

		private void updateProject(IProgressMonitor monitor) throws HgException {
			if (resources.size() == 1 && resources.contains(project)) {
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

		@Override
		public boolean belongsTo(Object family) {
			return ProjectUpdateJob.class.equals(family);
		}

		@Override
		public boolean equals(Object obj) {
			if(!(obj instanceof ProjectUpdateJob)){
				return false;
			}
			ProjectUpdateJob job = (ProjectUpdateJob) obj;
			if(resources.size() != job.resources.size()){
				return false;
			}
			if(!project.equals(job.project)){
				return false;
			}
			return resources.containsAll(job.resources);
		}

		@Override
		public int hashCode() {
			return resources.size() + project.getName().hashCode();
		}
	}

	private final class MemberStatusVisitor {

		private int bitSet;

		public MemberStatusVisitor(IPath parentLocation, int bitSet) {
			this.bitSet = bitSet;
		}

		public boolean visit(IPath childLocation) {
			Integer memberBitSet = statusMap.get(childLocation);
			if (memberBitSet != null) {
				if(Bits.contains(memberBitSet.intValue(), MODIFIED_MASK)){
					bitSet |= BIT_MODIFIED;
					// now we are dirty, so we can stop
					return false;
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

	public final static int BIT_IGNORE = 1 << 1;
	public final static int BIT_CLEAN = 1 << 2;
	/** file is tracked by hg, but it is missing on a disk (probably deleted by external command) */
	public final static int BIT_MISSING = 1 << 3;
	public final static int BIT_REMOVED = 1 << 4;
	public final static int BIT_UNKNOWN = 1 << 5;
	public final static int BIT_ADDED = 1 << 6;
	public final static int BIT_MODIFIED = 1 << 7;
	public final static int BIT_IMPOSSIBLE = 1 << 8;
	public final static int BIT_CONFLICT = 1 << 9;
	/** directory bit */
	public final static int BIT_DIR = 1 << 10;

	private final static Integer _IGNORE = Integer.valueOf(BIT_IGNORE);
	private final static Integer _CLEAN = Integer.valueOf(BIT_CLEAN);
//    private final static Integer _MISSING = Integer.valueOf(BIT_MISSING);
//    private final static Integer _REMOVED = Integer.valueOf(BIT_REMOVED);
//    private final static Integer _UNKNOWN = Integer.valueOf(BIT_UNKNOWN);
//    private final static Integer _ADDED = Integer.valueOf(BIT_ADDED);
//    private final static Integer _MODIFIED = Integer.valueOf(BIT_MODIFIED);
//    private final static Integer _IMPOSSIBLE = Integer.valueOf(BIT_IMPOSSIBLE);
	private final static Integer _CONFLICT = Integer.valueOf(BIT_CONFLICT);

	/** maximum bits count used in the cache */
//    private final static int MAX_BITS_COUNT = 9;

	public static final char CHAR_IGNORED = 'I';
	public static final char CHAR_CLEAN = 'C';
	public static final char CHAR_MISSING = '!';
	public static final char CHAR_REMOVED = 'R';
	public static final char CHAR_UNKNOWN = '?';
	public static final char CHAR_ADDED = 'A';
	public static final char CHAR_MODIFIED = 'M';
	public static final char CHAR_UNRESOLVED = 'U';
	public static final char CHAR_RESOLVED = 'R';

	/**
	 * If the child file has any of the bits set: BIT_IGNORE | BIT_CLEAN |
	 *  BIT_MISSING | BIT_REMOVED | BIT_UNKNOWN | BIT_ADDED,
	 * we do not propagate this bits to the parent directory directly,
	 * but propagate only {@link #BIT_MODIFIED} or {@link #BIT_CONFLICT}
	 */
	private static final int IGNORED_MASK = BIT_IGNORE | BIT_CLEAN |
			BIT_MISSING | BIT_REMOVED | BIT_UNKNOWN | BIT_ADDED;

	/**
	 * We propagate only {@link #BIT_MODIFIED} bit to the parent directory, if any from bits:
	 * BIT_MISSING | BIT_REMOVED | BIT_UNKNOWN | BIT_ADDED | BIT_MODIFIED is set on the child file.
	 */
	public static final int MODIFIED_MASK = BIT_MISSING | BIT_REMOVED |
			BIT_UNKNOWN | BIT_ADDED | BIT_MODIFIED;

	/** a directory is still supervised if one of the following bits is set */
	private static final int DIR_SUPERVISED_MASK = BIT_ADDED | BIT_CLEAN | BIT_MISSING
		| BIT_MODIFIED | BIT_REMOVED | BIT_CONFLICT;

	/**  an "added" directory is only added if NONE of the following bits is set */
	private static final int DIR_NOT_ADDED_MASK = BIT_CLEAN | BIT_MISSING
		| BIT_MODIFIED | BIT_REMOVED | BIT_CONFLICT | BIT_IGNORE;


	protected static final int MASK_CHANGED = IResourceDelta.OPEN | IResourceDelta.CONTENT
		| IResourceDelta.MOVED_FROM | IResourceDelta.REPLACED | IResourceDelta.TYPE;

	protected static final int MASK_DELTA = MASK_CHANGED | IResourceDelta.MOVED_TO
		| IResourceDelta.ADDED | IResourceDelta.COPIED_FROM | IResourceDelta.REMOVED;

	/** Used to store the last known status of a resource */
	private final ConcurrentHashMap<IPath, Integer> statusMap = new ConcurrentHashMap<IPath, Integer>(10000, 0.75f, 4);
	private final BitMap bitMap;
	private final Object statusUpdateLock = new byte[0];

	/** Used to store which projects have already been parsed */
	private final ConcurrentHashMap<IProject, HgRoot> knownStatus = new ConcurrentHashMap<IProject, HgRoot>();

	private boolean computeDeepStatus;
	private int statusBatchSize;
	private static final Set<IResource> EMPTY_SET = new HashSet<IResource>();

	static class BitMap {
		private final Set<IPath> IGNORE_SET = new HashSet<IPath>();
		// don't waste space with most popular state
		// private final Set<IPath> MAP_CLEAN = new HashSet<IPath>();
		private final Set<IPath> MISSING_SET = new HashSet<IPath>();
		private final Set<IPath> REMOVED_SET = new HashSet<IPath>();
		private final Set<IPath> UNKNOWN_SET = new HashSet<IPath>();
		private final Set<IPath> ADDED_SET = new HashSet<IPath>();
		private final Set<IPath> MODIFIED_SET = new HashSet<IPath>();
		private final Set<IPath> CONFLICT_SET = new HashSet<IPath>();
		/** directories */
		private final Set<IPath> DIR_SET = new HashSet<IPath>();
		// we do not cache impossible values
		// private final Set<IPath> MAP_IMPOSSIBLE = new HashSet<IPath>();

		public BitMap() {
			super();
		}

		synchronized void put(IPath path, Integer set){
			// removed is the first one for speed
			int mask = set.intValue();
			if((mask & BIT_REMOVED) != 0){
				REMOVED_SET.add(path);
			}
			if((mask & BIT_MISSING) != 0){
				MISSING_SET.add(path);
			}
			if((mask & BIT_UNKNOWN) != 0){
				UNKNOWN_SET.add(path);
			}
			if((mask & BIT_ADDED) != 0){
				ADDED_SET.add(path);
			}
			if((mask & BIT_MODIFIED) != 0){
				MODIFIED_SET.add(path);
			}
			if((mask & BIT_CONFLICT) != 0){
				CONFLICT_SET.add(path);
			}
			if((mask & BIT_IGNORE) != 0){
				IGNORE_SET.add(path);
			}
			if((mask & BIT_DIR) != 0){
				DIR_SET.add(path);
			}
		}

		synchronized Set<IPath> get(int bit){
			switch (bit) {
			case BIT_REMOVED:
				return REMOVED_SET;
			case BIT_MISSING:
				return MISSING_SET;
			case BIT_UNKNOWN:
				return UNKNOWN_SET;
			case BIT_ADDED:
				return ADDED_SET;
			case BIT_MODIFIED:
				return MODIFIED_SET;
			case BIT_CONFLICT:
				return CONFLICT_SET;
			case BIT_IGNORE:
				return IGNORE_SET;
			case BIT_DIR:
				return DIR_SET;
			default:
				return null;
			}
		}

		synchronized void remove(IPath path) {
			remove(path, REMOVED_SET);
			remove(path, MISSING_SET);
			remove(path, UNKNOWN_SET);
			remove(path, ADDED_SET);
			remove(path, MODIFIED_SET);
			remove(path, CONFLICT_SET);
			remove(path, IGNORE_SET);
			remove(path, DIR_SET);
		}

		void remove(IPath path, Set<IPath> set) {
			if(!set.isEmpty()) {
				set.remove(path);
			}
		}
	}

	private MercurialStatusCache() {
		super();
		bitMap = new BitMap();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this,
				IResourceChangeEvent.POST_CHANGE);
	}

	public static final MercurialStatusCache getInstance() {
		return MercurialStatusCacheHolder.instance;
	}

	/**
	 * Checks if status for given project is known.
	 *
	 * @param project
	 *            the project to be checked
	 * @return true if known, false if not.
	 */
	public boolean isStatusKnown(IProject project) {
		return knownStatus.containsKey(project);
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
	public Integer getStatus(IResource resource) {
		return statusMap.get(resource.getLocation());
	}

	public boolean isSupervised(IResource resource) {
		return isSupervised(resource, resource.getLocation());
	}

	public boolean isSupervised(IResource resource, IPath path) {
		Assert.isNotNull(resource);
		Assert.isNotNull(path);
		Integer statusInt = statusMap.get(path);
		if(statusInt == null){
			return false;
		}
		IProject project = resource.getProject();
		if (path.equals(project.getLocation())) {
			return project.isAccessible() && MercurialTeamProvider.isHgTeamProviderFor(project);
		}
		int status = statusInt.intValue();
		int highestBit = Bits.highestBit(status);
		switch (highestBit) {
		case BIT_IGNORE:
		case BIT_UNKNOWN:
			if (resource.getType() != IResource.FILE && highestBit != BIT_IGNORE) {
				// check for Eclipse ignore settings
				if (Team.isIgnoredHint(resource)) {
					return false;
				}
				// a directory is still supervised if one of the lower bits set
				return Bits.contains(status, DIR_SUPERVISED_MASK);
			}
			return false;
		}
		return true;

	}

	public boolean isAdded(IPath path) {
		Assert.isNotNull(path);
		Integer statusInt = statusMap.get(path);
		if(statusInt == null){
			return false;
		}
		int status = statusInt.intValue();
		if (Bits.highestBit(status) == BIT_ADDED) {
			File fileSystemResource = path.toFile();
			if (fileSystemResource.isDirectory()) {
				return Bits.contains(status, DIR_NOT_ADDED_MASK);
			}
			return true;
		}
		return false;
	}

	public boolean isDirectory(IPath location) {
		if(location == null){
			return false;
		}
		return bitMap.get(BIT_DIR).contains(location);
	}

	public boolean isRemoved(IResource resource) {
		Assert.isNotNull(resource);
		Integer status = getStatus(resource);
		if(status == null){
			return false;
		}
		return Bits.contains(status.intValue(), BIT_REMOVED);
	}

	public boolean isUnknown(IResource resource) {
		Assert.isNotNull(resource);
		Integer status = getStatus(resource);
		if(status == null){
			// since we track everything now, all "unknown" files are really unknown
			return true;
		}
		return Bits.contains(status.intValue(), BIT_UNKNOWN);
	}

	public boolean isIgnored(IResource resource) {
		Assert.isNotNull(resource);
		Integer status = getStatus(resource);
		if(status == null && isStatusKnown(resource.getProject())){
			// it seems that original autors intentionally do not tracked status for
			// ignored files. I guess the reason was performance: for a java project,
			// including "ignored" class files would double the cache size...
			return true;
		}
		return false;
	}

	public boolean isClean(IResource resource) {
		Assert.isNotNull(resource);
		Integer status = getStatus(resource);
		if(status == null){
			return false;
		}
		return Bits.contains(status.intValue(), BIT_CLEAN);
	}

	/**
	 *
	 * @param statusBit
	 * @param parent
	 * @return may return null, if no paths for given parent and bitset are known
	 */
	private Set<IPath> getPaths(int statusBit, IPath parent){
		boolean isMappedState = statusBit != BIT_CLEAN && statusBit != BIT_IMPOSSIBLE;
		if(!isMappedState) {
			return null;
		}
		Set<IPath> all = bitMap.get(statusBit);
		if(all.isEmpty()){
			return null;
		}
		Set<IPath> result = null;
		int segmentCount = parent.segmentCount();
		for (IPath path : all) {
			if(path.segmentCount() <= segmentCount){
				continue;
			}
			if(parent.isPrefixOf(path)){
				if(result == null){
					result = new HashSet<IPath>();
				}
				result.add(path);
			}
		}
		return result;
	}

	public Set<IFile> getFiles(int statusBits, IContainer folder){
		Set<IResource> resources = getResources(statusBits, folder);
		Set<IFile> files = new HashSet<IFile>();
		for (IResource resource : resources) {
			IPath location = resource.getLocation();
			if(resource instanceof IFile && location != null && !location.toFile().isDirectory()){
				files.add((IFile) resource);
			}
		}
		return files;
	}

	public Set<IResource> getResources(int statusBits, IContainer folder){
		Set<IResource> resources;
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		boolean isMappedState = statusBits != BIT_CLEAN && statusBits != BIT_IMPOSSIBLE
			&& Bits.cardinality(statusBits) == 1;
		if(isMappedState){
			Set<IPath> set = bitMap.get(statusBits);
			if(set == null || set.isEmpty()){
				return EMPTY_SET;
			}
			IPath parentPath = ResourceUtils.getPath(folder);
			resources = new HashSet<IResource>();
			for (IPath path : set) {
			// we don't know if it is a file or folder...
				IFile tmp = root.getFileForLocation(path);
				if(tmp != null) {
					if(parentPath.isPrefixOf(path)) {
						resources.add(tmp);
					}
				} else {
					IContainer container = root.getContainerForLocation(path);
					if(container != null) {
						if(parentPath.isPrefixOf(path)) {
							resources.add(container);
						}
					}
				}
			}
		} else {
			resources = new HashSet<IResource>();
			Set<Entry<IPath,Integer>> entrySet = statusMap.entrySet();
			IPath parentPath = ResourceUtils.getPath(folder);
			for (Entry<IPath, Integer> entry : entrySet) {
				Integer status = entry.getValue();
				if(status != null && Bits.contains(status.intValue(), statusBits)){
					IPath path = entry.getKey();
					// we don't know if it is a file or folder...
					IFile tmp = root.getFileForLocation(path);
					if(tmp != null) {
						if(parentPath.isPrefixOf(path)) {
							resources.add(tmp);
						}
					} else {
						IContainer container = root.getContainerForLocation(path);
						if(container != null) {
							if(parentPath.isPrefixOf(path)) {
								resources.add(container);
							}
						}
					}
				}
			}
		}
		return resources;
	}

	private static IProgressMonitor checkMonitor(IProgressMonitor monitor){
		if(monitor == null){
			return new NullProgressMonitor();
		}
		return monitor;
	}

	/**
	 * Refreshes the local repository status for all projects under the given hg root
	 *  and notifies the listeners about changes. No refresh of changesets.
	 */
	public void refreshStatus(HgRoot root, IProgressMonitor monitor) throws HgException {
		Assert.isNotNull(root);
		monitor = checkMonitor(monitor);
		monitor.subTask(NLS.bind(Messages.mercurialStatusCache_Refreshing, root.getName()));


		Set<IProject> projects = ResourceUtils.getProjects(root);
		Set<IResource> changed = new HashSet<IResource>();
		synchronized (statusUpdateLock) {
			// get status and branch for hg root
			String output = HgStatusClient.getStatusWithoutIgnored(root);
			String[] mergeStatus = HgStatusClient.getMergeStatus(root);
			String currentChangeSetId = mergeStatus[0];
			LocalChangesetCache.getInstance().updateLatestChangeset(root, currentChangeSetId);
			String mergeNode = mergeStatus[1];
			String branch = mergeStatus[2];

			String[] lines = NEWLINE.split(output);
			Map<IProject, IPath> pathMap = new HashMap<IProject, IPath>();
			Iterator<IProject> iterator = projects.iterator();
			while (iterator.hasNext()) {
				IProject project = iterator.next();
				if (!project.isOpen() || !MercurialUtilities.isPossiblySupervised(project)) {
					iterator.remove();
					continue;
				}
				// clear status for project
				projectDeletedOrClosed(project);
				monitor.worked(1);
				if(monitor.isCanceled()){
					return;
				}
				pathMap.put(project, project.getLocation());
			}

			changed.addAll(parseStatus(root, pathMap, lines));
			boolean mergeInProgress = mergeNode != null && mergeNode.length() > 0;
			for (IProject project : projects) {
				// TODO use multiple projects (from this hg root) as input at ONCE
				knownStatus.put(project, root);
				try {
					HgStatusClient.setMergeStatus(project, mergeNode);
					MercurialTeamProvider.setCurrentBranch(branch, project);
				} catch (CoreException e) {
					throw new HgException(Messages.mercurialStatusCache_FailedToRefreshMergeStatus, e);
				}
			}
			if(mergeInProgress) {
				changed.addAll(checkForConflict(root));
			}
		}
		monitor.worked(1);
		if(monitor.isCanceled()){
			return;
		}
		notifyChanged(changed, false);

		monitor.worked(1);
	}

	/**
	 * Refreshes local repository status and notifies the listeners about changes. No refresh of changesets.
	 */
	public void refreshStatus(IResource res, IProgressMonitor monitor) throws HgException {
		Assert.isNotNull(res);
		monitor = checkMonitor(monitor);
		monitor.subTask(NLS.bind(Messages.mercurialStatusCache_Refreshing, res.getName()));

		IProject project = res.getProject();

		if (!project.isOpen() || !MercurialUtilities.isPossiblySupervised(res)) {
			return;
		}

		HgRoot root = AbstractClient.getHgRoot(res);

		Set<IResource> changed;
		IPath projectLocation = project.getLocation();
		synchronized (statusUpdateLock) {
			String output = HgStatusClient.getStatusWithoutIgnored(root, res);
			if(monitor.isCanceled()){
				return;
			}
			monitor.worked(1);
			// clear status for files, folders or project
			if(res instanceof IProject){
				projectDeletedOrClosed(project);
			} else {
				clearStatusCache(res, false);
			}
			monitor.worked(1);
			if(monitor.isCanceled()){
				return;
			}
			String[] lines = NEWLINE.split(output);
			Map<IProject, IPath> pathMap = new HashMap<IProject, IPath>();
			pathMap.put(project, projectLocation);
			changed = parseStatus(root, pathMap, lines);
			if(!(res instanceof IProject) && !changed.contains(res)){
				// fix for issue 10155: No status update after reverting changes on .hgignore
				changed.add(res);
				if(res instanceof IFolder){
					IFolder folder = (IFolder) res;
					ResourceUtils.collectAllResources(folder, changed);
				}
			}
			if(res instanceof IProject) {
				knownStatus.put(project, root);
			}
		}
		if(monitor.isCanceled()){
			return;
		}
		monitor.worked(1);

		if(res instanceof IProject){
			try {
				String[] mergeStatus = HgStatusClient.getMergeStatus(root);
				String id = mergeStatus[0];
				LocalChangesetCache.getInstance().updateLatestChangeset(root, id);
				String mergeNode = mergeStatus[1];
				String branch = mergeStatus[2];
				HgStatusClient.setMergeStatus(project, mergeNode);
				// TODO use branch map
				MercurialTeamProvider.setCurrentBranch(branch, project);
			} catch (CoreException e) {
				throw new HgException(Messages.mercurialStatusCache_FailedToRefreshMergeStatus, e);
			}
		}
		// TODO shouldn't this go in the block above?
		changed.addAll(checkForConflict(project));
		if(monitor.isCanceled()){
			return;
		}
		monitor.worked(1);
		notifyChanged(changed, false);

		monitor.worked(1);
	}

	/**
	 * @param res
	 * @return true if a change of given file can trigger a project status update
	 * @throws HgException
	 */
	public static boolean canTriggerFullCacheUpdate(IResource res) throws HgException {
		if(!(res instanceof IFile)){
			return false;
		}
		return ".hgignore".equals(res.getName());
	}

	/**
	 * @param folder non null resource
	 * @return non null set of all child entries managed by this cache
	 */
	private Set<IPath> getChildrenFromCache(IContainer folder) {
		IPath parentPath = ResourceUtils.getPath(folder);
		return getPathChildrenFromCache(parentPath);
	}

	/**
	 * @param parentPath
	 * @return non null set of all child entries managed by this cache
	 */
	private Set<IPath> getPathChildrenFromCache(IPath parentPath) {
		Set<IPath> children = new HashSet<IPath>();
		Set<IPath> keySet = statusMap.keySet();
		for (IPath path : keySet) {
			if(path != null && parentPath.isPrefixOf(path)) {
				children.add(path);
			}
		}
		children.remove(parentPath);
		return children;
	}

	private Set<IResource> checkForConflict(final IProject project) throws HgException {
		if (!HgStatusClient.isMergeInProgress(project)) {
			return Collections.emptySet();
		}
		List<FlaggedAdaptable> status = HgResolveClient.list(project);
		Set<IResource> changed = new HashSet<IResource>();
		Set<IResource> members = getLocalMembers(project);
		for (IResource res : members) {
			if(removeConflict(res.getLocation())){
				changed.add(res);
			}
		}
		if(removeConflict(project.getLocation())){
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

	private Set<IResource> checkForConflict(final HgRoot hgRoot) throws HgException {

		List<FlaggedAdaptable> status = HgResolveClient.list(hgRoot);
		Set<IResource> changed = new HashSet<IResource>();
		IPath parentPath = new Path(hgRoot.getAbsolutePath());
		Set<IPath> members = getPathChildrenFromCache(parentPath );
		for (IPath childPath : members) {
			if(removeConflict(childPath)){
				IFile fileHandle = ResourceUtils.getFileHandle(childPath);
				if(fileHandle != null) {
					changed.add(fileHandle);
				}
			}
		}
		for (FlaggedAdaptable flaggedAdaptable : status) {
			IFile file = (IFile) flaggedAdaptable.getAdapter(IFile.class);
			if (flaggedAdaptable.getFlag() == CHAR_UNRESOLVED) {
				changed.addAll(addConflict(file));
			}
		}
		return changed;
	}

	Pattern NEWLINE = Pattern.compile("\n");

	/**
	 * @param lines must contain file paths as paths relative to the hg root
	 * @param pathMap multiple projects (from this hg root) as input
	 * @return set with resources to refresh
	 */
	private Set<IResource> parseStatus(HgRoot root, Map<IProject, IPath> pathMap, String[] lines) {
		long start = System.currentTimeMillis();

		// we need the project for performance reasons - gotta hand it to
		// addToProjectResources
		Set<IResource> changed = new HashSet<IResource>();
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();

		List<String> strangeStates = new ArrayList<String>();
		IPath hgRootPath = new Path(root.getAbsolutePath());

		for (String line : lines) {
			if(line.length() <= 2){
				strangeStates.add(line);
				continue;
			}

			int bit = getBit(line.charAt(0));
			if(bit == BIT_IMPOSSIBLE){
				strangeStates.add(line);
				continue;
			}
			String localName = line.substring(2);
			IResource member = findMember(pathMap, hgRootPath, localName);

			// doesn't belong to our project (can happen if root is above project level)
			// or simply deleted, so can't be found...
			if (member == null) {
				if(bit == BIT_REMOVED){
					IPath path = hgRootPath.append(localName);
					// creates a handle to non-existent file. This is ok.
					member = workspaceRoot.getFileForLocation(path);
					if(member == null) {
						continue;
					}
				} else {
					continue;
				}
			}

			Integer bitSet;
			boolean ignoredHint = Team.isIgnoredHint(member);
			if (ignoredHint) {
				bitSet = _IGNORE;
			} else {
				bitSet = Integer.valueOf(bit);
				changed.add(member);
			}
			setStatus(member.getLocation(), bitSet, member.getType() == IResource.FOLDER);

			changed.addAll(setStatusToAncestors(member, bitSet));
		}
		if(strangeStates.size() > 0){
			IStatus [] states = new IStatus[strangeStates.size()];
			for (int i = 0; i < states.length; i++) {
				states[i] = MercurialEclipsePlugin.createStatus(strangeStates.get(i), IStatus.OK, IStatus.INFO, null);
			}
			String message = "Strange status received from hg";
			MultiStatus st = new MultiStatus(MercurialEclipsePlugin.ID, IStatus.OK, states,
					message, new Exception(message));
			MercurialEclipsePlugin.getDefault().getLog().log(st);
		}
		if(debug){
			System.out.println("Parse status took: " + (System.currentTimeMillis() - start));
		}
		return changed;
	}

	private IResource findMember(Map<IProject, IPath> pathMap, IPath hgRootPath, String repoRelPath) {
		// determine absolute path
		IPath path = hgRootPath.append(repoRelPath);
		int rootegmentCount = hgRootPath.segmentCount();
		Set<Entry<IProject,IPath>> set = pathMap.entrySet();
		for (Entry<IProject, IPath> entry : set) {
			IPath projectLocation = entry.getValue();
			// determine project relative path
			int equalSegments = path.matchingFirstSegments(projectLocation);
			if(equalSegments > rootegmentCount || pathMap.size() == 1) {
				IPath segments = path.removeFirstSegments(equalSegments);
				return entry.getKey().findMember(segments);
			}
		}
		return null;
	}

	private void setStatus(IPath location, Integer status, boolean isDir) {
		statusMap.put(location, status);
		bitMap.put(location, status);
		if(isDir){
			bitMap.put(location, Integer.valueOf(BIT_DIR));
		}
	}

	private Set<IResource> setStatusToAncestors(IResource resource, Integer resourceBitSet) {
		Set<IResource> ancestors = new HashSet<IResource>();
		boolean computeDeep = isComputeDeepStatus();
		IContainer parent = resource.getParent();
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		for (; parent != null && parent != root; parent = parent.getParent()) {
			IPath location = parent.getLocation();
			int parentBitSet = 0;
			{
				Integer parentBits = statusMap.get(location);
				if(parentBits != null){
					parentBitSet = parentBits.intValue();
				}
			}
			int cloneBitSet = resourceBitSet.intValue();

			// should not propagate ignores states to parents
			// TODO issue 237: "two status feature"
			cloneBitSet = Bits.clear(cloneBitSet, IGNORED_MASK);
			boolean intersects = Bits.contains(resourceBitSet.intValue(), MODIFIED_MASK);
			if(intersects) {
				cloneBitSet |= BIT_MODIFIED;
			} else {
				cloneBitSet |= BIT_CLEAN;
			}

			if (computeDeep && resource.getType() != IResource.PROJECT) {
				if (!Bits.contains(cloneBitSet, BIT_MODIFIED) &&
						parent.isAccessible() && !parent.isTeamPrivateMember() && !parent.isDerived()) {
					MemberStatusVisitor visitor = new MemberStatusVisitor(location, cloneBitSet);
					// we have to traverse all "dirty" resources and change parent state to "dirty"...
					boolean visit = checkChildrenFor(location, visitor, BIT_MODIFIED);
					if(visit){
						visit = checkChildrenFor(location, visitor, BIT_UNKNOWN);
					}
					if(visit){
						visit = checkChildrenFor(location, visitor, BIT_ADDED);
					}
					if(visit){
						visit = checkChildrenFor(location, visitor, BIT_REMOVED);
					}
					if(visit){
						visit = checkChildrenFor(location, visitor, BIT_MISSING);
					}
					cloneBitSet = visitor.bitSet;
				}
			} else {
				cloneBitSet |= parentBitSet;
			}
			setStatus(location, Integer.valueOf(cloneBitSet), parent.getType() == IResource.FOLDER);
			ancestors.add(parent);
		}
		return ancestors;
	}

	private boolean checkChildrenFor(IPath location, MemberStatusVisitor visitor, int stateBit) {
		Set<IPath> resources = getPaths(stateBit, location);
		if(resources == null){
			return true;
		}
		for (IPath child : resources) {
			boolean continueVisit = visitor.visit(child);
			if(!continueVisit){
				return false;
			}
		}
		return true;
	}

	private boolean isComputeDeepStatus() {
		return computeDeepStatus;
	}

	private int getBit(char status) {
		switch (status) {
		case '!':
			return BIT_MISSING;
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
			return BIT_IMPOSSIBLE;
		}
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
		ProjectUpdateJob updateJob = new ProjectUpdateJob(removedSet, changedSet, project, addedSet);
		Job[] jobs = Job.getJobManager().find(ProjectUpdateJob.class);
		for (Job job : jobs) {
			if(updateJob.equals(job)){
				job.cancel();
				if(debug){
					System.out.println("Status cache update cancelled for: "
							+ ((ProjectUpdateJob) job).project.getName());
				}
			}
		}
		updateJob.schedule(100);
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

		HgRoot root = AbstractClient.getHgRoot(project);
		for (Iterator<IResource> iterator = resources.iterator(); iterator.hasNext();) {
			IResource resource = iterator.next();

			// status for single resource is batched
			if (!resource.isTeamPrivateMember()) {
				currentBatch.add(resource);
			}
			if (currentBatch.size() % batchSize == 0 || !iterator.hasNext()) {
				// call hg with batch
				synchronized (statusUpdateLock) {
					for (IResource curr : currentBatch) {
						boolean unknown = (curr instanceof IContainer) || isUnknown(curr);
						clearStatusCache(curr, false);
						if (unknown && !curr.exists()) {
							// remember parents of deleted files: we must update their state
							IContainer directory = ResourceUtils.getFirstExistingDirectory(curr);
							while(directory != null) {
								changed.add(directory);
								IPath parentPath = directory.getLocation();
								bitMap.remove(parentPath);
								statusMap.remove(parentPath);
								directory = ResourceUtils.getFirstExistingDirectory(directory.getParent());
							}
							// recursive recalculate parents state
							// TODO better to combine it with parse status below...
							setStatusToAncestors(curr, _CLEAN);
						}
					}
					String output = HgStatusClient.getStatusWithoutIgnored(root, currentBatch);
					String[] lines = NEWLINE.split(output);
					Map<IProject, IPath> pathMap = new HashMap<IProject, IPath>();
					pathMap.put(project, project.getLocation());
					changed.addAll(parseStatus(root, pathMap, lines));
				}
				currentBatch.clear();
			}
		}
		if(!resources.isEmpty()) {
			changed.addAll(checkForConflict(project));
		}
		notifyChanged(changed, false);
		return changed;
	}

	public void clearStatusCache(IResource resource, boolean notify) {
		Set<IResource> members = null;
		if(notify){
			members = getLocalMembers(resource);
			members.add(resource);
		}
		synchronized (statusUpdateLock) {
			IPath parentPath = ResourceUtils.getPath(resource);
			if(resource instanceof IContainer){
				// same can be done via getChildrenFromCache(resource), but we
				// iterating/removing over keyset directly to reduce memory consumption
				Set<IPath> entrySet = statusMap.keySet();
				Iterator<IPath> it = entrySet.iterator();
				while (it.hasNext()) {
					IPath path = it.next();
					if(path != null && parentPath.isPrefixOf(path)) {
						it.remove();
						bitMap.remove(path);
					}
				}
			} else {
				bitMap.remove(parentPath);
				statusMap.remove(parentPath);
			}
		}
		if(notify){
			notifyChanged(members, false);
		}
	}

	private int getStatusBatchSize() {
		return statusBatchSize;
	}

	/**
	 * @param resource
	 * @return never null. Set will contain all known files under the given directory,
	 * or the file itself if given resource is not a directory
	 */
	public Set<IResource> getLocalMembers(IResource resource) {
		Set<IResource> members = new HashSet<IResource>();
		if(resource instanceof IContainer){
			IContainer container = (IContainer) resource;
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			Set<IPath> children = getChildrenFromCache(container);
			for (IPath path : children) {
				// TODO the if block below costs a lot of time because it requires file I/O
				// hovewer, without it, Eclipse generates a dummy handle to a FILE instead to the directory...
//				File file = path.toFile();
//				if(file.isDirectory()){
//					continue;
//				}
				IFile iFile = root.getFileForLocation(path);
				if(iFile != null) {
					members.add(iFile);
				}
			}
		} else {
			members.add(resource);
		}
		return members;
	}

	@Override
	protected void projectDeletedOrClosed(IProject project) {
		clear(project, false);
		knownStatus.remove(project);
	}

	public void clear(HgRoot root, boolean notify) {
		Set<IProject> projects = ResourceUtils.getProjects(root);
		for (IProject project : projects) {
			clearStatusCache(project, false);
			if(notify) {
				notifyChanged(project, false);
			}
		}
	}

	public void clear(IProject project, boolean notify) {
		clearStatusCache(project, false);
		if(notify) {
			notifyChanged(project, false);
		}
	}

	/**
	 * Sets conflict marker on resource status
	 */
	private Set<IResource> addConflict(IResource local) {
		IPath location = local.getLocation();
		Integer status = statusMap.get(location);
		boolean isDir = local.getType() == IResource.FOLDER;
		if(status == null){
			status = _CONFLICT;
			setStatus(location, _CONFLICT, isDir);
		} else {
			status = Integer.valueOf(status.intValue() | BIT_CONFLICT);
			setStatus(location, status, isDir);
		}
		Set<IResource> changed = setStatusToAncestors(local, status);
		changed.add(local);
		return changed;
	}

	/**
	 * Removes conflict marker on resource status
	 *
	 * @param local non null
	 * @return true if there was a conflict and now it is removed
	 */
	private boolean removeConflict(IPath local) {
		Integer statusInt = statusMap.get(local);
		if(statusInt == null){
			return false;
		}
		int status = statusInt.intValue();
		if(Bits.contains(status, BIT_CONFLICT)) {
			status = Bits.clear(status, BIT_CONFLICT);
			setStatus(local, Integer.valueOf(status), false);
			return true;
		}
		return false;
	}

	@Override
	protected void configureFromPreferences(IPreferenceStore store){
		computeDeepStatus = store.getBoolean(MercurialPreferenceConstants.RESOURCE_DECORATOR_COMPUTE_DEEP_STATUS);
		// TODO: group batches by repo root

		statusBatchSize = store.getInt(MercurialPreferenceConstants.STATUS_BATCH_SIZE);// STATUS_BATCH_SIZE;
		if (statusBatchSize <= 0) {
			store.setValue(MercurialPreferenceConstants.STATUS_BATCH_SIZE, STATUS_BATCH_SIZE);
			statusBatchSize = STATUS_BATCH_SIZE;
			MercurialEclipsePlugin.logWarning(Messages.mercurialStatusCache_BatchSizeForStatusCommandNotCorrect, null);
		}
	}

}
