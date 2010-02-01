/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeWorkspaceJob;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public final class RefreshWorkspaceStatusJob extends SafeWorkspaceJob {
	private final boolean refreshOnly;
	private final HgRoot root;

	public RefreshWorkspaceStatusJob(HgRoot root) {
		this(root, false);
	}

	public RefreshWorkspaceStatusJob(HgRoot root, boolean refreshOnly) {
		super("Refreshing status for " + root.getName() + "...");
		this.root = root;
		this.refreshOnly = refreshOnly;
	}

	@Override
	protected IStatus runSafe(IProgressMonitor monitor) {
		try {
			String branch = null;
			if(!refreshOnly) {
				branch = HgBranchClient.getActiveBranch(root);
			}
			Set<IProject> projects = ResourceUtils.getProjects(root);
			for (IProject project1 : projects) {
				refreshProject(monitor, project1, branch);
			}
			return super.runSafe(monitor);
		} catch (CoreException e) {
			MercurialEclipsePlugin.logError(e);
			return e.getStatus();
		}
	}

	/**
	 * @param monitor
	 * @throws HgException
	 * @throws CoreException
	 */
	private void refreshProject(IProgressMonitor monitor, IProject toRefresh, String branch) throws HgException, CoreException {
		if(!refreshOnly){
			// update branch name
			MercurialTeamProvider.setCurrentBranch(branch, toRefresh);

			// reset merge properties
			HgStatusClient.clearMergeStatus(toRefresh);
		}

		// refresh resources
		toRefresh.refreshLocal(IResource.DEPTH_INFINITE, monitor);
	}
}