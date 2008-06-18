package com.vectrace.MercurialEclipse.menu;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.window.Window;

import com.vectrace.MercurialEclipse.commands.HgCommitClient;
import com.vectrace.MercurialEclipse.dialogs.CommitDialog;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.team.ResourceProperties;
import com.vectrace.MercurialEclipse.team.cache.RefreshJob;

public class CommitMergeHandler extends SingleResourceHandler {

    @Override
    protected void run(IResource resource) throws HgException {
        // FIXME let's pray that all resources are in the same project...
        final IProject project = resource.getProject();

        IResource[] selectedResourceArray = new IResource[1];
        selectedResourceArray[0] = resource;

        try {
        CommitDialog commitDialog = new CommitDialog(getShell(), project,
                selectedResourceArray,"Merge with "
                + project.getPersistentProperty(ResourceProperties.MERGING),false);

        if (commitDialog.open() == Window.OK) {
            
            // commit
            String messageToCommit = commitDialog.getCommitMessage();

            commitMerge(resource, messageToCommit);

            new RefreshJob("Refreshing local changesets after commit...", null, project).schedule();           
        }
        }
        catch(CoreException e) {
            throw new HgException(e);
        }
     }

//    @Override
//    protected void run(IResource resource) throws Exception {
//        commitMerge(resource);
//    }

    public static String commitMerge(IResource resource) throws HgException, CoreException {
        final IProject project = resource.getProject();
        return commitMerge(resource,"Merge with "
                + project.getPersistentProperty(ResourceProperties.MERGING));
    }
    
    /**
     * @param resource
     * @throws HgException
     * @throws CoreException
     */
    public static String commitMerge(IResource resource, String message) throws HgException,
            CoreException {
        IProject project = resource.getProject();
        String result = HgCommitClient.commitProject(project, null, message);
        project.setPersistentProperty(ResourceProperties.MERGING, null);
        project.refreshLocal(IResource.DEPTH_INFINITE, null);
        new RefreshJob("Refresh status and changesets after merge", null,
                project).schedule();
        return result;
    }

}
