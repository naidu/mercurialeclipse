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

import java.io.IOException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgServeClient;

/**
 * @author bastian
 * 
 */
public class ServeWizardPage extends HgWizardPage {

    private IResource hgroot;
    private Text nameTextField;
    private Text prefixTextField;
    private Button defaultCheckBox;
    private Text portTextField;
    private Button ipv6CheckBox;
    private Button stdioCheckBox;
    private Text webdirConfTextField;

    /**
     * @param string
     * @param string2
     * @param object
     * @param project
     */
    public ServeWizardPage(String pageName, String title,
            ImageDescriptor image, IResource hgRoot) {
        super(pageName, title, image);
        this.hgroot = hgRoot;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
     */
    public void createControl(Composite parent) {
        Composite composite = createComposite(parent, 3);
        // server settings

        final Group defaultsGroup = createGroup(composite, Messages.getString("ServeWizardPage.defaultsGroup.title")); //$NON-NLS-1$
        final Group settingsGroup = createGroup(composite, Messages.getString("ServeWizardPage.settingsGroup.title")); //$NON-NLS-1$
        settingsGroup.setEnabled(false);

        this.defaultCheckBox = createCheckBox(defaultsGroup, Messages.getString("ServeWizardPage.defaultCheckBox.title")); //$NON-NLS-1$
        this.defaultCheckBox.setSelection(true);        
        

        final Label portLabel = createLabel(settingsGroup,
                Messages.getString("ServeWizardPage.portLabel.title")); //$NON-NLS-1$
        portLabel.setEnabled(false);
        this.portTextField = createTextField(settingsGroup);
        this.portTextField.setEnabled(false);
        this.portTextField.setText(Messages.getString("ServeWizardPage.portTextField.defaultValue")); //$NON-NLS-1$

        final Label nameLabel = createLabel(settingsGroup,
                Messages.getString("ServeWizardPage.nameLabel.title")); //$NON-NLS-1$
        nameLabel.setEnabled(false);
        this.nameTextField = createTextField(settingsGroup);
        this.nameTextField.setEnabled(false);

        final Label prefixLabel = createLabel(settingsGroup,
                Messages.getString("ServeWizardPage.prefixLabel.title")); //$NON-NLS-1$
        prefixLabel.setEnabled(false);
        this.prefixTextField = createTextField(settingsGroup);
        this.prefixTextField.setEnabled(false);

        final Label webdirLabel = createLabel(settingsGroup,
                Messages.getString("ServeWizardPage.webdirLabel.title")); //$NON-NLS-1$
        webdirLabel.setEnabled(false);
        this.webdirConfTextField = createTextField(settingsGroup);
        this.webdirConfTextField.setEnabled(false);

        this.stdioCheckBox = createCheckBox(settingsGroup, Messages.getString("ServeWizardPage.stdioCheckBox.title")); //$NON-NLS-1$
        this.stdioCheckBox.setEnabled(false);

        this.ipv6CheckBox = createCheckBox(settingsGroup,
                Messages.getString("ServeWizardPage.ipv6CheckBox.title")); //$NON-NLS-1$
        this.ipv6CheckBox.setEnabled(false);
        
        SelectionListener defaultCheckBoxListener = new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }

            public void widgetSelected(SelectionEvent e) {
                settingsGroup.setEnabled(!defaultCheckBox.getSelection());
                portLabel.setEnabled(!defaultCheckBox.getSelection());                
                portTextField.setEnabled(!defaultCheckBox.getSelection());
                nameLabel.setEnabled(!defaultCheckBox.getSelection());
                nameTextField.setEnabled(!defaultCheckBox.getSelection());
                prefixLabel.setEnabled(!defaultCheckBox.getSelection());
                prefixTextField.setEnabled(!defaultCheckBox.getSelection());
                webdirLabel.setEnabled(!defaultCheckBox.getSelection());
                webdirConfTextField.setEnabled(!defaultCheckBox.getSelection());

                stdioCheckBox.setEnabled(!defaultCheckBox.getSelection());
                ipv6CheckBox.setEnabled(!defaultCheckBox.getSelection());
            }
        };
        
        this.defaultCheckBox.addSelectionListener(defaultCheckBoxListener);
        setControl(composite);
        setPageComplete(true);
    }

    @Override
    public boolean finish(IProgressMonitor monitor) {
        super.finish(monitor);
        // call hg
        try {
            new HgServeClient().serve(hgroot, Integer.parseInt(portTextField
                    .getText()), prefixTextField.getText(), nameTextField
                    .getText(), webdirConfTextField.getText(), stdioCheckBox
                    .getSelection(), ipv6CheckBox.getSelection());
        } catch (NumberFormatException e) {
            MercurialEclipsePlugin.logError(e);
            MercurialEclipsePlugin.showError(e);
        } catch (IOException e) {
            MercurialEclipsePlugin.logError(e);
            MercurialEclipsePlugin.showError(e);
        }
        return true;
    }

}
