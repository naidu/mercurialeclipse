package com.vectrace.MercurialEclipse.menu;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Shell;

import com.vectrace.MercurialEclipse.commands.HgCommitClient;
import com.vectrace.MercurialEclipse.dialogs.CommitDialog;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.team.ResourceProperties;

public class CommitMergeHandler extends SingleResourceHandler {

    /**
     * run the commit merge handler
     */
    @Override
    protected void run(IResource resource) throws HgException {
        Assert.isNotNull(resource);
        commitMergeWithCommitDialog(resource.getProject(), getShell());
    }

    /**
     * Opens the Commit dialog and commits the merge if ok is pressed.
     * @param resource
     *            the resource
     * @return the hg command output
     * @throws HgException
     */
    public String commitMergeWithCommitDialog(IProject resource, Shell shell) throws HgException {
        Assert.isNotNull(resource);
        String result = ""; //$NON-NLS-1$
        try {
            CommitDialog commitDialog = new CommitDialog(shell,  resource,
                    Messages.getString("CommitMergeHandler.mergeWith") //$NON-NLS-1$
                            + resource.getProject().getPersistentProperty(ResourceProperties.MERGING));

            // open dialog and wait for ok
            commitDialog.open();
        } catch (CoreException e) {
            throw new HgException(Messages.getString("CommitMergeHandler.failedToSetMergeStatus"), e); //$NON-NLS-1$
        }
        return result;
    }

    /**
     * Commits a merge with a default merge message. The commit dialog is not shown.
     * @param project a project to be committed.
     * @return the output of hg commit
     * @throws HgException
     * @throws CoreException
     */
    public static String commitMerge(IProject project) throws HgException,
            CoreException {
        Assert.isNotNull(project);
        return commitMerge(project, Messages.getString("CommitMergeHandler.mergeWith") //$NON-NLS-1$
                + project.getPersistentProperty(ResourceProperties.MERGING));
    }

    /**
     * Commits a merge with the given message. The commit dialog is not shown.
     * @param project a project to be committed, not null
     * @return the output of hg commit
     * @throws HgException
     * @throws CoreException
     */
    public static String commitMerge(IProject project, String message)
            throws HgException, CoreException {
        Assert.isNotNull(project);
        Assert.isNotNull(message);

        // do hg call
        String result = HgCommitClient.commitProject(project, null, message);

        // clear merge status in Eclipse
        project.setPersistentProperty(ResourceProperties.MERGING, null);
        project.setSessionProperty(ResourceProperties.MERGE_COMMIT_OFFERED, null);

        project.touch(null);
        return result;
    }

}
