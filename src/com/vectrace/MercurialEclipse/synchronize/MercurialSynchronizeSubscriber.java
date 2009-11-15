/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian Doetsch	implementation
 * Andrei Loskutov (Intland) - bugfixes
 * Adam Berkes (Intland) - bugfixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IFile;
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
import com.vectrace.MercurialEclipse.commands.HgBranchClient;
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
import com.vectrace.MercurialEclipse.utils.Bits;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class MercurialSynchronizeSubscriber extends Subscriber /*implements Observer*/ {

	private static final String UNCOMMITTED_BRANCH = "_UNCOMMITTED_BRANCH_!!!_";

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
	/** key is hg root, value is the *current* changeset of this root */
	private final Map<HgRoot, String> currentBranchMap;

	private ISubscriberChangeEvent[] lastEvents;

	private MercurialSynchronizeParticipant participant;

	public MercurialSynchronizeSubscriber(RepositorySynchronizationScope synchronizationScope) {
		Assert.isNotNull(synchronizationScope);
		currentCsMap = new ConcurrentHashMap<HgRoot, String>();
		currentBranchMap = new ConcurrentHashMap<HgRoot, String>();
		debug = MercurialEclipsePlugin.getDefault().isDebugging();
		scope = synchronizationScope;
		synchronizationScope.setSubscriber(this);
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

	private static String getRealBranchName(String currBranch){
		if(currBranch == null){
			return Branch.DEFAULT;
		}
		if(currBranch.startsWith(UNCOMMITTED_BRANCH)){
			return currBranch.substring(UNCOMMITTED_BRANCH.length());
		}
		return currBranch;
	}

	@Override
	public SyncInfo getSyncInfo(IResource resource) {
		if (!isInteresting(resource)) {
			return null;
		}
		IFile file = (IFile) resource;
		HgRoot root;
		try {
			root = MercurialTeamProvider.getHgRoot(resource);
		} catch (HgException e1) {
			MercurialEclipsePlugin.logError(e1);
			return null;
		}
		String currentBranch = currentBranchMap.get(root);
		if(currentBranch == null){
			currentBranch = updateBranchMap(root, MercurialTeamProvider.getCurrentBranch(resource));
		}
		boolean uncommittedBranch = currentBranch.startsWith(UNCOMMITTED_BRANCH);
		if(uncommittedBranch) {
			currentBranch = getRealBranchName(currentBranch);
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

			csOutgoing = OUTGOING_CACHE.getNewestChangeSet(resource, getRepo(), currentBranch);
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
			return null;
		} finally {
			sema.release();
		}

		MercurialRevisionStorage outgoingIStorage;
		IResourceVariant outgoing;
		// determine outgoing revision
		boolean hasOutgoingChanges = false;
		boolean hasIncomingChanges = false;
		Integer status = STATUS_CACHE.getStatus(resource);
		int sMask = status != null? status.intValue() : 0;
		if (csOutgoing != null) {
			outgoingIStorage = new MercurialRevisionStorage(file,
					csOutgoing.getRevision().getRevision(),
					csOutgoing.getChangeset(), csOutgoing);

			outgoing = new MercurialResourceVariant(outgoingIStorage);
			hasOutgoingChanges = true;
		} else {
			boolean exists = resource.exists();
			// if outgoing != null it's our base, else we gotta construct one
			if (exists && !Bits.contains(sMask, MercurialStatusCache.BIT_ADDED)
					|| (!exists && Bits.contains(sMask, MercurialStatusCache.BIT_REMOVED))) {

				try {
					// Find current working directory changeset (not head)

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
				outgoingIStorage = new MercurialRevisionStorage(file,
						csOutgoing.getChangesetIndex(), csOutgoing.getChangeset(), csOutgoing);

				outgoing = new MercurialResourceVariant(outgoingIStorage);
			} else {
				// new incoming file - no local available
				outgoingIStorage = null;
				outgoing = null;
			}
		}

		// determine incoming revision get newest incoming changeset
		ChangeSet csIncoming = null;
		// do not call incoming if the branch is known only locally
		if(!uncommittedBranch){
			try {
				if(!sema.tryAcquire(60 * 5, TimeUnit.SECONDS)){
					// waiting didn't worked for us...
					return null;
				}
			} catch (InterruptedException e) {
				MercurialEclipsePlugin.logError(e);
				return null;
			}
			try {
				// this can trigger a refresh and a call to the remote server...
				csIncoming = INCOMING_CACHE.getNewestChangeSet(resource, getRepo(), currentBranch);
			} catch (HgException e) {
				MercurialEclipsePlugin.logError(e);
				return null;
			} finally {
				sema.release();
			}
		}

		MercurialRevisionStorage incomingIStorage;
		int syncMode = -1;
		if (csIncoming != null) {
			hasIncomingChanges = true;
			boolean fileRemoved = csIncoming.isRemoved(resource);
			if(fileRemoved){
				incomingIStorage = null;
			} else {
				incomingIStorage = getIncomingIStorage(file, csIncoming);
			}
		} else {
			if(!hasOutgoingChanges && Bits.contains(sMask, MercurialStatusCache.BIT_CLEAN)){
				return null;
			}
			if(debug) {
				System.out.println("Visiting: " + resource);
			}
			// if no incoming revision, incoming = base/outgoing

			// TODO it seems that the line below causes NO DIFF shown if the outgoing
			// change consists from MULTIPLE changes on same file, see issue 10486
			// we have to get the parent of the first outgoing changeset on a given file here
			incomingIStorage = outgoingIStorage;

			// TODO validate if code below fixes the issue 10486
			try {
				SortedSet<ChangeSet> sets = OUTGOING_CACHE.getChangeSets(resource, getRepo(), currentBranch);
				int size = sets.size();

				// case where we have one outgoung changeset AND one not committed change
				if(size == 1 && !Bits.contains(sMask, MercurialStatusCache.BIT_CLEAN)){
					size ++;
				}
				if(size > 1){
					ChangeSet first = sets.first();
					String[] parents = first.getParents();
					String parentCs = null;
					if(parents.length > 0){
						parentCs = parents[0];
					} else {
						ChangeSet tmpCs = LOCAL_CACHE.getOrFetchChangeSetById(resource, first.getChangeset());
						if(tmpCs != null && tmpCs.getParents().length > 0){
							parentCs = tmpCs.getParents()[0];
						}
					}
					if(parentCs != null){
						ChangeSet baseChangeset = LOCAL_CACHE.getOrFetchChangeSetById(resource, parentCs);
						incomingIStorage = getIncomingIStorage(file, baseChangeset);
						// we change outgoing (base) to the first parent of the first outgoing changeset
						outgoing = new MercurialResourceVariant(incomingIStorage);
						syncMode = SyncInfo.OUTGOING | SyncInfo.CHANGE;
					}
				}
			} catch (HgException e) {
				MercurialEclipsePlugin.logError(e);
			}
		}

		if(!hasIncomingChanges && !hasOutgoingChanges && Bits.contains(sMask, MercurialStatusCache.BIT_CLEAN)){
			return null;
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
		SyncInfo info = new MercurialSyncInfo(resource, outgoing, incoming, getResourceComparator(), syncMode);

		try {
			info.init();
			return info;
		} catch (CoreException e) {
			MercurialEclipsePlugin.logError(e);
			return null;
		}
	}

	private boolean isInteresting(IResource resource) {
		return resource instanceof IFile
				&& MercurialTeamProvider.isHgTeamProviderFor(resource.getProject())
				&& (isSupervised(resource) || (!resource.exists()));
	}

	private MercurialRevisionStorage getIncomingIStorage(IFile resource,
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

		List<IResource> resources2 = Arrays.asList(resources);
		Map<IProject, List<IResource>> byProject = ResourceUtils.groupByProject(resources2);
		Set<IProject> projects = byProject.keySet();
		if(projects.isEmpty()){
			return;
		}

		Map<HgRoot, List<IResource>> byRoot = ResourceUtils.groupByRoot(new ArrayList<IResource>(byProject.keySet()));
		for (Entry<HgRoot, List<IResource>> entry : byRoot.entrySet()) {
			IResource res = entry.getValue().get(0);
			String branch = MercurialTeamProvider.getCurrentBranch(res);
			updateBranchMap(entry.getKey(), branch);
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
			HgRoot hgRoot = MercurialTeamProvider.getHgRoot(project);
			String currentBranch = currentBranchMap.get(hgRoot);
			boolean uncommittedBranch = currentBranch.startsWith(UNCOMMITTED_BRANCH);
			if(uncommittedBranch) {
				currentBranch = getRealBranchName(currentBranch);
			}
			try {
				sema.acquire();
				if(debug) {
					System.out.println("going to refresh local/in/out: " + project + ", depth: " + flag);
				}
				currentCsMap.remove(hgRoot);

				monitor.subTask(Messages.getString("MercurialSynchronizeSubscriber.refreshingLocal")); //$NON-NLS-1$
				refreshLocal(flag, monitor, project, forceRefresh);
				monitor.worked(1);
				if (monitor.isCanceled()) {
					return;
				}
				if(!uncommittedBranch){
					monitor.subTask(Messages.getString("MercurialSynchronizeSubscriber.refreshingIncoming")); //$NON-NLS-1$
					refreshIncoming(flag, resourcesToRefresh, project, repositoryLocation, forceRefresh, currentBranch);
					monitor.worked(1);
				}
				if (monitor.isCanceled()) {
					return;
				}
				monitor.subTask(Messages.getString("MercurialSynchronizeSubscriber.refreshingOutgoing")); //$NON-NLS-1$
				refreshOutgoing(flag, resourcesToRefresh, project, repositoryLocation, forceRefresh, currentBranch);
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

		// we need to send events only if WE trigger status update, not if the refresh
		// is called from the framework (like F5 hit by user)
		if(flag < 0){
			List<ISubscriberChangeEvent> changeEvents = createEvents(resources, resourcesToRefresh);
			monitor.worked(1);
			if (monitor.isCanceled()) {
				return;
			}

			monitor.subTask(Messages.getString("MercurialSynchronizeSubscriber.triggeringStatusCalc")); //$NON-NLS-1$
			lastEvents = changeEvents.toArray(new ISubscriberChangeEvent[changeEvents.size()]);
			fireTeamResourceChange(lastEvents);
			monitor.worked(1);
		}
		monitor.done();
	}

	private String updateBranchMap(HgRoot root, String branch) {
		if (!HgBranchClient.isKnownRemote(root, getRepo(), branch)) {
			branch = UNCOMMITTED_BRANCH + branch;
		}
		currentBranchMap.put(root, branch);
		return branch;
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
		if(debug) {
			System.out.println("created: " + changeEvents.size() + " change events");
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
			HgRepositoryLocation repositoryLocation, boolean forceRefresh, String branch) throws HgException {
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
			Set<IResource> incomingMembers = INCOMING_CACHE.getMembers(project, repositoryLocation, branch);
			resourcesToRefresh.addAll(incomingMembers);
		}
	}

	private void refreshOutgoing(int flag, Set<IResource> resourcesToRefresh, IProject project,
			HgRepositoryLocation repositoryLocation, boolean forceRefresh, String branch) throws HgException {
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
			Set<IResource> outgoingMembers = OUTGOING_CACHE.getMembers(project, repositoryLocation, branch);
			resourcesToRefresh.addAll(outgoingMembers);
		}
	}

	public RepositorySynchronizationScope getScope() {
		return scope;
	}

	protected HgRepositoryLocation getRepo(){
		return scope.getRepositoryLocation();
	}

	protected IProject[] getProjects() {
		return scope.getProjects();
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

	public void setParticipant(MercurialSynchronizeParticipant participant){
		this.participant = participant;
	}

	public MercurialSynchronizeParticipant getParticipant() {
		return participant;
	}

}
