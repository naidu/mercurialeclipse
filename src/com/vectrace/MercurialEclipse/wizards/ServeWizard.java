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
import org.eclipse.core.runtime.NullProgressMonitor;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;

/**
 * @author bastian
 * 
 */
public class ServeWizard extends HgWizard {
    private IProject project;
    private ServeWizardPage page;

    private ServeWizard() {
        super(Messages.getString("ServeWizard.name")); //$NON-NLS-1$
        setNeedsProgressMonitor(true);
    }

    public ServeWizard(IResource resource) {
        this();
        this.project = resource.getProject();
    }

    @Override
    public void addPages() {
        super.addPages();
        page = createPage(Messages.getString("ServeWizard.pageName"), Messages.getString("ServeWizard.pageTitle"), Messages //$NON-NLS-1$ //$NON-NLS-2$
                .getString("NewLocationWizard.repoCreationPage.image"), //$NON-NLS-1$
                Messages.getString("ServeWizard.pageDescription")); //$NON-NLS-1$
        addPage(page);
    }

    /**
     * Creates a Page.
     */
    protected ServeWizardPage createPage(String pageName, String pageTitle,
            String iconPath, String description) {
        this.page = new ServeWizardPage(pageName, pageTitle,
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
        page.finish(new NullProgressMonitor());
        return super.performFinish();
    }

}
