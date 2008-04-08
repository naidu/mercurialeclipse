/**
 * /*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch - Implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;

import java.net.URI;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.ProjectSetCapability;
import org.eclipse.team.core.ProjectSetSerializationContext;
import org.eclipse.team.core.TeamException;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.actions.AddToWorkspaceAction;

/**
 * Defines ProjectSetCapabilities for MercurialEclipse
 * 
 * @author Bastian Doetsch
 */
public class MercurialProjectSetCapability extends ProjectSetCapability {
	private static MercurialProjectSetCapability instance;

	@Override
	public String[] asReference(IProject[] providerProjects,
			ProjectSetSerializationContext context, IProgressMonitor monitor)
			throws TeamException {

		try {
			String[] references = new String[providerProjects.length];

			monitor.beginTask("Determining project references...",
					providerProjects.length);

			for (int i = 0; i < providerProjects.length; i++) {
				String reference = asReference(null, providerProjects[i]
						.getName());
				if (!(monitor.isCanceled() || reference == null)) {
					references[i] = reference;
				} else {
					String msg;
					if (monitor.isCanceled()) {
						msg = "Project reference determination cancelled.";
					} else {
						msg = "Project reference not determinable for "
								+ providerProjects[i];
					}
					throw new TeamException(msg);
				}
			}
			return references;
		} finally {
			monitor.done();
		}
	}

	@Override
	public IProject[] addToWorkspace(String[] referenceStrings,
			ProjectSetSerializationContext context, IProgressMonitor monitor)
			throws TeamException {
		
		// use AddToWorkspaceAction to decouple adding from other workspace
		// tasks.
		AddToWorkspaceAction action = new AddToWorkspaceAction();
		
		// our beloved projects from the import file
		action.setReferenceStrings(referenceStrings);
		try {
			action.run(monitor);
		} catch (Exception e) {
			MercurialEclipsePlugin.logError(e);
		}
		return action.getProjectsCreated();
	}

	@Override
	public String asReference(URI uri, String projectName) {
		String reference = null;
		try {
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			IProject project = workspace.getRoot().getProject(projectName);

			String srcRepository = project
					.getPersistentProperty(MercurialTeamProvider.QUALIFIED_NAME_PROJECT_SOURCE_REPOSITORY);

			if (srcRepository != null && srcRepository.length() > 0) {
				reference = "MercurialEclipseProjectSet_" + project.getName()
						+ "_" + srcRepository;
			} 
		} catch (CoreException e) {
			// reference is null -> error condition
		}
		return reference;
	}

	@Override
	public String getProject(String referenceString) {
		return referenceString.split("_")[1];
	}

	/**
	 * Singleton accessor method.
	 * @return
	 */
	public static ProjectSetCapability getInstance() {
		if (instance == null) {
			instance = new MercurialProjectSetCapability();
		}
		return instance;
	}

}
