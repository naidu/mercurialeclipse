/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - some updates
 *     Stefan C                  - Code cleanup
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import java.net.MalformedURLException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

/*
 * This file implements a wizard page which will allow the user to create a
 * repository location.
 * 
 */

public class PullPage extends PushPullPage {

    private Button updateCheckBox;

    /**
     * @param pageName
     */
    public PullPage(String pageName, String title, String description,
            IResource resource, ImageDescriptor titleImage) {
        super(resource, pageName, title, titleImage);
        setDescription(description);
        setShowCredentials(true);
        setShowBundleButton(true);
        setShowRevisionTable(false);
    }

    @Override
    public boolean canFlipToNextPage() {
        try {
            if (getUrlCombo().getText() != null
                    && getUrlCombo().getText() != null) {
                IncomingPage incomingPage = (IncomingPage) getNextPage();
                incomingPage.setProject(resource.getProject());
                incomingPage.setLocation(new HgRepositoryLocation(getUrlCombo()
                        .getText()));
                return isPageComplete()
                        && (getWizard().getNextPage(this) != null);
            }
        } catch (MalformedURLException e) {
            MercurialEclipsePlugin.showError(e);
        }
        return false;
    }

    @Override
    public boolean isPageComplete() {
        return HgRepositoryLocation.validateLocation(getUrlCombo().getText());
    }

    protected boolean isPageComplete(String url) {
        return HgRepositoryLocation.validateLocation(url);
    }

    protected boolean validateAndSetComplete(String url) {
        boolean validLocation = isPageComplete(url);

        setPageComplete(validLocation);

        return validLocation;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createControl(Composite parent) {
        super.createControl(parent);
        Composite composite = (Composite) getControl();        

        // now the options
        this.updateCheckBox = createCheckBox(optionGroup, Messages
                .getString("PullPage.toggleUpdate.text")); //$NON-NLS-1$

        this.updateCheckBox.moveAbove(this.revCheckBox);

        setPageComplete(false);
        setControl(composite);

    }

    /* (non-Javadoc)
     * @see com.vectrace.MercurialEclipse.wizards.ConfigurationWizardMainPage#finish(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public boolean finish(IProgressMonitor monitor) {
        return super.finish(monitor);        
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.vectrace.MercurialEclipse.wizards.PushPullPage#getForceCheckBoxLabel()
     */
    @Override
    protected String getForceCheckBoxLabel() {
        return Messages.getString("PullPage.forceCheckBox.title"); //$NON-NLS-1$
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vectrace.MercurialEclipse.wizards.PushPullPage#getRevGroupLabel()
     */
    @Override
    protected String getRevGroupLabel() {
        return Messages.getString("PullPage.revGroup.title"); //$NON-NLS-1$
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vectrace.MercurialEclipse.wizards.PushPullPage#getRevCheckBoxLabel()
     */
    @Override
    protected String getRevCheckBoxLabel() {
        return Messages.getString("PullPage.revCheckBox.title"); //$NON-NLS-1$
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vectrace.MercurialEclipse.wizards.PushPullPage#getTimeoutCheckBoxLabel()
     */
    @Override
    protected String getTimeoutCheckBoxLabel() {
        return Messages.getString("PullPage.timeoutCheckBox.title"); //$NON-NLS-1$
    }

    /**
     * @return the updateCheckBox
     */
    public Button getUpdateCheckBox() {
        return updateCheckBox;
    }

    /**
     * @param updateCheckBox
     *            the updateCheckBox to set
     */
    public void setUpdateCheckBox(Button updateCheckBox) {
        this.updateCheckBox = updateCheckBox;
    }

    
}
