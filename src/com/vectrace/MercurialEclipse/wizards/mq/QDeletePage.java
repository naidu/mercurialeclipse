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

import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.mq.HgQAppliedClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.Patch;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.ui.ChangesetTable;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;
import com.vectrace.MercurialEclipse.wizards.HgWizardPage;

/**
 * @author bastian
 * 
 */
public class QDeletePage extends HgWizardPage {

    private IResource resource;
    private ListViewer patchViewer;
    private ChangesetTable changesetTable;
    private Button revCheckBox;
    private Button keepCheckBox;

    /**
     * @param pageName
     * @param title
     * @param titleImage
     * @param description
     */
    public QDeletePage(String pageName, String title,
            ImageDescriptor titleImage, String description, IResource resource) {
        super(pageName, title, titleImage, description);
        this.resource = resource;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
     */
    public void createControl(Composite parent) {
        Composite composite = SWTWidgetHelper.createComposite(parent, 2);
        Group g = SWTWidgetHelper.createGroup(composite,
                "Select patches to delete");

        IBaseLabelProvider labelProvider = new LabelProvider() {
            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
             */
            @Override
            public String getText(Object element) {
                return element.toString();
            }

        };

        this.patchViewer = SWTWidgetHelper.createListViewer(g, "", 100,
                labelProvider);
        populatePatchViewer();                

        g = SWTWidgetHelper.createGroup(composite, "Options");
        this.keepCheckBox = SWTWidgetHelper.createCheckBox(g, "Keep patch file");
        this.revCheckBox = SWTWidgetHelper.createCheckBox(g,
                "Stop managing a revision");

        SelectionListener revListener = new SelectionListener() {

            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }

            public void widgetSelected(SelectionEvent e) {
                changesetTable.setEnabled(revCheckBox.getSelection());
            }

        };

        revCheckBox.addSelectionListener(revListener);

        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.heightHint = 150;
        gridData.minimumHeight = 50;
        this.changesetTable = new ChangesetTable(g);
        this.changesetTable.setLayoutData(gridData);
        this.changesetTable.setEnabled(false);
        populateChangesetTable();
        setControl(composite);

    }

    /**
     * 
     */
    private void populateChangesetTable() {
        try {
            SortedSet<ChangeSet> changesets = LocalChangesetCache.getInstance()
                    .getLocalChangeSets(resource.getProject());
            if (changesets != null) {
                TreeSet<ChangeSet> temp = new TreeSet<ChangeSet>(Collections.reverseOrder());
                temp.addAll(changesets);
                changesetTable.setChangesets(temp.toArray(new ChangeSet[temp.size()]));
            }
        } catch (HgException e) {
           MercurialEclipsePlugin.logError(e);
           setErrorMessage(e.getLocalizedMessage());
        }

    }

    /**
     * 
     */
    private void populatePatchViewer() {
        try {
            List<Patch>patches = HgQAppliedClient.getUnappliedPatches(resource);
            for (Patch patch : patches) {
                patchViewer.add(patch);
            }
        } catch (HgException e) {
            MercurialEclipsePlugin.logError(e);
            setErrorMessage(e.getLocalizedMessage());
        }
    }

    /**
     * @return the patchViewer
     */
    public ListViewer getPatchViewer() {
        return patchViewer;
    }

    /**
     * @return the resource
     */
    public IResource getResource() {
        return resource;
    }

    /**
     * @param resource the resource to set
     */
    public void setResource(IResource resource) {
        this.resource = resource;
    }

    /**
     * @return the revCheckBox
     */
    public Button getRevCheckBox() {
        return revCheckBox;
    }

    /**
     * @return the keepCheckBox
     */
    public Button getKeepCheckBox() {
        return keepCheckBox;
    }

    /**
     * @return the changesetTable
     */
    public ChangesetTable getChangesetTable() {
        return changesetTable;
    }

}
