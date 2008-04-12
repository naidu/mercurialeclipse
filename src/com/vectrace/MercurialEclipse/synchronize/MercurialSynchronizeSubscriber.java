/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian Doetsch	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;
import org.eclipse.team.ui.synchronize.ISynchronizeScope;

import com.vectrace.MercurialEclipse.HgRevision;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.team.IStorageMercurialRevision;
import com.vectrace.MercurialEclipse.team.MercurialStatusCache;

public class MercurialSynchronizeSubscriber extends Subscriber {
	private static MercurialStatusCache statusCache = MercurialStatusCache
			.getInstance();
	private ISynchronizeScope myScope;
	private IResource[] myRoots;

	public MercurialSynchronizeSubscriber(ISynchronizeScope scope) {
		this.myScope = scope;
	}

	@Override
	public String getName() {
		return "Mercurial Repository Watcher";
	}

	@Override
	public IResourceVariantComparator getResourceComparator() {
		return MercurialResourceVariantComparator.getInstance();
	}

	@Override
	public SyncInfo getSyncInfo(IResource resource) throws TeamException {
		ChangeSet csBase = statusCache.getVersion(resource);
		ChangeSet csRemote = statusCache.getIncomingVersion(resource);
		IResourceVariant base;
		IResourceVariant remote;
		if (csBase != null) {
			HgRevision rv = csBase.getRevision();

			IStorageMercurialRevision baseIStorage = new IStorageMercurialRevision(
					resource, rv.getRevision() + "", rv.getChangeset());

			base = new MercurialResourceVariant(baseIStorage);

			HgRevision rvRemote;
			IStorageMercurialRevision remoteIStorage;
			if (csRemote != null) {
				rvRemote = csRemote.getRevision();
				remoteIStorage = new IStorageMercurialRevision(resource,
						rvRemote + "", rvRemote.getChangeset());
			} else {
				remoteIStorage = baseIStorage;
			}

			remote = new MercurialResourceVariant(remoteIStorage);

			SyncInfo info = new MercurialSyncInfo(resource, base, remote,
					MercurialResourceVariantComparator.getInstance());

			info.init();
			return info;
		}
		return null;

	}

	@Override
	public boolean isSupervised(IResource resource) throws TeamException {
		return statusCache.isStatusKnown(resource);
	}

	@Override
	public IResource[] members(IResource resource) throws TeamException {
		Set<IResource> members = new HashSet<IResource>();
		IResource[] localMembers = statusCache.getLocalMembers(resource);
		IResource[] remoteMembers = statusCache.getIncomingMembers(resource);
		members.addAll(Arrays.asList(localMembers));
		if (remoteMembers.length > 0) {
			members.addAll(Arrays.asList(remoteMembers));
		}
		return members.toArray(new IResource[members.size()]);
	}

	@Override
	public void refresh(IResource[] resources, int depth,
			IProgressMonitor monitor) throws TeamException {
		IResource[] toRefresh = resources;
		if (toRefresh == null) {
			toRefresh = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		}
		Set<IProject> refreshed = new HashSet<IProject>(toRefresh.length);
		monitor.beginTask("Refreshing resources...", toRefresh.length);
		for (IResource resource : toRefresh) {
			if (monitor.isCanceled()) {
				return;
			}
			IProject project = resource.getProject();
			if (refreshed.contains(project)) {
				monitor.worked(1);
				continue;
			}
			statusCache.refresh(project);
			refreshed.add(project);
			monitor.worked(1);
		}
		monitor.done();
	}

	@Override
	public IResource[] roots() {
		if (myRoots == null) {
			if (myScope.getRoots() != null) {
				myRoots = myScope.getRoots();
			} else {
				myRoots = ResourcesPlugin.getWorkspace().getRoot()
						.getProjects();
			}
		}
		return myRoots;
	}

}
