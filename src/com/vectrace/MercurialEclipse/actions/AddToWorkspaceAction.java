/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch - Implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgCloneClient;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

/**
 * This action adds projects to the workspace using their unique Mercurial
 * reference strings from projectsets
 * 
 * @author Bastian Doetsch
 */
public class AddToWorkspaceAction extends WorkspaceModifyOperation {
	private String[] referenceStrings = null;
	private IProject[] projectsCreated = null;

	/**
	 * 
	 */
	public AddToWorkspaceAction() {
	}

	/**
	 * @param rule
	 */
	public AddToWorkspaceAction(ISchedulingRule rule) {
		super(rule);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.actions.WorkspaceModifyOperation#execute(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected void execute(IProgressMonitor monitor) throws CoreException,
			InvocationTargetException, InterruptedException {

		try {
			monitor.beginTask("Adding projects to workspace...",
					referenceStrings.length);

			IWorkspace workspace = ResourcesPlugin.getWorkspace();

			ArrayList<IProject> projects = new ArrayList<IProject>(
					referenceStrings.length);

			/*
			 * iterate over all reference strings and use them to create
			 * projects in the current workspace.
			 * 
			 * A reference string uses underscore as delimiter looks like this:
			 * 
			 * "MercurialEclipseProjectSet_ProjectName_RepositoryURLForClone"
			 * 
			 */

			for (String reference : referenceStrings) {
				if (monitor.isCanceled()) {
					break;
				}
				String[] referenceParts = reference.split("_");

				// Project name is stored in part 1
				IProject proj = workspace.getRoot().getProject(
						referenceParts[1]);

				// only new projects
				if (proj.exists() || proj.getLocation() != null) {
					MercurialEclipsePlugin.logInfo("Project" + proj.getName()
							+ " not imported. Already exists.", null);
					monitor.worked(1);
					continue;
				}

				// Repository-URL is stored in part 2
				HgRepositoryLocation location = new HgRepositoryLocation(
						referenceParts[2]);

				// Use action from clone command
//				RepositoryCloneAction action = new RepositoryCloneAction(null,
//						workspace, location, null, proj.getName(), null);				
//				action.run(monitor);
				
				HgCloneClient.clone(workspace, location, null, proj.getName());

				// FIXME: This is duplicate code (from CloneRepoWizard) which
				// has to be refactored.
				try {
					proj.create(null);
					proj.open(null);

					// we need an identifier (=qualified name) to store settings
					QualifiedName qualifiedName = MercurialTeamProvider.QUALIFIED_NAME_PROJECT_SOURCE_REPOSITORY;

					// save project's original clone repository as a persistent
					// setting
					proj
							.setPersistentProperty(qualifiedName, location
									.getUrl());
				} catch (CoreException e) {
					MercurialEclipsePlugin.logError(e);
				}

				try {
					// Register the project with Team. This will bring all
					// the files that
					// we cloned into the project.
					RepositoryProvider.map(proj, MercurialTeamProvider.class
							.getName());
					RepositoryProvider.getProvider(proj,
							MercurialTeamProvider.class.getName());
					projects.add(proj);
					MercurialEclipsePlugin.getRepoManager().addRepoLocation(proj, location);
				} catch (TeamException e) {
					MercurialEclipsePlugin.logError(e);
				}				
				// increase monitor so we see at least a bit of a progress when
				// importing multiple projects
				monitor.worked(1);
			}
			projectsCreated = projects.toArray(new IProject[projects.size()]);
		} finally {
			monitor.done();
		}
	}

	public String[] getReferenceStrings() {
		return referenceStrings;
	}

	public void setReferenceStrings(String[] referenceStrings) {
		this.referenceStrings = referenceStrings;
	}

	public IProject[] getProjectsCreated() {
		return projectsCreated;
	}

	public void setProjectsCreated(IProject[] projectsCreated) {
		this.projectsCreated = projectsCreated;
	}
}
