/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     steeven
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

import com.vectrace.MercurialEclipse.wizards.ImportPatchWizard;

public class ImportPatchHandler extends SingleResourceHandler {

    @Override
    protected void run(IResource resource) throws Exception {
        openWizard(resource, getShell());
    }

    public void openWizard(IResource resource, Shell shell) throws Exception {
        IProject project = resource.getProject();
        ImportPatchWizard wizard = new ImportPatchWizard(project);
        WizardDialog dialog = new WizardDialog(shell, wizard);
        dialog.setBlockOnOpen(true);
        if (Window.OK == dialog.open())
            project.refreshLocal(IResource.DEPTH_INFINITE, null);
    }
}
