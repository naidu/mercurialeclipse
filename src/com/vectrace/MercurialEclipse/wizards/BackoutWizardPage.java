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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgBackoutClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;

/**
 * @author bastian
 * 
 */
public class BackoutWizardPage extends HgWizardPage {

    private ListViewer changeSetListView;
    private Text messageTextField;
    private Button mergeCheckBox;
    protected ChangeSet backoutRevision;
    private IProject project;
    private Text userTextField;

    /**
     * @param string
     * @param string2
     * @param object
     * @param project
     */
    public BackoutWizardPage(String pageName, String title,
            ImageDescriptor image, IProject project) {
        super(pageName, title, image);
        this.project = project;
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
                "Please select the changeset to back out from");

        changeSetListView = super.createChangeSetListViewer(changeSetGroup,
                null, 100);

        ISelectionChangedListener listener = new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                backoutRevision = (ChangeSet) ((IStructuredSelection) event
                        .getSelection()).getFirstElement();
                messageTextField.setText("Backed out of changeset "
                        .concat(backoutRevision.toString()));
                setPageComplete(true);
            }
        };

        changeSetListView.addSelectionChangedListener(listener);
        changeSetListView.getControl().setEnabled(true);
                

        // now the options
        Group optionGroup = createGroup(composite, "Please choose the options");
        
        
        createLabel(optionGroup, "User");
        this.userTextField = createTextField(optionGroup);
        this.userTextField.setText(MercurialUtilities.getHGUsername());

        
        createLabel(optionGroup, "Commit message");
        this.messageTextField = createTextField(optionGroup);
        
        // --merge merge with old dirstate parent after backout
        this.mergeCheckBox = createCheckBox(optionGroup,
                "Merge with old dirstate parent after backout");
        this.mergeCheckBox.setSelection(true);
        
        try {
            populateBackoutRevisionListView();
        } catch (HgException e) {
            MessageDialog.openInformation(getShell(),
                    "Error while loading local changesets", e
                            .getMessage());
            MercurialEclipsePlugin.logError(e);
        }
        setControl(composite);
    }    
    
    protected void populateBackoutRevisionListView() throws HgException {
        SortedSet<ChangeSet> changesets = LocalChangesetCache.getInstance()
                .getLocalChangeSets(project);
        if (changesets != null) {
            for (ChangeSet changeSet : changesets) {
                changeSetListView.add(changeSet);
            }
        }
    }

    @Override
    public boolean finish(IProgressMonitor monitor) {
        String msg = messageTextField.getText();
        boolean merge = mergeCheckBox.getSelection();
        try {
            String result = HgBackoutClient.backout(project, backoutRevision,
                    merge, msg, userTextField.getText());
            MessageDialog.openInformation(getShell(), "Backout output: ",
                    result);
        } catch (HgException e) {
            MessageDialog.openError(getShell(), "Error while backing out: ", e
                    .getMessage());
            MercurialEclipsePlugin.logError(e);
            return false;
        }
        return true;
    }

}
