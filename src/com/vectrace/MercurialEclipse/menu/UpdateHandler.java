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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeWorkspaceJob;
import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.commands.HgUpdateClient;
import com.vectrace.MercurialEclipse.team.ResourceProperties;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;

public class UpdateHandler extends SingleResourceHandler {

    @Override
    public void run(IResource resource) throws Exception {
        final IProject project = resource.getProject();
        boolean dirty = HgStatusClient.isDirty(project);
        if (dirty) {
            boolean result = MessageDialog.openQuestion(getShell(), "Uncommited Changes", 
                    "Your project has uncommited changes.\nDo you really want to update?");
            if (!result)
                return;
        }
        HgUpdateClient.update(project, null, false);
        // reset merge properties
        project.setPersistentProperty(ResourceProperties.MERGING, null);
        project.setSessionProperty(ResourceProperties.MERGE_COMMIT_OFFERED, null);
        new SafeWorkspaceJob("Refreshing project files...") {
            /*
             * (non-Javadoc)
             * 
             * @see
             * com.vectrace.MercurialEclipse.SafeWorkspaceJob#runSafe(org.eclipse
             * .core.runtime.IProgressMonitor)
             */
            @Override
            protected IStatus runSafe(IProgressMonitor monitor) {
                try {
                    project.refreshLocal(IResource.DEPTH_INFINITE, null);
                    MercurialStatusCache.getInstance().notifyChanged(project);
                    return super.runSafe(monitor);
                } catch (CoreException e) {
                    MercurialEclipsePlugin.logError(e);
                    return new Status(IStatus.ERROR, MercurialEclipsePlugin.ID,
                            e.getLocalizedMessage(), e); 
                }
            }
        }.schedule();
        
    }

}
