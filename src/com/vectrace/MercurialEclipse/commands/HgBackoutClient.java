/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.cache.RefreshJob;

/**
 * @author bastian
 *
 */
public class HgBackoutClient {

    /**
     * Backout of a changeset
     *
     * @param project
     *            the project
     * @param backoutRevision
     *            revision to backout
     * @param merge
     *            flag if merge with a parent is wanted
     * @param msg
     *            commit message
     */
    public static String backout(final IProject project, ChangeSet backoutRevision,
            boolean merge, String msg, String user) throws CoreException {

        AbstractShellCommand command = new HgCommand("backout", project, true); //$NON-NLS-1$
        boolean useExternalMergeTool = Boolean.valueOf(
                HgClients.getPreference(MercurialPreferenceConstants.PREF_USE_EXTERNAL_MERGE,
                        "false")).booleanValue(); //$NON-NLS-1$

        if (!useExternalMergeTool) {
            command.addOptions("--config", "ui.merge=simplemerge"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        command.addOptions("-r", backoutRevision.getChangeset(), "-m", msg, "-u", user);//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        if (merge) {
            command.addOptions("--merge"); //$NON-NLS-1$
        }

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
