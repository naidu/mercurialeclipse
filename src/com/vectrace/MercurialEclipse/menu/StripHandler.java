/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;

import com.vectrace.MercurialEclipse.commands.RefreshWorkspaceStatusJob;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.cache.RefreshJob;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;
import com.vectrace.MercurialEclipse.wizards.StripWizard;

public class StripHandler extends SingleResourceHandler {

    @Override
    protected void run(IResource resource) throws Exception {
        IProject project = resource.getProject();
        StripWizard stripWizard = new StripWizard(project);
        WizardDialog dialog = new WizardDialog(getShell(), stripWizard);
        dialog.setBlockOnOpen(true);
        if (Window.OK == dialog.open()){
            Set<IProject> projects = ResourceUtils.getProjects(MercurialTeamProvider.getHgRoot(project));
            for (final IProject iProject : projects) {
                RefreshWorkspaceStatusJob job = new RefreshWorkspaceStatusJob(iProject);
                job.addJobChangeListener(new JobChangeAdapter(){
                    @Override
                    public void done(IJobChangeEvent event) {
                        new RefreshJob("Refreshing " + iProject.getName(), iProject, RefreshJob.LOCAL).schedule();
                    }
                });
                job.schedule();
            }
        }
    }

}
