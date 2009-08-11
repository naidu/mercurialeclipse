package com.vectrace.MercurialEclipse.commands;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeWorkspaceJob;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.ResourceProperties;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;

public class HgUpdateClient {

    public static void update(final IProject project, String revision, boolean clean)
            throws HgException {
        AbstractShellCommand command = new HgCommand("update", project, false); //$NON-NLS-1$
        command
                .setUsePreferenceTimeout(MercurialPreferenceConstants.UPDATE_TIMEOUT);
        if (revision != null) {
            command.addOptions("-r", revision); //$NON-NLS-1$
        }
        if (clean) {
            command.addOptions("-C"); //$NON-NLS-1$
        }
        command.executeToBytes();
        final String branch = HgBranchClient.getActiveBranch(project.getLocation().toFile());

        new RefreshWorkspaceStatusJob(branch, project).schedule();
    }

    private static final class RefreshWorkspaceStatusJob extends SafeWorkspaceJob {
        private final String branch;
        private final IProject project;

        private RefreshWorkspaceStatusJob(String branch, IProject project) {
            super("Refreshing project status...");
            this.branch = branch;
            this.project = project;
        }

        @Override
        protected IStatus runSafe(IProgressMonitor monitor) {
            try {
                // update branch name
                project.setSessionProperty(ResourceProperties.HG_BRANCH, branch);

                // reset merge properties
                project.setPersistentProperty(ResourceProperties.MERGING, null);
                project.setSessionProperty(ResourceProperties.MERGE_COMMIT_OFFERED, null);

                // refresh resources
                project.refreshLocal(IResource.DEPTH_INFINITE, null);

                // refresh status for resources
                MercurialStatusCache.getInstance().refreshStatus(project, monitor);
                LocalChangesetCache.getInstance().refreshAllLocalRevisions(project, true);
                return super.runSafe(monitor);
            } catch (CoreException e) {
                MercurialEclipsePlugin.logError(e);
                return new Status(IStatus.ERROR, MercurialEclipsePlugin.ID,
                        e.getLocalizedMessage(), e);
            }
        }
    }
}
