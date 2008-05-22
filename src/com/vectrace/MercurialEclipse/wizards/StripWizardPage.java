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

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgStripClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.ui.ChangesetTable;

/**
 * @author bastian
 * 
 */
public class StripWizardPage extends HgWizardPage {

    private ChangesetTable changesetTable;
    private Button unrelatedCheckBox;
    protected ChangeSet stripRevision;
    private IProject project;
    private Button backupCheckBox;
    private Button stripHeadsCheckBox;
    private boolean unrelated;
    private boolean stripHeads;
    private boolean backup;

    /**
     * @param string
     * @param string2
     * @param object
     * @param project
     */
    public StripWizardPage(String pageName, String title,
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
                Messages.getString("StripWizardPage.changeSetGroup.title"), GridData.FILL_BOTH); //$NON-NLS-1$

        changesetTable = new ChangesetTable(changeSetGroup);
        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.heightHint = 200;
        gridData.minimumHeight = 50;
        changesetTable.setLayoutData(gridData);

        SelectionListener listener = new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
                stripRevision = changesetTable.getSelection();
                setPageComplete(true);
            }

            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }

        };

        changesetTable.addSelectionListener(listener);
        changesetTable.setEnabled(true);

        // now the options
        Group optionGroup = createGroup(composite, Messages.getString("StripWizardPage.optionsGroup.title")); //$NON-NLS-1$

        // backup
        this.unrelatedCheckBox = createCheckBox(
                optionGroup,
                Messages.getString("StripWizardPage.unrelatedCheckBox.title")); //$NON-NLS-1$
        this.unrelatedCheckBox.setSelection(true);
        this.backupCheckBox = createCheckBox(optionGroup, Messages.getString("StripWizardPage.backupCheckBox.title")); //$NON-NLS-1$
        this.backupCheckBox.setSelection(true);
        this.stripHeadsCheckBox = createCheckBox(optionGroup,
                Messages.getString("StripWizardPage.stripHeadsCheckBox.title")); //$NON-NLS-1$

        try {
            populateChangesetTable();
        } catch (HgException e) {
            MessageDialog.openInformation(getShell(),
                    Messages.getString("StripWizardPage.errorLoadChangesets"), e.getMessage()); //$NON-NLS-1$
            MercurialEclipsePlugin.logError(e);
        }
        setControl(composite);
    }

    protected void populateChangesetTable() throws HgException {
        LocalChangesetCache.getInstance().refreshAllLocalRevisions(project,
                true);

        SortedSet<ChangeSet> changesets = LocalChangesetCache.getInstance()
                .getLocalChangeSets(project);

        SortedSet<ChangeSet> reverseOrderSet = new TreeSet<ChangeSet>(
                Collections.reverseOrder());
        reverseOrderSet.addAll(changesets);

        if (changesets != null) {
            changesetTable.setChangesets(reverseOrderSet
                    .toArray(new ChangeSet[changesets.size()]));
        }
    }

    @Override
    public boolean finish(IProgressMonitor monitor) {
        super.finish(monitor);
        this.stripRevision = changesetTable.getSelection();
        this.unrelated = unrelatedCheckBox.getSelection();
        this.stripHeads = stripHeadsCheckBox.getSelection();
        this.backup = backupCheckBox.getSelection();
        try {
            String result = HgStripClient.strip(project, this.unrelated,
                    this.backup, this.stripHeads, stripRevision);
            MessageDialog.openInformation(getShell(), Messages.getString("StripWizardPage.outputMessage"), result); //$NON-NLS-1$
        } catch (HgException e) {
            MessageDialog.openError(getShell(), Messages.getString("StripWizardPage.errorCallingStrip"), e //$NON-NLS-1$
                    .getMessage());
            MercurialEclipsePlugin.logError(e);
            return false;
        }
        return true;
    }

}
