/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;


import java.net.URISyntaxException;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Composite;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgPathsClient;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

/**
 * @author bastian
 * 
 */
public class PushRepoPage extends PushPullPage {

    public PushRepoPage(String pageName, String title,
            ImageDescriptor titleImage, IResource resource) {
        super(resource, pageName, title, titleImage);
        showRevisionTable = false;
    }
    
    /* (non-Javadoc)
     * @see com.vectrace.MercurialEclipse.wizards.PushPullPage#createControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createControl(Composite parent) {     
        super.createControl(parent);        
    }
    

    @Override
    public boolean finish(IProgressMonitor monitor) {
        this.force = forceCheckBox.getSelection();
        this.timeout = timeoutCheckBox.getSelection();
        return super.finish(monitor);
    }
    
    @Override
    public boolean canFlipToNextPage() {
        try {
            if (getUrlCombo().getText() != null
                    && getUrlCombo().getText() != null) {
                OutgoingPage outgoingPage = (OutgoingPage) getNextPage();
                outgoingPage.setProject(resource.getProject());
                HgRepositoryLocation loc = MercurialEclipsePlugin
                        .getRepoManager().getRepoLocation(urlCombo.getText(),
                                getUserCombo().getText(),
                                getPasswordText()
                                .getText());                
                outgoingPage.setLocation(loc);
                outgoingPage.setSvn(getSvnCheckBox() != null
                        && getSvnCheckBox().getSelection());
                setErrorMessage(null);
                return isPageComplete()
                        && (getWizard().getNextPage(this) != null);
            }
        } catch (URISyntaxException e) {
            setErrorMessage(e.getLocalizedMessage());
        }
        return false;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see
     * com.vectrace.MercurialEclipse.wizards.PushPullPage#setDefaultLocation()
     */
    @Override
    protected Map<String, HgRepositoryLocation> setDefaultLocation() {
        HgRepositoryLocation defaultLocation = null;
        Map<String, HgRepositoryLocation> paths = super.setDefaultLocation();
        if (paths == null) {
            return null;
        }
        if (paths.containsKey(HgPathsClient.DEFAULT_PUSH)) {
            defaultLocation = paths.get(HgPathsClient.DEFAULT_PUSH);
        } else if (paths.containsKey(HgPathsClient.DEFAULT)) {
            defaultLocation = paths.get(HgPathsClient.DEFAULT);
        }
        if (defaultLocation != null) {
            getUrlCombo().setText(defaultLocation.getDisplayLocation());
        }
        return paths;
    }

}
