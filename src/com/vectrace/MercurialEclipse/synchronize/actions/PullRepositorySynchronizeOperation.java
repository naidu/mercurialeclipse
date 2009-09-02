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

public class PullRepositorySynchronizeOperation extends SynchronizeModelOperation {
    private final org.eclipse.core.resources.IProject project;
    private final ISynchronizePageConfiguration configuration;
    private final MercurialSynchronizeParticipant participant;
    private final boolean update;

    public PullRepositorySynchronizeOperation(
            ISynchronizePageConfiguration configuration,
            IDiffElement[] elements, IResource[] resources, boolean update) {
        super(configuration, elements);
        this.project = resources[0].getProject();
        this.configuration = configuration;
        this.participant = (MercurialSynchronizeParticipant) this.configuration
                .getParticipant();
        this.update = update;
    }

    public void run(IProgressMonitor monitor) throws InvocationTargetException,
            InterruptedException {
        monitor.beginTask("Starting pull from "+project+". Update: "+update, 1);
        new SafeUiJob("Pulling...") {

            @Override
            protected IStatus runSafe(IProgressMonitor moni) {
                try {
                    HgRepositoryLocation loc = participant
                            .getRepositoryLocation();

                    if (loc != null) {
                        HgPushPullClient.pull(project, loc, update);
                    }

                } catch (HgException e) {
                    MercurialEclipsePlugin.logError(e);
                    return e.getStatus();
                }
                return super.runSafe(moni);
            }

        }.schedule();
        monitor.done();
    }

}
