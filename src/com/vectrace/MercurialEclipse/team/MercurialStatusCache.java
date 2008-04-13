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

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.core.TeamException;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeWorkspaceJob;
import com.vectrace.MercurialEclipse.commands.HgIncomingClient;
import com.vectrace.MercurialEclipse.commands.HgLogClient;
import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

/**
 * Caches the Mercurial Status of each file and offers methods for retrieving,
 * clearing and refreshing repository state.
 * 
 * @author Bastian Doetsch
 * 
 */
public class MercurialStatusCache extends Observable {

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

	private static Map<IResource, SortedMap<Integer, ChangeSet>> localChangeSets;

	private static Map<IProject, Set<IResource>> projectResources;

	private static Map<IResource, SortedMap<Integer, ChangeSet>> incomingChangeSets;
	private boolean localUpdateInProgress = false;
	private boolean remoteUpdateInProgress = false;

	private MercurialStatusCache() {
		try {
			knownStatus = new HashSet<IProject>();
			localChangeSets = new HashMap<IResource, SortedMap<Integer, ChangeSet>>();
			projectResources = new HashMap<IProject, Set<IResource>>();
			incomingChangeSets = new HashMap<IResource, SortedMap<Integer, ChangeSet>>();
			refresh();
		} catch (TeamException e) {
			MercurialEclipsePlugin.logError(e);
		}
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
	public void clear() {
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
		SortedMap<Integer, ChangeSet> revisions = getLocalChangeSets(objectResource);
		if (revisions != null && revisions.size() > 0) {
			return revisions.get(revisions.lastKey());
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

	public SortedMap<Integer, ChangeSet> getLocalChangeSets(
			IResource objectResource) throws HgException {
		SortedMap<Integer, ChangeSet> revisions = localChangeSets
				.get(objectResource);
		if (revisions == null) {
			if (objectResource.getType() != IResource.FOLDER
					&& isSupervised(objectResource)) {
				refreshAllLocalRevisions(objectResource.getProject());
				revisions = localChangeSets.get(objectResource);
			}
		}
		return revisions;
	}

	public SortedMap<Integer, ChangeSet> getIncomingChangeSets(
			IResource objectResource) throws HgException {
		if (remoteUpdateInProgress) {
			synchronized (incomingChangeSets) {
				// wait...
			}
		}
		SortedMap<Integer, ChangeSet> revisions = incomingChangeSets
				.get(objectResource);
		if (revisions == null) {
			refreshIncomingChangeSets(objectResource.getProject());
		}
		return incomingChangeSets.get(objectResource);
	}

	/*
	 * public SortedMap<Integer, ChangeSet> refreshChangedLocalChangeSets(
	 * IResource objectResource) throws HgException { int status = BIT_UNKNOWN;
	 * BitSet bitSet = getStatus(objectResource); if (bitSet != null) { status =
	 * bitSet.length() - 1; } // only proceed if there were changes or we are at
	 * initial load. switch (status) { case BIT_UNKNOWN: case BIT_IGNORE: return
	 * null; case BIT_CLEAN: if (isLocallyKnown(objectResource)) { return
	 * localChangeSets.get(objectResource); } } // revisions =
	 * loadLocalChangeSetInformation(objectResource);
	 * refreshAllLocalRevisions(objectResource.getProject()); return
	 * localChangeSets.get(objectResource); }
	 */

	/*
	 * private SortedMap<Integer, ChangeSet> loadLocalChangeSetInformation(
	 * IResource objectResource) throws HgException { SortedMap<Integer,
	 * ChangeSet> revisions; revisions = new TreeMap<Integer, ChangeSet>();
	 * ChangeSet[] changeSets;
	 * 
	 * if (objectResource.getType() == IResource.PROJECT ||
	 * objectResource.getType() == IResource.FOLDER) {
	 * 
	 * changeSets = new ProjectDataLoader(objectResource.getProject())
	 * .getRevisions(); } else { changeSets = new FileDataLoader((IFile)
	 * objectResource) .getRevisions(); }
	 * 
	 * if (changeSets != null) { for (ChangeSet changeSet : changeSets) {
	 * revisions.put(Integer.valueOf(changeSet.getChangesetIndex()), changeSet); }
	 * localChangeSets.put(objectResource, revisions); } return revisions; }
	 */

	/**
	 * Refreshes sync status of given project by questioning Mercurial.
	 * 
	 * @param project
	 * @throws TeamException
	 */
	public void refresh(final IProject project) throws TeamException {
		/* hg status on project (all files) instead of per file basis */
		try {
			// set status
			refreshStatus(project);

			new SafeWorkspaceJob("Updating status and version cache...") {
				@Override
				protected IStatus runSafe(IProgressMonitor monitor) {
					// set version
					monitor
							.beginTask("Updating status and version cache...",
									3);
					try {
						monitor.subTask("Loading local revisions...");
						refreshAllLocalRevisions(project);
						monitor.worked(1);
						// incoming
						monitor
								.subTask("Loading remote revisions from repositories...");
						refreshIncomingChangeSets(project);
						monitor.worked(1);
						setChanged();
						notifyObservers();
						monitor.worked(1);
					} catch (HgException e) {
						MercurialEclipsePlugin.logError(e);
					}
					monitor.done();
					return super.runSafe(monitor);
				}

			}.schedule();
		} catch (HgException e) {
			throw new TeamException(e.getMessage(), e);
		}
	}

	/**
	 * @param project
	 * @throws HgException
	 */
	public void refreshStatus(final IProject project) throws HgException {
		synchronized (statusMap) {
			parseStatusCommand(project, HgStatusClient.getStatus(project));
		}
		setChanged();
		notifyObservers(project);

	}

	private SortedMap<Integer, ChangeSet> refreshIncomingChangeSets(
			IProject project) throws HgException {
		synchronized (incomingChangeSets) {
			remoteUpdateInProgress = true;
			Set<HgRepositoryLocation> repositories = MercurialEclipsePlugin
					.getRepoManager().getAllRepoLocations();

			if (repositories == null) {
				return null;
			}

			for (HgRepositoryLocation hgRepositoryLocation : repositories) {

				Map<IResource, SortedSet<ChangeSet>> incomingResources = HgIncomingClient
						.getHgIncoming(project, hgRepositoryLocation);

				if (incomingResources != null && incomingResources.size() > 0) {

					for (Iterator<IResource> iter = incomingResources.keySet()
							.iterator(); iter.hasNext();) {
						IResource res = iter.next();
						SortedSet<ChangeSet> changes = incomingResources
								.get(res);

						if (changes != null && changes.size() > 0) {
							SortedMap<Integer, ChangeSet> revisions = new TreeMap<Integer, ChangeSet>();
							ChangeSet[] changeSets = changes
									.toArray(new ChangeSet[changes.size()]);

							if (changeSets != null) {
								for (ChangeSet changeSet : changeSets) {
									revisions.put(Integer.valueOf(changeSet
											.getChangesetIndex()), changeSet);
								}
							}
							incomingChangeSets.put(res, revisions);
						}
					}
				}
			}
			remoteUpdateInProgress = false;
			return incomingChangeSets.get(project);
		}
	}

	/**
	 * @param output
	 */
	private void parseStatusCommand(IProject ctr, String output) {
		IContainer ctrParent = ctr.getParent();
		knownStatus.add(ctr);
		Scanner scanner = new Scanner(output);
		while (scanner.hasNext()) {
			String status = scanner.next();
			String localName = scanner.nextLine();
			IResource member = ctr.getFile(localName.trim());

			BitSet bitSet = new BitSet();
			bitSet.set(getBitIndex(status.charAt(0)));
			statusMap.put(member, bitSet);

			addToProjectResources(member);

			// ancestors
			for (IResource parent = member.getParent(); parent != ctrParent; parent = parent
					.getParent()) {
				BitSet parentBitSet = statusMap.get(parent);
				if (parentBitSet != null) {
					bitSet = (BitSet) bitSet.clone();
					bitSet.or(parentBitSet);
				}
				statusMap.put(parent, bitSet);
			}
		}
	}

	private void addToProjectResources(IResource member) {
		if (member.getType() == IResource.PROJECT
				|| member.getType() == IResource.FOLDER) {
			return;
		}
		Set<IResource> set = projectResources.get(member.getProject());
		if (set == null) {
			set = new HashSet<IResource>();
		}
		set.add(member);
		projectResources.put(member.getProject(), set);
	}

	private final int getBitIndex(char status) {
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
	 * Mercurial.
	 * 
	 * @throws TeamException
	 *             if status check encountered problems.
	 */
	public void refresh() throws TeamException {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
				.getProjects();
		for (IProject project : projects) {
			refresh(project);
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
		IContainer container = (IContainer) resource;

		Set<IResource> members = new HashSet<IResource>();

		switch (resource.getType()) {
		case IResource.FILE:
			break;
		case IResource.PROJECT:
			Set<IResource> resources = projectResources.get(resource);
			members.addAll(resources);
			break;
		case IResource.FOLDER:
			for (Iterator<IResource> iterator = statusMap.keySet().iterator(); iterator
					.hasNext();) {
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

	public IResource[] getIncomingMembers(IResource resource) {
		return incomingChangeSets.keySet().toArray(
				new IResource[incomingChangeSets.keySet().size()]);
	}

	public ChangeSet getNewestIncomingChangeSet(IResource resource)
			throws HgException {
		if (isSupervised(resource)) {
			if (remoteUpdateInProgress) {
				synchronized (incomingChangeSets) {
					// wait for update...
				}
			}
			SortedMap<Integer, ChangeSet> revisions = incomingChangeSets
					.get(resource);
			if (revisions != null && revisions.size() > 0) {
				return revisions.get(revisions.lastKey());
			}
		}
		return null;
	}

	public void refreshAllLocalRevisions(IProject project) throws HgException {
		synchronized (localChangeSets) {
			localUpdateInProgress = true;
			Map<IResource, SortedSet<ChangeSet>> revisions = HgLogClient
					.getCompleteProjectLog(project);
			for (Iterator<IResource> iter = revisions.keySet().iterator(); iter
					.hasNext();) {
				IResource res = iter.next();
				SortedSet<ChangeSet> changes = revisions.get(res);
				if (changes != null && changes.size() > 0) {
					SortedMap<Integer, ChangeSet> mercRevisions = new TreeMap<Integer, ChangeSet>();
					ChangeSet[] changeSets = changes
							.toArray(new ChangeSet[changes.size()]);

					if (changeSets != null) {
						for (ChangeSet changeSet : changeSets) {
							mercRevisions.put(Integer.valueOf(changeSet
									.getChangesetIndex()), changeSet);
						}
					}
					BitSet bitSet = getStatus(res);
					int status = BIT_UNKNOWN;
					if (bitSet != null) {
						status = bitSet.length() - 1;
					}
					// only proceed if there were changes or we are at initial
					// load.
					switch (status) {
					case BIT_IGNORE:
					case BIT_UNKNOWN:
						continue;
					}
					localChangeSets.put(res, mercRevisions);
				}
			}
			localUpdateInProgress = false;
		}
	}
}
