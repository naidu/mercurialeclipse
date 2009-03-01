package com.vectrace.MercurialEclipse.menu;

import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.widgets.Shell;

import com.vectrace.MercurialEclipse.SafeUiJob;
import com.vectrace.MercurialEclipse.commands.HgCommitClient;
import com.vectrace.MercurialEclipse.dialogs.CommitDialog;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.team.ResourceProperties;
import com.vectrace.MercurialEclipse.team.cache.RefreshJob;
import com.vectrace.MercurialEclipse.views.MergeView;

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
        String result = ""; //$NON-NLS-1$
        try {
            // FIXME let's pray that all resources are in the same project...

            ArrayList<IResource> selectedResource = new ArrayList<IResource>(1);
            selectedResource.add(resource);
            HgRoot root = new HgRoot(MercurialUtilities
                    .search4MercurialRoot(resource.getLocation().toFile()));

            CommitDialog commitDialog = new CommitDialog(
                    shell,
                    root,
                    selectedResource,
                    Messages.getString("CommitMergeHandler.mergeWith") //$NON-NLS-1$
                            + resource.getProject()
                                    .getPersistentProperty(ResourceProperties.MERGING),
                    false);

            // open dialog and wait for ok
            commitDialog.open();
        } catch (CoreException e) {
            throw new HgException(Messages.getString("CommitMergeHandler.failedToSetMergeStatus"), e); //$NON-NLS-1$
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
        return commitMerge(resource, Messages.getString("CommitMergeHandler.mergeWith") //$NON-NLS-1$
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
        new RefreshJob(Messages.getString("CommitMergeHandler.refreshStatusAndChangesetsAfterMergeCommit"), null, //$NON-NLS-1$
                project).schedule();
        project.touch(null);
        new SafeUiJob(Messages.getString("CommitMergeHandler.clearingMergeView")) { //$NON-NLS-1$
            /*
             * (non-Javadoc)
             * 
             * @see
             * com.vectrace.MercurialEclipse.SafeUiJob#runSafe(org.eclipse.core
             * .runtime.IProgressMonitor)
             */
            @Override
            protected IStatus runSafe(IProgressMonitor monitor) {
                MergeView.getView().clearView();
                return super.runSafe(monitor);
            }
        }.schedule();
        
        return result;
    }

}
