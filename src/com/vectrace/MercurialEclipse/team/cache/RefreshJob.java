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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeWorkspaceJob;
import com.vectrace.MercurialEclipse.commands.HgClients;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

/**
 * Refreshes status, local changesets, incoming changesets and outgoing
 * changesets. If you only want to refresh the status use
 * {@link RefreshStatusJob}.
 * 
 * For big repositories this can be quite slow when "withFiles" is set to true
 * in constructor.
 * 
 * @author Bastian Doetsch
 * 
 */
public final class RefreshJob extends SafeWorkspaceJob {
    private final static MercurialStatusCache mercurialStatusCache = MercurialStatusCache
            .getInstance();
    private final HgRepositoryLocation repositoryLocation;
    private final IProject project;
    private boolean withFiles = false;

    public RefreshJob(String name, HgRepositoryLocation repositoryLocation,
            IProject project, boolean withFiles) {
        super(name);
        this.repositoryLocation = repositoryLocation;
        this.project = project;
        this.withFiles = withFiles;
    }

    public RefreshJob(String name, HgRepositoryLocation repositoryLocation,
            IProject project) {
        super(name);
        this.repositoryLocation = repositoryLocation;
        this.project = project;
        this.withFiles = Boolean
                .valueOf(
                        HgClients
                                .getPreference(
                                        MercurialPreferenceConstants.RESOURCE_DECORATOR_SHOW_CHANGESET,
                                        "false")).booleanValue();
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
            LocalChangesetCache.getInstance().refreshAllLocalRevisions(project,
                    false, withFiles);
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

                MercurialEclipsePlugin.getRepoManager().addRepoLocation(
                        project, repositoryLocation);
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