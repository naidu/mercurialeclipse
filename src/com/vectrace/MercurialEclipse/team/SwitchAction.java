package com.vectrace.MercurialEclipse.team;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;

import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.commands.HgUpdateClient;
import com.vectrace.MercurialEclipse.dialogs.RevisionChooserDialog;

public class SwitchAction extends SingleResourceAction {

    @Override
    protected void run(IResource resource) throws Exception {
        IProject project = resource.getProject();
        if(HgStatusClient.isDirty(project)) {
            if(!MessageDialog.openQuestion(
                    getShell(),
                    "Do you really want to switch changeset?",
                    "Project has some pending changes, do you want to continue and lose them?")) {
                return;
            }
        }
        RevisionChooserDialog dialog = new RevisionChooserDialog(
                getShell(),
                "Switch to...",
                project);
        int result = dialog.open();
        if (result == IDialogConstants.OK_ID) {
            HgUpdateClient.update(project, dialog.getRevision(), true);
            project.refreshLocal(IResource.DEPTH_INFINITE, null);
        }
    }

}
