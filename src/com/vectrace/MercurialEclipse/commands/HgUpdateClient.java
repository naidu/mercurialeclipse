package com.vectrace.MercurialEclipse.commands;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.cache.RefreshJob;

public class HgUpdateClient {

    public static void update(final IProject project, String revision, boolean clean)
            throws HgException {
        AbstractShellCommand command = new HgCommand("update", project, false); //$NON-NLS-1$
        command.setUsePreferenceTimeout(MercurialPreferenceConstants.UPDATE_TIMEOUT);
        if (revision != null) {
            command.addOptions("-r", revision); //$NON-NLS-1$
        }
        if (clean) {
            command.addOptions("-C"); //$NON-NLS-1$
        }
        command.executeToBytes();

        RefreshWorkspaceStatusJob job = new RefreshWorkspaceStatusJob(project);
        job.addJobChangeListener(new JobChangeAdapter(){
           @Override
            public void done(IJobChangeEvent event) {
                new RefreshJob("Refreshing " + project.getName(), project, RefreshJob.LOCAL).schedule();
            }
        });
        job.schedule();
    }
}
