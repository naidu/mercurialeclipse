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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.extensions.HgSignClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.ui.ChangesetTable;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;

/**
 * @author Bastian Doetsch
 * 
 */
public class SignWizardPage extends HgWizardPage {

    private final IProject project;
    private Text userTextField;
    private Combo keyCombo;
    private Button localCheckBox;
    private Button forceCheckBox;
    private Button noCommitCheckBox;
    private ChangesetTable changesetTable;
    private Text messageTextField;
    private Text passTextField;
    private boolean gotGPGkeys;

    /**
     * @param pageName
     * @param title
     * @param titleImage
     * @param description
     * @param project
     */
    public SignWizardPage(String pageName, String title,
            ImageDescriptor titleImage, String description, IProject proj) {
        super(pageName, title, titleImage, description);
        this.project = proj;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
     */
    public void createControl(Composite parent) {

        Composite composite = SWTWidgetHelper.createComposite(parent, 2);

        // list view of changesets
        Group changeSetGroup = SWTWidgetHelper.createGroup(composite,
                Messages.getString("SignWizardPage.changeSetGroup.title"),GridData.FILL_BOTH); //$NON-NLS-1$
        GridData gridData = new GridData(GridData.FILL_BOTH);        
        gridData.heightHint = 200;
        gridData.minimumHeight = 50;
        this.changesetTable = new ChangesetTable(changeSetGroup, project);                
        this.changesetTable.setLayoutData(gridData);
        this.changesetTable.setEnabled(true);

        SelectionListener listener = new SelectionListener() {
            public void widgetSelected(SelectionEvent event) {
                ChangeSet cs = changesetTable.getSelection();
                messageTextField.setText(Messages.getString("SignWizardPage.messageTextField.text") //$NON-NLS-1$
                        .concat(cs.toString()));
                if (gotGPGkeys) {
                    setPageComplete(true);
                }
            }

            public void widgetDefaultSelected(SelectionEvent e) {
               widgetSelected(e);
            }
        };

        changesetTable.addSelectionListener(listener);

        // now the fields for user data
        Group userGroup = SWTWidgetHelper.createGroup(composite,
                Messages.getString("SignWizardPage.userGroup.title")); //$NON-NLS-1$

        SWTWidgetHelper.createLabel(userGroup, Messages.getString("SignWizardPage.userLabel.text")); //$NON-NLS-1$
        this.userTextField = SWTWidgetHelper.createTextField(userGroup);
        this.userTextField.setText(MercurialUtilities.getHGUsername());

        SWTWidgetHelper.createLabel(userGroup, Messages.getString("SignWizardPage.keyLabel.text")); //$NON-NLS-1$
        this.keyCombo = SWTWidgetHelper.createCombo(userGroup);

        SWTWidgetHelper.createLabel(userGroup, Messages.getString("SignWizardPage.passphraseLabel.text")); //$NON-NLS-1$
        this.passTextField = SWTWidgetHelper.createTextField(userGroup);
        // this.passTextField.setEchoChar('*');
        this.passTextField
                .setText(Messages.getString("SignWizardPage.passTextField.text")); //$NON-NLS-1$
        this.passTextField.setEnabled(false);

        // now the options
        Group optionGroup = SWTWidgetHelper.createGroup(composite, Messages.getString("SignWizardPage.optionGroup.title")); //$NON-NLS-1$

        this.localCheckBox = SWTWidgetHelper.createCheckBox(optionGroup,
                Messages.getString("SignWizardPage.localCheckBox.text")); //$NON-NLS-1$

        this.forceCheckBox = SWTWidgetHelper.createCheckBox(optionGroup,
                Messages.getString("SignWizardPage.forceCheckBox.text")); //$NON-NLS-1$

        this.noCommitCheckBox = SWTWidgetHelper.createCheckBox(optionGroup,
                Messages.getString("SignWizardPage.noCommitCheckBox.text")); //$NON-NLS-1$

        SWTWidgetHelper.createLabel(optionGroup, Messages.getString("SignWizardPage.commitLabel.text")); //$NON-NLS-1$
        this.messageTextField = SWTWidgetHelper.createTextField(optionGroup);
        this.messageTextField.setText(Messages.getString("SignWizardPage.messageTextField.defaultText")); //$NON-NLS-1$
        
        populateKeyCombo(keyCombo);
        setControl(composite);
    }

    private void populateKeyCombo(Combo combo) {
        try {
            String keys = HgSignClient.getPrivateKeyList();
            if (keys.indexOf("\n") == -1) { //$NON-NLS-1$
                combo.add(keys);
            } else {
                String[] items = keys.split("\n\n"); //$NON-NLS-1$
                for (String string : items) {
                    if (string.trim().startsWith("sec")) { //$NON-NLS-1$
                        combo.add(string.substring(6));
                    }
                }
            }
            gotGPGkeys = true;
        } catch (HgException e) {
            gotGPGkeys = false;
            combo.add(Messages.getString("SignWizardPage.errorLoadingGpgKeys")); //$NON-NLS-1$
            setPageComplete(false);
            MercurialEclipsePlugin.logError(e);
        }
        combo.setText(combo.getItem(0));
    }
    
    @Override    
    public boolean finish(IProgressMonitor monitor) {
        ChangeSet cs = changesetTable.getSelection();
        String key = keyCombo.getText();
        key = key.substring(key.indexOf("/") + 1, key.indexOf(" ")); //$NON-NLS-1$ //$NON-NLS-2$
        String msg = messageTextField.getText();
        String user = userTextField.getText();
        String pass = passTextField.getText();
        boolean local = localCheckBox.getSelection();
        boolean force = forceCheckBox.getSelection();
        boolean noCommit = noCommitCheckBox.getSelection();
        try {
            HgSignClient.sign(project.getLocation().toFile(), cs, key, msg,
                    user, local, force,
                    noCommit, pass);
        } catch (HgException e) {
            MessageDialog.openInformation(getShell(), Messages.getString("SignWizardPage.errorSigning"), //$NON-NLS-1$
                    e.getMessage());
            MercurialEclipsePlugin.logError(e);
            return false;
        }
        return true;
    }
}
