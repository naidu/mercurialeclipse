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
 *     Bastian Doetsch           - usage
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import org.eclipse.jface.resource.ImageDescriptor;

/**
 * This file implements a wizard page which will allow the user to choose a
 * repository location.
 * 
 */
public class SynchronizeWizardPage extends SyncRepoPage {   

    public SynchronizeWizardPage(String pageName, String title,
            ImageDescriptor titleImage) {
        super(pageName, title, titleImage);
    }

    @Override
    protected boolean validateAndSetComplete(String url) {
        boolean isValid = super.isPageComplete(url);
        setPageComplete(isValid);
        return isValid;
    }
    
    @Override
    protected String getBrowseDialogMessage() {
        return "Select local repository for synchronization.";
    }
    
    @Override
    protected String getProjectNameLabelText() {
        return "";
    }
    
    

}
