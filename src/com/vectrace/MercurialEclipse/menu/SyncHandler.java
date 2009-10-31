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
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.wizards.MercurialParticipantSynchronizeWizard;

public class SyncHandler extends SingleResourceHandler {

    @Override
    protected void run(IResource resource) throws Exception {
        MercurialParticipantSynchronizeWizard wizard = new MercurialParticipantSynchronizeWizard();
        wizard.init(PlatformUI.getWorkbench(), new StructuredSelection(resource));
        if(wizard.isComplete()){
            wizard.performFinish();
        } else {
            WizardDialog wizardDialog = new WizardDialog(getShell(), wizard);
            wizardDialog.open();
        }
    }

}
