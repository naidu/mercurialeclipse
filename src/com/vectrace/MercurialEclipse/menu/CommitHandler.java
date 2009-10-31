package com.vectrace.MercurialEclipse.menu;

import java.util.List;

import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.dialogs.CommitDialog;
import com.vectrace.MercurialEclipse.exception.HgException;

public class CommitHandler extends MultipleResourcesHandler {

    @Override
    public void run(final List<IResource> resources) throws HgException {
        ensureSameRoot(resources);
        CommitDialog commitDialog = new CommitDialog(getShell(), resources);
        commitDialog.open();
    }

}
