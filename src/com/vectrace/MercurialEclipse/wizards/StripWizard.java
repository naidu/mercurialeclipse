/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian Doetsch	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;

/**
 * @author bastian
 * 
 */
public class StripWizard extends HgWizard {
    private IProject project;

    private StripWizard() {
        super(Messages.getString("StripWizard.title"));  //$NON-NLS-1$
        setNeedsProgressMonitor(true);
    }

    public StripWizard(IResource resource) {
        this();
        this.project = resource.getProject();
    }

    @Override
    public void addPages() {
        super.addPages();
        page = createPage(
                Messages.getString("StripWizard.page.name"), //$NON-NLS-1$
                Messages.getString("StripWizard.pageTitle"), //$NON-NLS-1$
                null,
                Messages.getString("StripWizard.page.description.1") //$NON-NLS-1$
                        + Messages.getString("StripWizard.page.description.2")); //$NON-NLS-1$
        addPage(page);
    }

    /**
     * Creates a ConfigurationWizardPage.
     */
    protected HgWizardPage createPage(String pageName, String pageTitle,
            String iconPath, String description) {
        this.page = new StripWizardPage(pageName, pageTitle,
                MercurialEclipsePlugin.getImageDescriptor(iconPath), project);
        initPage(description, page);
        return page;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vectrace.MercurialEclipse.wizards.HgWizard#performFinish()
     */
    @Override
    public boolean performFinish() {
        return super.performFinish();
    }

}
