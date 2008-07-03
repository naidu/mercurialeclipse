package com.vectrace.MercurialEclipse.menu;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.window.Window;

import com.vectrace.MercurialEclipse.commands.HgAddClient;
import com.vectrace.MercurialEclipse.commands.HgCommitClient;
import com.vectrace.MercurialEclipse.commands.HgRemoveClient;
import com.vectrace.MercurialEclipse.dialogs.CommitDialog;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.team.cache.RefreshJob;

public class CommitHandler extends MultipleResourcesHandler {

    @Override
    public void run(final List<IResource> resources) throws HgException {
        // FIXME let's pray that all resources are in the same project...
        final IProject project = resources.get(0).getProject();
        for (IResource res : resources) {
            if (!res.getProject().equals(project)) {
                throw new HgException(
                        "All resources must be in the same project. It will be fixed soon ;)");
            }
        }

        IResource[] selectedResourceArray = resources.toArray(new IResource[0]);

        CommitDialog commitDialog = new CommitDialog(getShell(), project,
                selectedResourceArray);

        if (commitDialog.open() == Window.OK) {
            // add new resources
            List<IResource> filesToAdd = commitDialog.getResourcesToAdd();
            HgAddClient.addResources(filesToAdd, null);
            
            // remove deleted resources
            List<IResource> filesToRemove = commitDialog.getResourcesToRemove();
            HgRemoveClient.removeResources(filesToRemove);

            // commit all
            IResource[] resourcesToCommit = commitDialog.getResourcesToCommit();
            String messageToCommit = commitDialog.getCommitMessage();
            
            

            HgCommitClient.commitResources(Arrays.asList(resourcesToCommit),
                    null, // user
                    messageToCommit, null); // monitor

            new RefreshJob("Refreshing local changesets after commit...", null, project).schedule();           
        }
    }

}
