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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.wizard.WizardDialog;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeWorkspaceJob;
import com.vectrace.MercurialEclipse.wizards.PullRepoWizard;

public class PullHandler extends SingleResourceHandler {

    @Override
    protected void run(final IResource resource) throws Exception {
        PullRepoWizard pullRepoWizard = new PullRepoWizard(resource.getProject());
        WizardDialog pullWizardDialog = new WizardDialog(getShell(),pullRepoWizard);
        pullWizardDialog.open();
        new SafeWorkspaceJob("Refreshing local resources.") {
            /*
             * (non-Javadoc)
             *
             * @see com.vectrace.MercurialEclipse.SafeWorkspaceJob#runSafe(org.eclipse.core.runtime.IProgressMonitor)
             */
            @Override
            protected IStatus runSafe(IProgressMonitor monitor) {
                try {
                    resource.getProject().refreshLocal(IResource.DEPTH_INFINITE, monitor);
                } catch (CoreException e) {
                    MercurialEclipsePlugin.logError(e);
                    return new Status(IStatus.ERROR, MercurialEclipsePlugin.ID, e.getLocalizedMessage(), e);
                }
                return super.runSafe(monitor);
            }
        }.schedule();

    }

}
