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
import java.util.TreeMap;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.team.core.TeamException;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgIdentClient;
import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.storage.FileDataLoader;

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

	private MercurialStatusCache() {
		try {
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

	/** Used to store the last known status of a resource */
	private static Map<IResource, BitSet> statusMap = new HashMap<IResource, BitSet>();

	/** Used to store which projects have already been parsed */
	private static Set<IProject> knownStatus = new HashSet<IProject>();

	private static Map<IResource, SortedMap<Integer, ChangeSet>> versions = new HashMap<IResource, SortedMap<Integer, ChangeSet>>();

	private static Map<IProject, Set<IResource>> projectResources = new HashMap<IProject, Set<IResource>>();

	private static Map<IResource, String> incomingVersions = new HashMap<IResource, String>();

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
		incomingVersions.clear();
		versions.clear();
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
	public boolean isVersionKnown(IResource objectResource) {
		return versions.containsKey(objectResource);
	}

	/**
	 * Gets version for given resource.
	 * 
	 * @param objectResource
	 *            the resource to get status for.
	 * @return a String with version information.
	 * @throws HgException
	 */
	public ChangeSet getVersion(IResource objectResource) throws HgException {
		SortedMap<Integer, ChangeSet> revisions = getChangeSets(objectResource);
		if (revisions != null && revisions.size() > 0) {
			return revisions.get(revisions.lastKey());
		}
		return null;
	}

	public SortedMap<Integer, ChangeSet> getChangeSets(IResource objectResource)
			throws HgException {
		SortedMap<Integer, ChangeSet> revisions = versions.get(objectResource);
		if (revisions == null) {
			revisions = refreshChangeSets(objectResource);
		}
		return revisions;
	}

	private SortedMap<Integer, ChangeSet> refreshChangeSets(
			IResource objectResource) throws HgException {
		SortedMap<Integer, ChangeSet> revisions;
		switch (getStatus(objectResource).length() - 1) {
		case BIT_UNKNOWN:
		case BIT_IGNORE:
			return null;
		}
		revisions = new TreeMap<Integer, ChangeSet>();
		ChangeSet[] changeSets;
		if (objectResource.getType() == IResource.PROJECT
				|| objectResource.getType() == IResource.FOLDER) {

			String projectChangeSet = HgIdentClient
					.getCurrentRevision((IContainer) objectResource);

			String[] changeSetStrings = HgIdentClient
					.getChangeSets(projectChangeSet);

			changeSets = new ChangeSet[changeSetStrings.length];

			for (int i = 0; i < changeSets.length; i++) {
				String[] parts = changeSetStrings[i].trim().split(":");
				String rev = parts[0];
				String hex = parts[1];

				if (rev.endsWith("+")) {
					rev = rev.substring(0, rev.length() - 1);
				}

				if (hex.endsWith("+")) {
					hex = hex.substring(0, hex.length() - 1);
				}

				changeSets[i] = new ChangeSet(Integer.parseInt(rev), hex, null,
						null);
			}

		} else {
			changeSets = new FileDataLoader((IFile) objectResource)
					.getRevisions();
		}

		if (changeSets != null) {
			for (ChangeSet changeSet : changeSets) {
				revisions.put(Integer.valueOf(changeSet.getChangesetIndex()),
						changeSet);
			}
			versions.put(objectResource, revisions);
		}

		return revisions;
	}

	/**
	 * Refreshes sync status of given project by questioning Mercurial.
	 * 
	 * @param project
	 * @throws TeamException
	 */
	public void refresh(IProject project) throws TeamException {
		/* hg status on project (all files) instead of per file basis */
		try {
			// set status
			parseStatusCommand(project, HgStatusClient.getStatus(project));
			setChanged();
			notifyObservers(project);

			// set version
			refreshChangeSets(project);

			// incoming
			incomingVersions.put(project, getRemoteRepositoryVersion(project));
			setChanged();
			notifyObservers();
		} catch (HgException e) {
			throw new TeamException(e.getMessage(), e);
		}
	}

	private String getRemoteRepositoryVersion(IProject project) {

		return null;
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
				// IResource currParent = member.getParent();
				// while (currParent != null) {
				// if (currParent.equals(resource)) {
				// members.add(member);
				// break;
				// }
				// currParent = currParent.getParent();
				// }
				IResource foundMember = container.findMember(member.getName());
				if (foundMember != null && foundMember.equals(member)) {
					members.add(member);
				}
			}
		}

		return members.toArray(new IResource[members.size()]);
	}

	public IResource[] getIncomingMembers(IResource resource) {
		// TODO extract members not known from incoming that are members of
		// resource
		return new IResource[0];
	}

	public String getIncomingVersion(IResource resource) {
		return null;
	}
}
