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
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.ui.ChangesetTable;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;

/**
 * @author bastian
 * 
 */
public class TransplantOptionsPage extends HgWizardPage {

    private IProject project;
    private boolean merge;
    private String mergeNodeId;
    private boolean prune;
    private String pruneNodeId;
    private String filter;
    private boolean filterChangesets;
    private boolean continueLastTransplant;
    private Button continueLastTransplantCheckBox;
    private Button filterChangesetsCheckBox;
    private Text filterTextField;
    private ChangesetTable mergeNodeIdTable;
    private Button mergeCheckBox;
    private Button pruneCheckBox;
    private ChangesetTable pruneNodeIdTable;
    private SortedSet<ChangeSet> changesets = new TreeSet<ChangeSet>(
            Collections.reverseOrder());

    public TransplantOptionsPage(String pageName, String title,
            ImageDescriptor titleImage, IProject project) {
        super(pageName, title, titleImage);
        this.project = project;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.wizard.WizardPage#canFlipToNextPage()
     */
    @Override
    public boolean canFlipToNextPage() {
        return super.canFlipToNextPage();
    }

    public void createControl(Composite parent) {
        Composite composite = SWTWidgetHelper.createComposite(parent, 2);
        addContinueOptionGroup(composite);
        addOtherOptionsGroup(composite);
        setControl(composite);
    }

    /**
     * @param composite
     */
    private void addOtherOptionsGroup(Composite composite) {
        createMergeGroup(composite);
        createPruneGroup(composite);
        createFilterGroup(composite);
    }

    /**
     * @param composite
     */
    private void createFilterGroup(Composite composite) {
        // filter
        Group filterGroup = SWTWidgetHelper.createGroup(composite, Messages.getString("TransplantOptionsPage.filtergroup.title")); //$NON-NLS-1$

        this.filterChangesetsCheckBox = SWTWidgetHelper.createCheckBox(filterGroup,
                Messages.getString("TransplantOptionsPage.filterCheckBox.title")); //$NON-NLS-1$

        SelectionListener filterChangesetsCheckBoxListener = new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
                filterTextField.setEnabled(filterChangesetsCheckBox
                        .getSelection());
            }

            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
        };

        this.filterChangesetsCheckBox
                .addSelectionListener(filterChangesetsCheckBoxListener);

        SWTWidgetHelper.createLabel(filterGroup, Messages.getString("TransplantOptionsPage.filterLabel.title")); //$NON-NLS-1$
        this.filterTextField = SWTWidgetHelper.createTextField(filterGroup);
        this.filterTextField.setEnabled(false);
        
        ModifyListener filterListener = new ModifyListener() {

            public void modifyText(ModifyEvent e) {
                filter = filterTextField.getText();
                validatePage();
            }
            
        };
        
        this.filterTextField.addModifyListener(filterListener);
        
    }

    /**
     * @param composite
     */
    private void createPruneGroup(Composite composite) {
        GridData gridData;
        // prune
        Group pruneGroup = SWTWidgetHelper.createGroup(composite, Messages.getString("TransplantOptionsPage.pruneGroup.title")); //$NON-NLS-1$
        this.pruneCheckBox = SWTWidgetHelper.createCheckBox(pruneGroup, Messages.getString("TransplantOptionsPage.pruneCheckBox.title")); //$NON-NLS-1$

        SelectionListener pruneCheckBoxListener = new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
                pruneNodeIdTable.setEnabled(pruneCheckBox.getSelection());
                populatePruneNodeIdTable();
            }

            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
        };

        this.pruneCheckBox.addSelectionListener(pruneCheckBoxListener);

        this.pruneNodeIdTable = new ChangesetTable(pruneGroup);
        gridData = new GridData(GridData.FILL_BOTH);
        gridData.heightHint = 200;
        gridData.minimumHeight = 50;
        this.pruneNodeIdTable.setLayoutData(gridData);
        this.pruneNodeIdTable.setEnabled(false);
        SelectionListener pruneTableListener = new SelectionListener() {

            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }

            public void widgetSelected(SelectionEvent e) {
                ChangeSet changeSet = pruneNodeIdTable.getSelection();
                pruneNodeId = changeSet.getChangeset();
                validatePage();
            }
        };
        pruneNodeIdTable.addSelectionListener(pruneTableListener);
    }

    /**
     * @param composite
     */
    private void createMergeGroup(Composite composite) {
        // other options
        Group mergeGroup = SWTWidgetHelper.createGroup(composite, Messages.getString("TransplantOptionsPage.mergeGroup.title")); //$NON-NLS-1$

        // merge at revision
        this.mergeCheckBox = SWTWidgetHelper.createCheckBox(mergeGroup, Messages.getString("TransplantOptionsPage.mergeCheckBox.title")); //$NON-NLS-1$

        SelectionListener mergeCheckBoxListener = new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
                mergeNodeIdTable.setEnabled(mergeCheckBox.getSelection());
                populateMergeNodeIdTable();
            }

            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
        };

        this.mergeCheckBox.addSelectionListener(mergeCheckBoxListener);
        this.mergeNodeIdTable = new ChangesetTable(mergeGroup);
        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.heightHint = 200;
        gridData.minimumHeight = 50;
        this.mergeNodeIdTable.setLayoutData(gridData);
        this.mergeNodeIdTable.setEnabled(false);

        SelectionListener mergeTableListener = new SelectionListener() {

            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }

            public void widgetSelected(SelectionEvent e) {
                ChangeSet changeSet = mergeNodeIdTable.getSelection();
                mergeNodeId = changeSet.getChangeset();
                validatePage();
            }
        };
        mergeNodeIdTable.addSelectionListener(mergeTableListener);
        populateMergeNodeIdTable();
    }
    
    private void loadChangesets() {
        if (changesets.size()==0) {
            TransplantPage page = (TransplantPage) getPreviousPage();
            changesets.addAll(page.getChangesets());
        }
    }

    private void validatePage() {       
        boolean valid = true;
        if (merge) {
            valid &= mergeNodeId != null && mergeNodeId.length() > 0;
        }
        if (prune) {
            valid &= pruneNodeId != null && pruneNodeId.length() > 0;
        }

        if (filterChangesets) {
            valid &= filter != null && filter.length() > 0;
        }
        if (continueLastTransplant) {
            valid = true;
        }
        setPageComplete(valid);
    }

    /**
     * @param composite
     */
    private void addContinueOptionGroup(Composite composite) {
        // other options
        Group continueGroup = SWTWidgetHelper.createGroup(composite, Messages.getString("TransplantOptionsPage.continueGroup.title")); //$NON-NLS-1$

        this.continueLastTransplantCheckBox = SWTWidgetHelper.createCheckBox(continueGroup,
                Messages.getString("TransplantOptionsPage.continueCheckBox.title")); //$NON-NLS-1$

        SelectionListener continueLastTransplantCheckBoxListener = new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
                TransplantOptionsPage.this.filterChangesetsCheckBox
                        .setEnabled(!continueLastTransplantCheckBox
                                .getSelection());
                TransplantOptionsPage.this.mergeCheckBox
                        .setEnabled(!continueLastTransplantCheckBox
                                .getSelection());
                TransplantOptionsPage.this.pruneCheckBox
                        .setEnabled(!continueLastTransplantCheckBox
                                .getSelection());
                validatePage();
            }

            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
        };

        this.continueLastTransplantCheckBox
                .addSelectionListener(continueLastTransplantCheckBoxListener);
    }

    /**
     * 
     */
    private void populatePruneNodeIdTable() {
        loadChangesets();
        pruneNodeIdTable.setChangesets(changesets
                .toArray(new ChangeSet[changesets.size()]));
    }

    /**
     * 
     */
    private void populateMergeNodeIdTable() {
        loadChangesets();
        mergeNodeIdTable.setChangesets(changesets
                .toArray(new ChangeSet[changesets.size()]));
    }

    @Override
    public boolean finish(IProgressMonitor monitor) {
        return super.finish(monitor);
    }

    /**
     * @return the project
     */
    public IProject getProject() {
        return project;
    }

    /**
     * @param project
     *            the project to set
     */
    public void setProject(IProject project) {
        this.project = project;
    }

    /**
     * @return
     */
    public boolean isMerge() {
        return this.merge;
    }

    /**
     * @return
     */
    public String getMergeNodeId() {
        return this.mergeNodeId;
    }

    /**
     * @return
     */
    public boolean isPrune() {
        return this.prune;
    }

    /**
     * @return
     */
    public String getPruneNodeId() {
        return this.pruneNodeId;
    }

    public boolean isFilterChangesets() {
        return this.filterChangesets;
    }

    /**
     * @return
     */
    public String getFilter() {
        return this.filter;
    }

    /**
     * @return
     */
    public boolean isContinueLastTransplant() {
        return this.continueLastTransplant;
    }

}
