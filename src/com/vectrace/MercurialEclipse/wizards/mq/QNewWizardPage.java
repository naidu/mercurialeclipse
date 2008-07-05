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
package com.vectrace.MercurialEclipse.wizards.mq;

import static com.vectrace.MercurialEclipse.ui.SWTWidgetHelper.createCheckBox;
import static com.vectrace.MercurialEclipse.ui.SWTWidgetHelper.createComposite;
import static com.vectrace.MercurialEclipse.ui.SWTWidgetHelper.createGroup;
import static com.vectrace.MercurialEclipse.ui.SWTWidgetHelper.createLabel;
import static com.vectrace.MercurialEclipse.ui.SWTWidgetHelper.createTextField;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.wizards.HgWizardPage;

/**
 * @author bastian
 * 
 */
public class QNewWizardPage extends HgWizardPage {

    private IResource resource;
    private Text patchNameTextField;
    private Text userTextField;
    private Text date;
    private Text commitMessageTextField;
    private Button forceCheckBox;
    private Button gitCheckBox;
    private Text includeTextField;
    private Text excludeTextField;
    private boolean showPatchName;

    /**
     * @param pageName
     * @param title
     * @param titleImage
     * @param description
     */
    public QNewWizardPage(String pageName, String title,
            ImageDescriptor titleImage, String description, IResource resource,
            boolean showPatchName) {
        super(pageName, title, titleImage, description);
        this.resource = resource;
        this.showPatchName = showPatchName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
     */
    public void createControl(Composite parent) {
        Composite composite = createComposite(parent, 2);
        Group g = createGroup(composite, Messages.getString("QNewWizardPage.patchDataGroup.title")); //$NON-NLS-1$

        if (showPatchName) {
            createLabel(g, Messages.getString("QNewWizardPage.patchNameLabel.title")); //$NON-NLS-1$
            this.patchNameTextField = createTextField(g);
        }

        createLabel(g, Messages.getString("QNewWizardPage.userNameLabel.title")); //$NON-NLS-1$
        this.userTextField = createTextField(g);
        this.userTextField.setText(MercurialUtilities.getHGUsername());

        createLabel(g, Messages.getString("QNewWizardPage.dateLabel.title")); //$NON-NLS-1$
        this.date = createTextField(g);

        createLabel(g, Messages.getString("QNewWizardPage.commitMessageLabel.title")); //$NON-NLS-1$
        this.commitMessageTextField = createTextField(g);

        g = createGroup(composite, Messages.getString("QNewWizardPage.optionsGroup.title")); //$NON-NLS-1$
        this.forceCheckBox = createCheckBox(g,
                Messages.getString("QNewWizardPage.forceCheckBox.title")); //$NON-NLS-1$
        this.gitCheckBox = createCheckBox(g, Messages.getString("QNewWizardPage.gitCheckBox.title")); //$NON-NLS-1$
        this.gitCheckBox.setSelection(true);

        createLabel(g, Messages.getString("QNewWizardPage.includeLabel.title")); //$NON-NLS-1$
        this.includeTextField = createTextField(g);

        createLabel(g, Messages.getString("QNewWizardPage.excludeLabel.title")); //$NON-NLS-1$
        this.excludeTextField = createTextField(g);

        setControl(composite);
    }

    /**
     * @return the resource
     */
    public IResource getResource() {
        return resource;
    }

    /**
     * @return the patchNameTextField
     */
    public Text getPatchNameTextField() {
        return patchNameTextField;
    }

    /**
     * @param patchNameTextField
     *            the patchNameTextField to set
     */
    public void setPatchNameTextField(Text patchNameTextField) {
        this.patchNameTextField = patchNameTextField;
    }

    /**
     * @return the date
     */
    public Text getDate() {
        return date;
    }

    /**
     * @param date
     *            the date to set
     */
    public void setDate(Text date) {
        this.date = date;
    }

    /**
     * @return the commitMessageTextField
     */
    public Text getCommitMessageTextField() {
        return commitMessageTextField;
    }

    /**
     * @param commitMessageTextField
     *            the commitMessageTextField to set
     */
    public void setCommitMessageTextField(Text commitMessageTextField) {
        this.commitMessageTextField = commitMessageTextField;
    }

    /**
     * @return the forceCheckBox
     */
    public Button getForceCheckBox() {
        return forceCheckBox;
    }

    /**
     * @param forceCheckBox
     *            the forceCheckBox to set
     */
    public void setForceCheckBox(Button forceCheckBox) {
        this.forceCheckBox = forceCheckBox;
    }

    /**
     * @return the gitCheckBox
     */
    public Button getGitCheckBox() {
        return gitCheckBox;
    }

    /**
     * @param gitCheckBox
     *            the gitCheckBox to set
     */
    public void setGitCheckBox(Button gitCheckBox) {
        this.gitCheckBox = gitCheckBox;
    }

    /**
     * @return the includeTextField
     */
    public Text getIncludeTextField() {
        return includeTextField;
    }

    /**
     * @param includeTextField
     *            the includeTextField to set
     */
    public void setIncludeTextField(Text includeTextField) {
        this.includeTextField = includeTextField;
    }

    /**
     * @return the excludeTextField
     */
    public Text getExcludeTextField() {
        return excludeTextField;
    }

    /**
     * @param excludeTextField
     *            the excludeTextField to set
     */
    public void setExcludeTextField(Text excludeTextField) {
        this.excludeTextField = excludeTextField;
    }

    /**
     * @return the userTextField
     */
    public Text getUserTextField() {
        return userTextField;
    }

    /**
     * @param userTextField
     *            the userTextField to set
     */
    public void setUserTextField(Text userTextField) {
        this.userTextField = userTextField;
    }

}
