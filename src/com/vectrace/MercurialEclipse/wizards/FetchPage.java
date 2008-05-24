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

import java.net.MalformedURLException;
import java.util.Properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Composite;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

/**
 * @author bastian
 * 
 */
public class FetchPage extends ConfigurationWizardMainPage {

    private IProject project;

    public FetchPage(String pageName, String title, ImageDescriptor titleImage,
            IProject project) {
        super(pageName, title, titleImage);
        this.project = project;
        setShowBundleButton(true);
    }

    @Override
    public void createControl(Composite parent) {
        super.createControl(parent);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vectrace.MercurialEclipse.wizards.ConfigurationWizardMainPage#canFlipToNextPage()
     */
    @Override
    public boolean canFlipToNextPage() {
        try {
            super.finish(new NullProgressMonitor());
            IncomingPage incomingPage = (IncomingPage) getNextPage();
            incomingPage.setProject(project);
            Properties props = this.getProperties();
            if (props != null) {
                HgRepositoryLocation repo = HgRepositoryLocation
                        .fromProperties(props);
                incomingPage.setLocation(repo);
                return super.canFlipToNextPage();
            }
        } catch (HgException e) {
            MercurialEclipsePlugin.showError(e);
        } catch (MalformedURLException e) {
            MercurialEclipsePlugin.showError(e);
        }
        return false;
    }

    @Override
    public boolean finish(IProgressMonitor monitor) {
        return super.finish(monitor);
    }

    /**
     * @return the project
     */
    public IProject getProject() {
        return project;
    }

    /**
     * @param project
     *            the project to set
     */
    public void setProject(IProject project) {
        this.project = project;
    }

}
