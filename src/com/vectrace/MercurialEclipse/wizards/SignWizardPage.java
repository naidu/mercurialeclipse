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

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgSignClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.ui.ChangesetTable;

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

        Composite composite = createComposite(parent, 2);

        // list view of changesets
        Group changeSetGroup = createGroup(composite,
                Messages.getString("SignWizardPage.changeSetGroup.title"),GridData.FILL_BOTH); //$NON-NLS-1$
        GridData gridData = new GridData(GridData.FILL_BOTH);        
        gridData.heightHint = 200;
        gridData.minimumHeight = 50;
        this.changesetTable = new ChangesetTable(changeSetGroup);                
        this.changesetTable.setLayoutData(gridData);
        this.changesetTable.setEnabled(true);

        SelectionListener listener = new SelectionListener() {
            public void widgetSelected(SelectionEvent event) {
                ChangeSet cs = changesetTable.getSelection();
                messageTextField.setText(Messages.getString("SignWizardPage.messageTextField.text") //$NON-NLS-1$
                        .concat(cs.toString()));
                setPageComplete(true);
            }

            public void widgetDefaultSelected(SelectionEvent e) {
               widgetSelected(e);
            }
        };

        changesetTable.addSelectionListener(listener);

        // now the fields for user data
        Group userGroup = createGroup(composite,
                Messages.getString("SignWizardPage.userGroup.title")); //$NON-NLS-1$

        createLabel(userGroup, Messages.getString("SignWizardPage.userLabel.text")); //$NON-NLS-1$
        this.userTextField = createTextField(userGroup);
        this.userTextField.setText(MercurialUtilities.getHGUsername());

        createLabel(userGroup, Messages.getString("SignWizardPage.keyLabel.text")); //$NON-NLS-1$
        this.keyCombo = createCombo(userGroup);

        createLabel(userGroup, Messages.getString("SignWizardPage.passphraseLabel.text")); //$NON-NLS-1$
        this.passTextField = createTextField(userGroup);
        // this.passTextField.setEchoChar('*');
        this.passTextField
                .setText(Messages.getString("SignWizardPage.passTextField.text")); //$NON-NLS-1$
        this.passTextField.setEnabled(false);

        // now the options
        Group optionGroup = createGroup(composite, Messages.getString("SignWizardPage.optionGroup.title")); //$NON-NLS-1$

        this.localCheckBox = createCheckBox(optionGroup,
                Messages.getString("SignWizardPage.localCheckBox.text")); //$NON-NLS-1$

        this.forceCheckBox = createCheckBox(optionGroup,
                Messages.getString("SignWizardPage.forceCheckBox.text")); //$NON-NLS-1$

        this.noCommitCheckBox = createCheckBox(optionGroup,
                Messages.getString("SignWizardPage.noCommitCheckBox.text")); //$NON-NLS-1$

        createLabel(optionGroup, Messages.getString("SignWizardPage.commitLabel.text")); //$NON-NLS-1$
        this.messageTextField = createTextField(optionGroup);
        this.messageTextField.setText(Messages.getString("SignWizardPage.messageTextField.defaultText")); //$NON-NLS-1$

        populateChangesetTable();
        populateKeyCombo(keyCombo);
        setControl(composite);
    }

    private void populateKeyCombo(Combo combo) {
        try {
            String keys = HgSignClient.getPrivateKeyList();
            if (keys.indexOf("\n") == -1) { //$NON-NLS-1$
                combo.add(keys);
            } else {
                String[] items = keys.split("\n"); //$NON-NLS-1$
                for (String string : items) {
                    if (string.trim().startsWith("pub")) { //$NON-NLS-1$
                        combo.add(string.substring(6));
                    }
                }
            }
        } catch (HgException e) {
            combo.setText(Messages.getString("SignWizardPage.errorLoadingGpgKeys")); //$NON-NLS-1$
            MercurialEclipsePlugin.logError(e);
        }
        combo.setText(combo.getItem(0));
    }

    private void populateChangesetTable() {
        try {
            LocalChangesetCache.getInstance().refreshAllLocalRevisions(project, true);
            SortedSet<ChangeSet> changesets = LocalChangesetCache.getInstance().getLocalChangeSets(project);
            if (changesets != null) {
                TreeSet<ChangeSet>revOrderSet = new TreeSet<ChangeSet>(Collections.reverseOrder());
                revOrderSet.addAll(changesets);
                changesetTable.setChangesets(revOrderSet.toArray(new ChangeSet[changesets.size()]));
            }
        } catch (HgException e) {
            MercurialEclipsePlugin.logError(e);
        }
    }    

    @Override    
    public boolean finish(IProgressMonitor monitor) {
        ChangeSet cs = (ChangeSet) ((IStructuredSelection) changesetTable
                .getSelection()).getFirstElement();
        String key = keyCombo.getText();
        key = key.substring(key.indexOf("/") + 1, key.indexOf("\\")); //$NON-NLS-1$ //$NON-NLS-2$
        String msg = messageTextField.getText();
        String user = userTextField.getText();
        String pass = passTextField.getText();
        boolean local = localCheckBox.getSelection();
        boolean force = forceCheckBox.getSelection();
        boolean noCommit = noCommitCheckBox.getSelection();
        try {
            HgSignClient.sign(project, cs, key, msg, user, local, force,
                    noCommit, pass);
        } catch (HgException e) {
            MessageDialog.openInformation(getShell(), Messages.getString("SignWizardPage.errorSigning"),
                    e.getMessage());
            MercurialEclipsePlugin.logError(e);
            return false;
        }
        return true;
    }
}
