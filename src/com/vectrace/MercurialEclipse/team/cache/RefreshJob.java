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

import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;

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
    private final IProject project;
    private final boolean withFiles;

    public RefreshJob(String name, IProject project, boolean withFiles) {
        super(name);
        this.project = project;
        this.withFiles = withFiles;
    }


    public RefreshJob(String name, IProject project) {
        this(name, project, getWithFilesProperty());
    }

    private static boolean getWithFilesProperty() {
        return Boolean.valueOf(
                HgClients.getPreference(
                        MercurialPreferenceConstants.RESOURCE_DECORATOR_SHOW_CHANGESET,
                        "false")).booleanValue();
    }

    @Override
    protected IStatus runSafe(IProgressMonitor monitor) {
        if(monitor == null){
            monitor = new NullProgressMonitor();
        }

        if(MercurialEclipsePlugin.getDefault().isDebugging()) {
            System.out.println("Refresh Job for: " + project);
        }

        try {
            monitor.subTask(Messages.refreshJob_UpdatingStatusAndVersionCache);
            mercurialStatusCache.clear(project, false);
            mercurialStatusCache.refreshStatus(project, monitor);
            monitor.worked(1);

            monitor.subTask(Messages.refreshJob_LoadingLocalRevisions);
            LocalChangesetCache.getInstance().refreshAllLocalRevisions(project, true, withFiles);
            monitor.worked(1);

            Set<HgRepositoryLocation> repoLocations = MercurialEclipsePlugin.getRepoManager().getAllProjectRepoLocations(project);
            for (HgRepositoryLocation repositoryLocation : repoLocations) {
                monitor.subTask(Messages.refreshJob_LoadingIncomingRevisions + repositoryLocation);
                IncomingChangesetCache.getInstance().clear(repositoryLocation, project, true);
                monitor.worked(1);

                monitor.subTask(Messages.refreshJob_LoadingOutgoingRevisionsFor + repositoryLocation);
                OutgoingChangesetCache.getInstance().clear(repositoryLocation, project, true);
                monitor.worked(1);
            }
        } catch (HgException e) {
            MercurialEclipsePlugin.logError(e);
        }
        return super.runSafe(monitor);
    }
}