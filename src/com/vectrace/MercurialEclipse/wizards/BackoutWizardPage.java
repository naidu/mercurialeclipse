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
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
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
import com.vectrace.MercurialEclipse.ui.ChangesetTable;

/**
 * @author bastian
 * 
 */
public class BackoutWizardPage extends HgWizardPage {

    private ChangesetTable changesetTable;
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
                Messages.getString("BackoutWizardPage.changeSetGroup.title"), GridData.FILL_BOTH); //$NON-NLS-1$

        changesetTable = new ChangesetTable(changeSetGroup);
        GridData gridData = new GridData(GridData.FILL_BOTH);        
        gridData.heightHint = 200;
        gridData.minimumHeight = 50;
        changesetTable.setLayoutData(gridData);

        SelectionListener listener = new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
                backoutRevision = changesetTable.getSelection();
                messageTextField.setText(Messages.getString("BackoutWizardPage.defaultCommitMessage") //$NON-NLS-1$
                        .concat(backoutRevision.toString()));
                setPageComplete(true);
            }

            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }

            
        };

        changesetTable.addSelectionListener(listener);
        changesetTable.setEnabled(true);
                

        // now the options
        Group optionGroup = createGroup(composite, Messages.getString("BackoutWizardPage.optionGroup.title")); //$NON-NLS-1$
        
        
        createLabel(optionGroup, Messages.getString("BackoutWizardPage.userLabel.text")); //$NON-NLS-1$
        this.userTextField = createTextField(optionGroup);
        this.userTextField.setText(MercurialUtilities.getHGUsername());

        
        createLabel(optionGroup, Messages.getString("BackoutWizardPage.commitLabel.text")); //$NON-NLS-1$
        this.messageTextField = createTextField(optionGroup);
        
        // --merge merge with old dirstate parent after backout
        this.mergeCheckBox = createCheckBox(optionGroup,
                Messages.getString("BackoutWizardPage.mergeCheckBox.text")); //$NON-NLS-1$
        this.mergeCheckBox.setSelection(true);
        
        try {
            populateBackoutChangesetTable();
        } catch (HgException e) {
            MessageDialog.openInformation(getShell(),
                    Messages.getString("BackoutWizardPage.changesetLoadingError"), e //$NON-NLS-1$
                            .getMessage());
            MercurialEclipsePlugin.logError(e);
        }
        setControl(composite);
    }    
    
    protected void populateBackoutChangesetTable() throws HgException {
        LocalChangesetCache.getInstance().refreshAllLocalRevisions(project,true);

        SortedSet<ChangeSet> changesets = LocalChangesetCache.getInstance()
                .getLocalChangeSets(project);
        
        SortedSet<ChangeSet> reverseOrderSet = new TreeSet<ChangeSet>(Collections.reverseOrder());
        reverseOrderSet.addAll(changesets);
        
        if (changesets != null) {
            changesetTable.setChangesets(reverseOrderSet.toArray(new ChangeSet[changesets.size()]));
        }
    }

    @Override
    public boolean finish(IProgressMonitor monitor) {
        String msg = messageTextField.getText();
        boolean merge = mergeCheckBox.getSelection();
        try {
            String result = HgBackoutClient.backout(project, backoutRevision,
                    merge, msg, userTextField.getText());
            MessageDialog.openInformation(getShell(), Messages.getString("BackoutWizardPage.backoutOutput"), //$NON-NLS-1$
                    result);
        } catch (HgException e) {
            MessageDialog.openError(getShell(), Messages.getString("BackoutWizardPage.backoutError"), e //$NON-NLS-1$
                    .getMessage());
            MercurialEclipsePlugin.logError(e);
            return false;
        }
        return true;
    }

}
