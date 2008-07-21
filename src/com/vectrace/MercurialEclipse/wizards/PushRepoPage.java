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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Composite;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
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
        // if (revCheckBox.getSelection()) {
        // ChangeSet cs = changesetTable.getSelection();
        //
        // String rev = cs.toString();
        //            if (rev != null && rev.length() > 0 && rev.indexOf(":") != -1) { //$NON-NLS-1$
        // // we save the nodeshort info
        //                this.revision = rev.split(":")[1]; //$NON-NLS-1$
        // }
        // }
        return super.finish(monitor);
    }
    
    @Override
    public boolean canFlipToNextPage() {
        try {
            if (getUrlCombo().getText() != null
                    && getUrlCombo().getText() != null) {
                OutgoingPage outgoingPage = (OutgoingPage) getNextPage();
                outgoingPage.setProject(resource.getProject());
                outgoingPage.setLocation(new HgRepositoryLocation(getUrlCombo()
                        .getText(), getUserCombo().getText(), getPasswordText()
                        .getText()));
                return isPageComplete()
                        && (getWizard().getNextPage(this) != null);
            }
        } catch (URISyntaxException e) {
            MercurialEclipsePlugin.showError(e);
        }
        return false;
    }

}
