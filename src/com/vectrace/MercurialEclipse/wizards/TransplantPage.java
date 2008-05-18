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

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import com.vectrace.MercurialEclipse.ui.ChangesetTable;

/**
 * @author bastian
 * 
 */
public class TransplantPage extends ConfigurationWizardMainPage {

    private IProject project;
    private List<String> nodeIds;
    private boolean branch;
    private String branchName;
    private boolean all;
    private boolean merge;
    private String mergeNodeId;
    private boolean prune;
    private String pruneNodeId;
    private String filter;
    private boolean filterChangesets;
    private boolean continueLastTransplant;
    private ChangesetTable changesetTable;
    private Button branchCheckBox;
    private Combo branchNameCombo;
    private Button allCheckBox;

    public TransplantPage(String pageName, String title,
            ImageDescriptor titleImage, IProject project) {
        super(pageName, title, titleImage);
        this.project = project;
    }

    @Override
    public void createControl(Composite parent) {
        super.createControl(parent);
        Composite composite = (Composite) getControl();

        // now the options
        Group selectionGroup = createGroup(composite,
                "Select, if you want to transplant from a branch");
        this.branchCheckBox = createCheckBox(selectionGroup,
                "Pull patches from branch");
        this.allCheckBox = createCheckBox(
                selectionGroup,
                "Transplant all revisions on branch. If you select a revision, the branch\n"
                        + "will be rebased up to the selected revision onto your current working directory.");
        this.allCheckBox.setEnabled(false);

        createLabel(selectionGroup, "Branch name");
        this.branchNameCombo = createEditableCombo(selectionGroup);
        this.branchNameCombo.setEnabled(false);

        SelectionListener branchCheckBoxListener = new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
                TransplantPage.this.getUrlCombo().setEnabled(
                        !branchCheckBox.getSelection());
                TransplantPage.this.getUserCombo().setEnabled(
                        !branchCheckBox.getSelection());
                TransplantPage.this.getPasswordText().setEnabled(
                        !branchCheckBox.getSelection());
                TransplantPage.this.allCheckBox.setEnabled(branchCheckBox
                        .getSelection());
                TransplantPage.this.branchNameCombo.setEnabled(branchCheckBox
                        .getSelection());
            }

            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
        };

        this.branchCheckBox.addSelectionListener(branchCheckBoxListener);

        // table of changesets
        Group changeSetGroup = createGroup(composite,
                "Select changesets to transplant", GridData.FILL_BOTH);

        GridData gridData = new GridData(GridData.FILL_BOTH);        
        gridData.heightHint = 200;
        gridData.minimumHeight = 50;
        this.changesetTable = new ChangesetTable(changeSetGroup);                
        this.changesetTable.setLayoutData(gridData);
        this.changesetTable.setEnabled(false);

        SelectionListener changeSetTableListener = new SelectionListener() {

            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }

            public void widgetSelected(SelectionEvent e) {
                setPageComplete(true);
                // TODO Implement
            }
        };

        changesetTable.addSelectionListener(changeSetTableListener);
        
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
    public boolean isBranch() {
        return this.branch;
    }

    /**
     * @return the nodeIds
     */
    public List<String> getNodeIds() {
        return nodeIds;
    }

    /**
     * @return
     */
    public String getBranchName() {
        return this.branchName;
    }

    /**
     * @return
     */
    public boolean isAll() {
        return this.all;
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
