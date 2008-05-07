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
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;

/**
 * @author bastian
 * 
 */
public class PushRepoPage extends ConfigurationWizardMainPage {

    private Button forceCheckBox;
    private boolean force;
    private ListViewer revisionListView;
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
        Group optionGroup = createGroup(composite, "Please select the options");
        this.timeoutCheckBox = createCheckBox(optionGroup, "Abort push when a timeout occurs (might be dangerous)");
        this.forceCheckBox = createCheckBox(optionGroup, "Force Push");        
        this.revCheckBox = createCheckBox(optionGroup, "Push up to a revision");

        Listener revCheckBoxListener = new Listener() {
            public void handleEvent(Event event) {
                if (revCheckBox.getSelection()) {
                    if (revisionListView.getList().getItems().length == 0) {
                        try {
                            populateRevisionListView();
                        } catch (HgException e) {
                            MessageDialog.openInformation(getShell(),
                                    "Error while loading local changesets", e
                                            .getMessage());
                            MercurialEclipsePlugin.logError(e);
                        }
                    }                    
                }
                // en-/disable list view
                revisionListView.getControl().setEnabled(revCheckBox.getSelection());
            }
        };

        this.revCheckBox.addListener(SWT.Selection, revCheckBoxListener);       

        Group revGroup = createGroup(composite, "Please select the revision up to which you want to push");
        
        this.revisionListView = createChangeSetListViewer(revGroup, null,
                100);

        this.revisionListView.getControl().setEnabled(false);

        ISelectionChangedListener listener = new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                setPageComplete(true);
                revision = revisionListView.getSelection().toString();
            }
        };

        revisionListView.addSelectionChangedListener(listener);

    }

    private void populateRevisionListView() throws HgException {
        SortedSet<ChangeSet> changesets = LocalChangesetCache.getInstance()
                .getLocalChangeSets(project);
        if (changesets != null) {
            TreeSet<ChangeSet> reverseOrderSet = new TreeSet<ChangeSet>(
                    Collections.reverseOrder());
            reverseOrderSet.addAll(changesets);
            for (ChangeSet changeSet : reverseOrderSet) {
                revisionListView.add(changeSet);
            }
        }
    }

    @Override
    public boolean finish(IProgressMonitor monitor) {
        this.force = forceCheckBox.getSelection();
        this.timeout = timeoutCheckBox.getSelection();
        if (revCheckBox.getSelection()) {
            ChangeSet cs = (ChangeSet) ((IStructuredSelection) revisionListView
                    .getSelection()).getFirstElement();

            String rev = cs.toString();
            if (rev != null && rev.length() > 0 && rev.indexOf(":") != -1) {
                // we save the nodeshort info
                this.revision = rev.split(":")[1];
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
     * @return the revisionTextField
     */
    public ListViewer getRevisionListView() {
        return revisionListView;
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
