/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch           - init
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team.cache;

import java.net.MalformedURLException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeWorkspaceJob;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

/**
 * @author bastian
 * 
 */
public final class RefreshJob extends SafeWorkspaceJob {
    private final static MercurialStatusCache mercurialStatusCache = MercurialStatusCache
            .getInstance();
    private final String repositoryLocation;
    private final IProject project;

    /**
     * @param name
     * @param repositoryLocation
     * @param project
     * @param mercurialStatusCache
     *            TODO
     */
    public RefreshJob(String name, String repositoryLocation, IProject project) {
        super(name);
        this.repositoryLocation = repositoryLocation;
        this.project = project;
    }

    @Override
    protected IStatus runSafe(IProgressMonitor monitor) {
        try {
            mercurialStatusCache.refreshStatus(project, monitor);
        } catch (HgException e1) {
            MercurialEclipsePlugin.logError(e1);
        }

        if (monitor != null) {
            monitor.subTask("Updating status and version cache...");
        }
        try {
            if (monitor != null) {
                monitor.subTask("Loading local revisions...");
            }
            LocalChangesetCache.getInstance().refreshAllLocalRevisions(project);
            if (monitor != null) {
                monitor.worked(1);
            }
            // incoming
            if (repositoryLocation != null) {
                if (monitor != null) {
                    monitor.subTask("Loading incoming revisions for "
                            + repositoryLocation);
                }
                IncomingChangesetCache.getInstance().refreshIncomingChangeSets(
                        project, repositoryLocation);
                if (monitor != null) {
                    monitor.worked(1);
                }

                if (monitor != null) {
                    monitor.subTask("Loading outgoing revisions for "
                            + repositoryLocation);
                }
                OutgoingChangesetCache.getInstance().refreshOutgoingChangeSets(
                        project, repositoryLocation);
                if (monitor != null) {
                    monitor.worked(1);
                }

                if (monitor != null) {
                    monitor
                            .subTask("Adding remote repository to project repositories...");
                }
                try {
                    MercurialEclipsePlugin.getRepoManager().addRepoLocation(
                            project,
                            new HgRepositoryLocation(repositoryLocation));
                } catch (MalformedURLException e) {
                    MercurialEclipsePlugin.logWarning(
                            "couldn't add repository to location manager", e);
                }
                if (monitor != null) {
                    monitor.worked(1);
                }
            }
        } catch (HgException e) {
            MercurialEclipsePlugin.logError(e);
        }
        return super.runSafe(monitor);
    }
}