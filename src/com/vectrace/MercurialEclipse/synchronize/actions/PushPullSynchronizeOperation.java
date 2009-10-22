/*******************************************************************************
 * Copyright (c) 2008 MercurialEclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch				- implementation
 ******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeUiJob;
import com.vectrace.MercurialEclipse.commands.HgPushPullClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.synchronize.MercurialSynchronizeParticipant;
import com.vectrace.MercurialEclipse.synchronize.Messages;
import com.vectrace.MercurialEclipse.team.cache.RefreshJob;

public class PushPullSynchronizeOperation extends SynchronizeModelOperation {
    private final org.eclipse.core.resources.IProject project;
    private final ISynchronizePageConfiguration configuration;
    private final MercurialSynchronizeParticipant participant;
    private final boolean update;
    private final boolean isPull;
    private final String revision;

    public PushPullSynchronizeOperation(
            ISynchronizePageConfiguration configuration,
            IDiffElement[] elements, IResource[] resources, String revision, boolean isPull, boolean update) {
        super(configuration, elements);
        this.project = resources[0].getProject();
        this.configuration = configuration;
        this.participant = (MercurialSynchronizeParticipant) this.configuration
                .getParticipant();
        this.revision = revision;
        this.isPull = isPull;
        this.update = update;
    }

    public PushPullSynchronizeOperation(
            ISynchronizePageConfiguration configuration,
            IDiffElement[] elements, IResource[] resources, boolean isPull, boolean update) {
        this(configuration, elements, resources, null, isPull, update);
    }

    public void run(IProgressMonitor monitor) throws InvocationTargetException,
            InterruptedException {
        monitor.beginTask(isPull ? Messages.getString("PushPullSynchronizeOperation.PullTask")
                : Messages.getString("PushPullSynchronizeOperation.PushTask")
                + project, 1);
        new SafeUiJob(isPull ? Messages.getString("PushPullSynchronizeOperation.PullJob")
                : Messages.getString("PushPullSynchronizeOperation.PushJob")) {

            @Override
            protected IStatus runSafe(IProgressMonitor moni) {
                try {
                    HgRepositoryLocation location = participant
                            .getRepositoryLocation();

                    if (location != null) {
                        if (isPull) {
                            HgPushPullClient.pull(project, location, update);
                        } else {
                            HgPushPullClient.push(project, location, false, revision, Integer.MAX_VALUE);
                            new RefreshJob("Refreshing " + project.getName(), project, RefreshJob.OUTGOING).schedule();
                        }
                    }
                } catch (HgException ex) {
                    MercurialEclipsePlugin.logError(ex);
                    return ex.getStatus();
                }
                return super.runSafe(moni);
            }

        }.schedule();
        monitor.done();
    }

    /**
     * @return the isPull
     */
    public boolean isPull() {
        return isPull;
    }

    /**
     * @return the revision
     */
    public String getRevision() {
        return revision;
    }

}
