package com.vectrace.MercurialEclipse.menu;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.window.Window;

import com.vectrace.MercurialEclipse.commands.HgAddClient;
import com.vectrace.MercurialEclipse.commands.HgClients;
import com.vectrace.MercurialEclipse.commands.HgCommitClient;
import com.vectrace.MercurialEclipse.commands.HgRemoveClient;
import com.vectrace.MercurialEclipse.dialogs.CommitDialog;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.team.cache.RefreshJob;

public class CommitHandler extends MultipleResourcesHandler {

    @Override
    public void run(final List<IResource> resources) throws HgException {
        // FIXME let's pray that all resources are in the same project...
        IProject project = ensureSameProject();
        HgRoot root = new HgRoot(MercurialUtilities
                .search4MercurialRoot(project));
        CommitDialog commitDialog = new CommitDialog(getShell(), root,
                resources);

        if (commitDialog.open() == Window.OK) {
            // add new resources
            List<IResource> filesToAdd = commitDialog.getResourcesToAdd();
            HgAddClient.addResources(filesToAdd, null);

            // remove deleted resources
            List<IResource> filesToRemove = commitDialog.getResourcesToRemove();
            HgRemoveClient.removeResources(filesToRemove);

            // commit all
            List<IResource> resourcesToCommit = commitDialog.getResourcesToCommit();
            String messageToCommit = commitDialog.getCommitMessage();

            HgCommitClient.commitResources(resourcesToCommit, HgClients
                    .getDefaultUserName(), messageToCommit,
                    new NullProgressMonitor());

            new RefreshJob("Refreshing local changesets after commit...", null,
                    project).schedule();
        }
    }

}
