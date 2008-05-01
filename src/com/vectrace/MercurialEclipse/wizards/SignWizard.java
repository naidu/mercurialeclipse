/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian  implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.Wizard;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;

public class SignWizard extends Wizard {
    SignWizardPage page = null;
    IProject project = null;

    public SignWizard(IProject proj) {
        this.project = proj;
        ImageDescriptor image = MercurialEclipsePlugin
                .getImageDescriptor("wizards/share_wizban.png");
        page = new SignWizardPage("Sign changesets", "Sign changesets", image,
                "Cryptographically sign changesets", proj);
        IDialogSettings workbenchSettings = MercurialEclipsePlugin.getDefault()
                .getDialogSettings();
        IDialogSettings section = workbenchSettings.getSection("SignWizard");
        if (section == null) {
            section = workbenchSettings.addNewSection("SignWizard");
        }
        setDialogSettings(section);
    }

    @Override
    public boolean performFinish() {
        return page.finish(new NullProgressMonitor());
    }

    @Override
    public void addPages() {
        super.addPages();
        addPage(page);
    }

    @Override
    public boolean canFinish() {
        return super.canFinish() && page.canFlipToNextPage();
    }

}
