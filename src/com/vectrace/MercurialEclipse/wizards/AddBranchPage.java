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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;

/**
 * @author bastian
 *
 */
public class AddBranchPage extends HgWizardPage {

    private Button forceCheckBox;
    private Text branchNameTextField;

    /**
     * @param pageName
     * @param title
     * @param titleImage
     * @param description
     */
    public AddBranchPage(String pageName, String title,
            ImageDescriptor titleImage, String description) {
        super(pageName, title, titleImage, description);        
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
     */
    public void createControl(Composite parent) {
        Composite composite = SWTWidgetHelper.createComposite(parent, 2);
        SWTWidgetHelper.createLabel(composite, Messages.getString("AddBranchPage.branchNameTextField.title")); //$NON-NLS-1$
        this.branchNameTextField = SWTWidgetHelper.createTextField(composite);                
        this.forceCheckBox = SWTWidgetHelper.createCheckBox(composite, Messages.getString("AddBranchPage.forceCheckBox.title"));         //$NON-NLS-1$
        setControl(composite);
    }
    
    /* (non-Javadoc)
     * @see com.vectrace.MercurialEclipse.wizards.HgWizardPage#finish(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public boolean finish(IProgressMonitor monitor) {
        return super.finish(monitor);
    }

    /**
     * @return the forceCheckBox
     */
    public Button getForceCheckBox() {
        return forceCheckBox;
    }

    /**
     * @return the branchNameTextField
     */
    public Text getBranchNameTextField() {
        return branchNameTextField;
    }

}
