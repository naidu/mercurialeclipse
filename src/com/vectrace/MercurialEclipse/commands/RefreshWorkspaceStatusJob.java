/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Andrei	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeWorkspaceJob;
import com.vectrace.MercurialEclipse.team.ResourceProperties;

public final class RefreshWorkspaceStatusJob extends SafeWorkspaceJob {
    private final IProject project;

    public RefreshWorkspaceStatusJob(IProject project) {
        super("Refreshing project status...");
        this.project = project;
    }

    @Override
    protected IStatus runSafe(IProgressMonitor monitor) {
        try {
            final String branch = HgBranchClient.getActiveBranch(project.getLocation().toFile());
            // update branch name
            project.setSessionProperty(ResourceProperties.HG_BRANCH, branch);

            // reset merge properties
            project.setPersistentProperty(ResourceProperties.MERGING, null);
            project.setSessionProperty(ResourceProperties.MERGE_COMMIT_OFFERED, null);

            // refresh resources
            project.refreshLocal(IResource.DEPTH_INFINITE, null);

            return super.runSafe(monitor);
        } catch (CoreException e) {
            MercurialEclipsePlugin.logError(e);
            return new Status(IStatus.ERROR, MercurialEclipsePlugin.ID,
                    e.getLocalizedMessage(), e);
        }
    }
}