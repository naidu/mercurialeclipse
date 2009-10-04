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

import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

import com.vectrace.MercurialEclipse.team.cache.RefreshJob;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class HgRollbackClient {

    public static String rollback(final IProject project) throws CoreException {
        HgCommand command = new HgCommand("rollback", project, true);
        String result = command.executeToString();

        Set<IProject> projects = ResourceUtils.getProjects(command.getHgRoot());
        for (final IProject iProject : projects) {
            RefreshWorkspaceStatusJob job = new RefreshWorkspaceStatusJob(iProject);
            job.addJobChangeListener(new JobChangeAdapter(){
               @Override
                public void done(IJobChangeEvent event) {
                    new RefreshJob("Refreshing " + iProject.getName(), iProject).schedule();
                }
            });
            job.schedule();
        }
        return result;
    }

}
