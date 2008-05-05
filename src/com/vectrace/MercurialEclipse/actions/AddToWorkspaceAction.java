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
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.team.core.RepositoryProvider;
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
             * A reference string uses underscore as delimiter and looks like
             * this:
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
                try {
                    // Repository-URL is stored in part 2
                    HgRepositoryLocation location = new HgRepositoryLocation(
                            referenceParts[2]);

                    HgCloneClient.clone(workspace, location, null, proj
                            .getName());

                    proj.create(null);
                    proj.open(null);

                    // Register the project with Team.
                    RepositoryProvider.map(proj, MercurialTeamProvider.class
                            .getName());
                    RepositoryProvider.getProvider(proj,
                            MercurialTeamProvider.class.getName());
                    projects.add(proj);

                    // store repo as default repo
                    MercurialEclipsePlugin.getRepoManager()
                            .setDefaultProjectRepository(proj, location);
                    MercurialEclipsePlugin.getRepoManager().addRepoLocation(
                            proj, location);

                    // refresh project to get decorations
                    proj.refreshLocal(IResource.DEPTH_INFINITE, monitor);

                } catch (Exception e) {
                    MercurialEclipsePlugin.logError(e);
                    CoreException ex = new CoreException(new Status(
                            IStatus.ERROR, MercurialEclipsePlugin.ID, e
                                    .getLocalizedMessage()));
                    ex.initCause(e);
                    throw ex;
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
