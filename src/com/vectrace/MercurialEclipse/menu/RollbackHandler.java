/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.MessageDialog;

import com.vectrace.MercurialEclipse.commands.HgRollbackClient;
import com.vectrace.MercurialEclipse.team.cache.RefreshJob;

public class RollbackHandler extends SingleResourceHandler {

    @Override
    protected void run(IResource resource) throws Exception {
        IProject project = resource.getProject();
        String result = HgRollbackClient.rollback(project);
        MessageDialog.openInformation(getShell(),"Rollback output", result);
        project.refreshLocal(IResource.DEPTH_INFINITE, null);
        new RefreshJob("Refreshing status and changesets after rollback...",null,project).schedule();
    }

}
