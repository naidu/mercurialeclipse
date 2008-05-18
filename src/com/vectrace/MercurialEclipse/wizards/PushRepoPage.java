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

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.ui.ChangesetTable;

/**
 * @author bastian
 * 
 */
public class PushRepoPage extends ConfigurationWizardMainPage {

    private Button forceCheckBox;
    private boolean force;
    private ChangesetTable changesetTable;
    private IProject project;
    private String revision;
    private Button revCheckBox;
    private Button timeoutCheckBox;
    private boolean timeout;

    public PushRepoPage(String pageName, String title,
            ImageDescriptor titleImage, IProject project) {
        super(pageName, title, titleImage);
        this.project = project;
    }

    @Override
    public void createControl(Composite parent) {
        super.createControl(parent);
        Composite composite = (Composite) getControl();

        // now the options
        Group optionGroup = createGroup(composite, Messages
                .getString("PushRepoPage.optionGroup.title")); //$NON-NLS-1$
        this.timeoutCheckBox = createCheckBox(optionGroup, Messages
                .getString("PushRepoPage.timeoutCheckBox.text")); //$NON-NLS-1$
        this.forceCheckBox = createCheckBox(optionGroup, Messages
                .getString("PushRepoPage.forceCheckBox.text")); //$NON-NLS-1$
        this.revCheckBox = createCheckBox(optionGroup, Messages
                .getString("PushRepoPage.revCheckBox.text")); //$NON-NLS-1$

        Listener revCheckBoxListener = new Listener() {
            public void handleEvent(Event event) {
                if (revCheckBox.getSelection()) {
                    if (changesetTable.getChangesets() == null
                            || changesetTable.getChangesets().length == 0) {
                        try {
                            populateChangesetTable();
                        } catch (HgException e) {
                            MessageDialog
                                    .openInformation(
                                            getShell(),
                                            Messages
                                                    .getString("PushRepoPage.errorLoadingChangesets"), e //$NON-NLS-1$
                                                    .getMessage());
                            MercurialEclipsePlugin.logError(e);
                        }
                    }
                }
                // en-/disable list view
                changesetTable.setEnabled(revCheckBox.getSelection());
            }
        };

        this.revCheckBox.addListener(SWT.Selection, revCheckBoxListener);

        Group revGroup = createGroup(composite, Messages
                .getString("PushRepoPage.revGroup.title"),GridData.FILL_BOTH); //$NON-NLS-1$
        
        GridData gridData = new GridData(GridData.FILL_BOTH);        
        gridData.heightHint = 200;
        gridData.minimumHeight = 50;
        this.changesetTable = new ChangesetTable(revGroup);                
        this.changesetTable.setLayoutData(gridData);
        this.changesetTable.setEnabled(false);

        SelectionListener listener = new SelectionListener() {
            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetSelected(SelectionEvent e) {
                setPageComplete(true);
                revision = changesetTable.getSelection().toString();
            }

            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
        };

        this.changesetTable.addSelectionListener(listener);

    }

    private void populateChangesetTable() throws HgException {
        SortedSet<ChangeSet> changesets = LocalChangesetCache.getInstance()
                .getLocalChangeSets(project);
        if (changesets != null) {
            TreeSet<ChangeSet> reverseOrderSet = new TreeSet<ChangeSet>(
                    Collections.reverseOrder());
            reverseOrderSet.addAll(changesets);
            changesetTable.setChangesets(reverseOrderSet
                    .toArray(new ChangeSet[reverseOrderSet.size()]));
        }
    }

    @Override
    public boolean finish(IProgressMonitor monitor) {
        this.force = forceCheckBox.getSelection();
        this.timeout = timeoutCheckBox.getSelection();
        if (revCheckBox.getSelection()) {
            ChangeSet cs = changesetTable.getSelection();

            String rev = cs.toString();
            if (rev != null && rev.length() > 0 && rev.indexOf(":") != -1) { //$NON-NLS-1$
                // we save the nodeshort info
                this.revision = rev.split(":")[1]; //$NON-NLS-1$
            }
        }
        return super.finish(monitor);
    }

    /**
     * @return the force
     */
    public boolean isForce() {
        return force;
    }

    /**
     * @return the revision
     */
    public String getRevision() {
        return revision;
    }

    /**
     * @return the timeout
     */
    public boolean isTimeout() {
        return timeout;
    }

}
