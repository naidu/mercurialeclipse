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

import java.util.SortedSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgSignClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.team.MercurialStatusCache;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

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
    private ListViewer changeSetListView;
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
                "Please select the changeset to sign.");

        changeSetListView = super.createChangeSetListViewer(changeSetGroup,
                null, 200);

        ISelectionChangedListener listener = new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                setPageComplete(true);
            }
        };

        changeSetListView.addSelectionChangedListener(listener);

        // now the fields for user data
        Group userGroup = createGroup(composite,
                "Please enter the information needed for signing.");

        createLabel(userGroup, "User");
        this.userTextField = createTextField(userGroup);
        this.userTextField.setText(MercurialUtilities.getHGUsername());

        createLabel(userGroup, "Key");
        this.keyCombo = createCombo(userGroup);

        createLabel(userGroup, "Passphrase");
        this.passTextField = createTextField(userGroup);
        // this.passTextField.setEchoChar('*');
        this.passTextField
                .setText("Look out for the gpg agent that will ask you.");
        this.passTextField.setEnabled(false);

        // now the options
        Group optionGroup = createGroup(composite, "Please choose the options");

        this.localCheckBox = createCheckBox(optionGroup,
                "Make the signature local");

        this.forceCheckBox = createCheckBox(optionGroup,
                "Sign even if the sigfile is modified");

        this.noCommitCheckBox = createCheckBox(optionGroup,
                "Do not commit the sigfile after signing");

        createLabel(optionGroup, "Commit message");
        this.messageTextField = createTextField(optionGroup);
        this.messageTextField.setText("Signed changeset.");

        populateViewer(changeSetListView);
        populateKeyCombo(keyCombo);
        setControl(composite);
    }

    private void populateKeyCombo(Combo combo) {
        try {
            String keys = HgSignClient.getPrivateKeyList();
            if (keys.indexOf("\n") == -1) {
                combo.add(keys);
            } else {
                String[] items = keys.split("\n");
                for (String string : items) {
                    if (string.trim().startsWith("pub")) {
                        combo.add(string.substring(6));
                    }
                }
            }
        } catch (HgException e) {
            combo.setText("Couldn't load keys. See log for details.");
            MercurialEclipsePlugin.logError(e);
        }
        combo.setText(combo.getItem(0));
    }

    private void populateViewer(ListViewer viewer) {
        try {
            SortedSet<ChangeSet> changesets = MercurialStatusCache
                    .getInstance().getLocalChangeSets(project);
            if (changesets != null) {
                viewer
                        .add(changesets
                                .toArray(new ChangeSet[changesets.size()]));
            }
        } catch (HgException e) {
            MercurialEclipsePlugin.logError(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean canFlipToNextPage() {
        return ((IStructuredSelection) changeSetListView.getSelection()).size() == 1;
    }

    @SuppressWarnings("unchecked")
    public boolean finish(IProgressMonitor monitor) {
        ChangeSet cs = (ChangeSet) ((IStructuredSelection) changeSetListView
                .getSelection()).getFirstElement();
        String key = keyCombo.getText();
        key = key.substring(key.indexOf("/") + 1, key.indexOf(" "));
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
            MessageDialog.openInformation(getShell(), "Error while signing: ",
                    e.getMessage());
            MercurialEclipsePlugin.logError(e);
            return false;
        }
        return true;
    }
}
