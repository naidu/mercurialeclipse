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

import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

import com.vectrace.MercurialEclipse.wizards.ExportWizard;

public class ExportPatchHandler extends MultipleResourcesHandler {

    @Override
    protected void run(List<IResource> resources) throws Exception {
        openWizard(resources, getShell());
    }

    public static void openWizard(List<IResource> resources, Shell shell) {
        ExportWizard wizard = new ExportWizard(resources);
        WizardDialog dialog = new WizardDialog(shell, wizard);
        dialog.setBlockOnOpen(true);
        dialog.open();    
    }

}
