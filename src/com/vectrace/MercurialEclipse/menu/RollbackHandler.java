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

import com.vectrace.MercurialEclipse.commands.HgBranchClient;
import com.vectrace.MercurialEclipse.commands.HgRollbackClient;
import com.vectrace.MercurialEclipse.team.ResourceProperties;
import com.vectrace.MercurialEclipse.team.cache.RefreshJob;

public class RollbackHandler extends SingleResourceHandler {

    @Override
    protected void run(IResource resource) throws Exception {
        IProject project = resource.getProject();
        String result = HgRollbackClient.rollback(project);
        MessageDialog.openInformation(getShell(),Messages.getString("RollbackHandler.output"), result); //$NON-NLS-1$
        String branch = HgBranchClient.getActiveBranch(project.getLocation().toFile());
        project.setSessionProperty(ResourceProperties.HG_BRANCH, branch);
        project.refreshLocal(IResource.DEPTH_INFINITE, null);
        new RefreshJob(Messages.getString("RollbackHandler.refresh"),null,project).schedule(); //$NON-NLS-1$
    }

}
