/*******************************************************************************
 * Copyright (c) 2006-2008 Bastian Doetsch and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch - initial implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.wizard.WizardDialog;

import com.vectrace.MercurialEclipse.wizards.RebaseWizard;

public class RebaseHandler extends SingleResourceHandler {

    @Override
    protected void run(IResource resource) throws Exception {        
        RebaseWizard wizard = new RebaseWizard(resource);
        WizardDialog wizardDialog = new WizardDialog(getShell(), wizard);
        wizardDialog.open();        
    }

}
