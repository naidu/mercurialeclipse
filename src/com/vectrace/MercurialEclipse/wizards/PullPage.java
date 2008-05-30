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

import java.net.URISyntaxException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

/*
 * This file implements a wizard page which will allow the user to create a
 * repository location.
 * 
 */

public class PullPage extends PushPullPage {

    private Button updateCheckBox;
    private Button fetchCheckBox;

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
                        .getText(), getUserCombo().getText(), getPasswordText().getText()));
                return isPageComplete()
                        && (getWizard().getNextPage(this) != null);
            }
        } catch (URISyntaxException e) {
            MercurialEclipsePlugin.showError(e);
        }
        return false;
    }

    @Override
    public boolean isPageComplete() {
        return super.isPageComplete() && HgRepositoryLocation.validateLocation(getUrlCombo().getText());
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
        
        this.fetchCheckBox = createCheckBox(optionGroup, "Merge and, if there are no conflicts, commit after update");
        this.fetchCheckBox.moveBelow(this.updateCheckBox);
        
        SelectionListener fetchListener = new SelectionListener() {
            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetSelected(SelectionEvent e) {
                if (fetchCheckBox.getSelection()) {
                    String status;
                    try {
                        status = HgStatusClient.getStatus(resource.getProject());
                        if (status.length()>0 && status.indexOf("M ")>=0) {
                            setErrorMessage("Please commit modified resources before trying to merge.");      
                            setPageComplete(false);
                        } else {
                            setErrorMessage(null);
                            setPageComplete(true);
                        }
                    } catch (HgException e1) {
                        setErrorMessage("Couldn't get status from Mercurial. Merge disabled.");
                        fetchCheckBox.setSelection(false);
                        fetchCheckBox.setEnabled(false);
                        setPageComplete(true);
                    }
                    
                } else {
                  setErrorMessage(null);
                  setPageComplete(true);
                }
            }
        };
        
        fetchCheckBox.addSelectionListener(fetchListener);
        
        setPageComplete(true);
        setControl(composite);

    }

    /*
     * (non-Javadoc)
     * 
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

    /**
     * @return the mergeCheckBox
     */
    public Button getFetchCheckBox() {
        return fetchCheckBox;
    }

}
