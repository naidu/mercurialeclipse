package com.vectrace.MercurialEclipse.menu;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.dialogs.CommitDialog;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

public class CommitHandler extends MultipleResourcesHandler {

    @Override
    public void run(final List<IResource> resources) throws HgException {
        // FIXME let's pray that all resources are in the same project...
        IProject project = ensureSameProject();
        HgRoot root = new HgRoot(MercurialUtilities
                .search4MercurialRoot(project));
        CommitDialog commitDialog = new CommitDialog(getShell(), root,
                resources);

        commitDialog.open();
    }

}
