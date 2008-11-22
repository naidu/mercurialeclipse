/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;

import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.commands.HgUpdateClient;
import com.vectrace.MercurialEclipse.dialogs.RevisionChooserDialog;
import com.vectrace.MercurialEclipse.team.ResourceProperties;
import com.vectrace.MercurialEclipse.views.MergeView;

public class SwitchHandler extends SingleResourceHandler {

    @Override
    protected void run(IResource resource) throws Exception {
        IProject project = resource.getProject();
        // better safe than sorry => do not trust the FlagManager
        if (HgStatusClient.isDirty(project)) {
            if (!MessageDialog
                    .openQuestion(getShell(),
                            Messages.getString("SwitchHandler.pendingChangesConfirmation.1"), //$NON-NLS-1$
                            Messages.getString("SwitchHandler.pendingChangesConfirmation.2"))) { //$NON-NLS-1$
                return;
            }
        }
        RevisionChooserDialog dialog = new RevisionChooserDialog(getShell(),
                Messages.getString("SwitchHandler.switchTo"), project); //$NON-NLS-1$
        int result = dialog.open();
        if (result == IDialogConstants.OK_ID) {
            HgUpdateClient.update(project, dialog.getRevision(), true);
            project.setPersistentProperty(ResourceProperties.MERGING, null);
            project.setSessionProperty(ResourceProperties.MERGE_COMMIT_OFFERED, null);
            project.refreshLocal(IResource.DEPTH_INFINITE, null);
            MergeView view = MergeView.getView();
            if (view != null) {
                view.clearView();
            }
            // will trigger a FlagManager refresh
        }
    }

}
