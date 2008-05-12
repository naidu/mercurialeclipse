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
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.commands.HgIMergeClient;
import com.vectrace.MercurialEclipse.dialogs.RevisionChooserDialog;
import com.vectrace.MercurialEclipse.team.ResourceProperties;
import com.vectrace.MercurialEclipse.team.cache.RefreshJob;
import com.vectrace.MercurialEclipse.views.MergeView;

public class MergeHandler extends SingleResourceHandler {

    @Override
    protected void run(IResource resource) throws Exception {
        IProject project = resource.getProject();
        RevisionChooserDialog dialog = new RevisionChooserDialog(getShell(), "Merge With...",
                project);
        if (dialog.open() == IDialogConstants.OK_ID) {
            HgIMergeClient.merge(project, dialog.getRevision());
            project.setPersistentProperty(ResourceProperties.MERGING, "true");
            // will trigger a FlagManager refresh
            MergeView view = (MergeView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(MergeView.ID);
            view.clearView();
            view.setCurrentProject(project);
            project.refreshLocal(IResource.DEPTH_INFINITE, null);
            new RefreshJob(
                    "Refreshing project status and changesets after merge.",
                    null, resource.getProject()).schedule();
        }
    }

}
