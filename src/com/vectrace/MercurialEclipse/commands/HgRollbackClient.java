/*******************************************************************************
 * Copyright (c) 2007-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch	- implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

import com.vectrace.MercurialEclipse.team.cache.RefreshJob;

public class HgRollbackClient {

    public static String rollback(final IProject project) throws CoreException {
        AbstractShellCommand command = new HgCommand("rollback", project, true);
        String result = command.executeToString();

        RefreshWorkspaceStatusJob job = new RefreshWorkspaceStatusJob(project);
        job.addJobChangeListener(new JobChangeAdapter(){
           @Override
            public void done(IJobChangeEvent event) {
                new RefreshJob("Refreshing " + project.getName(), project).schedule();
            }
        });
        job.schedule();
        return result;
    }

}
