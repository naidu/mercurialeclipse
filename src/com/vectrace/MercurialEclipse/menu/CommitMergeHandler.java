package com.vectrace.MercurialEclipse.menu;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;

import com.vectrace.MercurialEclipse.commands.HgCommitClient;
import com.vectrace.MercurialEclipse.dialogs.CommitDialog;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.team.ResourceProperties;
import com.vectrace.MercurialEclipse.team.cache.RefreshJob;

public class CommitMergeHandler extends SingleResourceHandler {
    
    /**
     * run the commit merge handler
     */
    @Override
    protected void run(IResource resource) throws HgException {
        Assert.isNotNull(resource);
        commitMergeWithCommitDialog(resource, getShell());
    }

    /**
     * Opens the Commit dialog and commits the merge if ok is pressed.
     * @param resource
     *            the resource
     * @return the hg command output
     * @throws HgException
     */
    public String commitMergeWithCommitDialog(IResource resource, Shell shell) throws HgException {
        Assert.isNotNull(resource);
        String result = "";
        try {
            // FIXME let's pray that all resources are in the same project...
            IProject project = resource.getProject();
            Assert.isNotNull(project);

            IResource[] selectedResourceArray = new IResource[1];
            selectedResourceArray[0] = resource;

            CommitDialog commitDialog = new CommitDialog(
                    shell,
                    project,
                    selectedResourceArray,
                    "Merge with "
                            + project
                                    .getPersistentProperty(ResourceProperties.MERGING),
                    false);

            // open dialog and wait for ok
            if (commitDialog.open() == Window.OK) {
                // commit
                String messageToCommit = commitDialog.getCommitMessage();
                result = commitMerge(resource, messageToCommit);
            }
        } catch (CoreException e) {
            throw new HgException("Failed to set merge status", e);
        }
        return result;
    }

    /**
     * Commits a merge with a default merge message. The commit dialog is not shown.
     * @param resource a resource in the project to be committed.
     * @return the output of hg commit
     * @throws HgException
     * @throws CoreException
     */
    public static String commitMerge(IResource resource) throws HgException,
            CoreException {
        Assert.isNotNull(resource);
        final IProject project = resource.getProject();
        Assert.isNotNull(project);
        return commitMerge(resource, "Merge with "
                + project.getPersistentProperty(ResourceProperties.MERGING));
    }

    /**
     * Commits a merge with the given message. The commit dialog is not shown.
     * @param resource a resource in the project to be committed
     * @return the output of hg commit
     * @throws HgException
     * @throws CoreException
     */
    public static String commitMerge(IResource resource, String message)
            throws HgException, CoreException {
        Assert.isNotNull(resource);
        Assert.isNotNull(message);
        IProject project = resource.getProject();
        Assert.isNotNull(resource.getProject());
        
        // do hg call
        String result = HgCommitClient.commitProject(project, null, message);
        
        // clear merge status in Eclipse
        project.setPersistentProperty(ResourceProperties.MERGING, null);
        project.setSessionProperty(ResourceProperties.MERGE_COMMIT_OFFERED, null);
        
        // refresh caches
        new RefreshJob("Refresh status and changesets after merge commit...", null,
                project).schedule();
        project.touch(null);
        return result;
    }

}
