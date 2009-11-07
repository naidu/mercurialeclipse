/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian Doetsch	implementation
 * Andrei Loskutov (Intland) - bugfixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgBranchClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.Branch;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.cache.IncomingChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.ui.ChangesetTable;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;

/**
 * @author bastian
 *
 */
public class TransplantPage extends ConfigurationWizardMainPage {

    private IProject project;
    private final List<String> nodeIds = new ArrayList<String>();
    private boolean branch;
    private String branchName;
    private boolean all;
    private ChangesetTable changesetTable;
    private Button branchCheckBox;
    private Combo branchNameCombo;
    private Button allCheckBox;
    private final SortedSet<ChangeSet> changesets = new TreeSet<ChangeSet>(
            Collections.reverseOrder());

    public TransplantPage(String pageName, String title,
            ImageDescriptor titleImage, IProject project) {
        super(pageName, title, titleImage);
        this.project = project;
    }

    @Override
    public void createControl(Composite parent) {
        super.createControl(parent);
        Composite composite = (Composite) getControl();

        ModifyListener urlModifyListener = new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                HgRepositoryLocation repoLocation;
                try {
                    repoLocation = MercurialEclipsePlugin.getRepoManager()
                            .getRepoLocation(getUrlCombo().getText());
                } catch (HgException e1) {
                    // bad URI?
                    setErrorMessage(e1.getMessage());
                    return;
                }
                setErrorMessage(null);
                try {
                    Set<ChangeSet> changes = IncomingChangesetCache
                            .getInstance().getChangeSets(project, repoLocation, null);
                    changesets.clear();
                    changesets.addAll(changes);
                    populateChangesetTable();
                } catch (HgException e1) {
                    setErrorMessage(Messages.getString("TransplantPage.errorLoadChangesets")); //$NON-NLS-1$)
                    MercurialEclipsePlugin.logError(e1);
                }
            }
        };
        getUrlCombo().addModifyListener(urlModifyListener);

        addBranchGroup(composite);
        addChangesetGroup(composite);
    }

    @Override
    public boolean canFlipToNextPage() {
        return super.canFlipToNextPage();
    }

    private void validatePage() {
        boolean valid = true;
        if (branch) {
            valid &= branchName != null;
            if (!all) {
                valid &= nodeIds.size() > 0;
            }
        } else {
            valid &= nodeIds.size() > 0;
        }
        setPageComplete(valid);
    }

    private void addBranchGroup(Composite composite) {
        // now the branch group
        Group branchGroup = SWTWidgetHelper.createGroup(composite, Messages
                .getString("TransplantPage.branchGroup.title")); //$NON-NLS-1$
        createBranchCheckBox(branchGroup);
        createAllCheckBox(branchGroup);
        createBranchNameCombo(branchGroup);
    }

    private void createBranchNameCombo(Group branchGroup) {
        SWTWidgetHelper.createLabel(branchGroup, Messages
                .getString("TransplantPage.branchLabel.title")); //$NON-NLS-1$
        this.branchNameCombo = SWTWidgetHelper.createCombo(branchGroup);
        this.branchNameCombo.setEnabled(false);
        populateBranchNameCombo();

        SelectionListener branchNameComboListener = new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
                // TODO filter changeset table
                branchName = branchNameCombo.getText();
                if (Branch.isDefault(branchName)) {
                    branchName = ""; //$NON-NLS-1$
                }

                try {
                    changesets.clear();
                    changesets.addAll(LocalChangesetCache.getInstance()
                            .getOrFetchChangeSetsByBranch(project, branchName));
                    populateChangesetTable();
                } catch (HgException e1) {
                    setErrorMessage(Messages
                            .getString("TransplantPage.errorLoadChangesets")); //$NON-NLS-1$
                    MercurialEclipsePlugin.logError(e1);
                }

                validatePage();
            }

            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
        };

        this.branchNameCombo.addSelectionListener(branchNameComboListener);
    }

    private void createAllCheckBox(Group branchGroup) {
        this.allCheckBox = SWTWidgetHelper.createCheckBox(branchGroup, Messages
                .getString("TransplantPage.allCheckBox.title.1") //$NON-NLS-1$
                + Messages.getString("TransplantPage.allCheckBox.title.2")); //$NON-NLS-1$
        this.allCheckBox.setEnabled(false);

        SelectionListener allCheckBoxListener = new SelectionListener() {

            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }

            public void widgetSelected(SelectionEvent e) {
                all = allCheckBox.getSelection();
                validatePage();
            }
        };

        this.allCheckBox.addSelectionListener(allCheckBoxListener);
    }

    private void createBranchCheckBox(Group branchGroup) {
        this.branchCheckBox = SWTWidgetHelper.createCheckBox(branchGroup, Messages
                .getString("TransplantPage.branchCheckBox.title")); //$NON-NLS-1$

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
                branch = branchCheckBox.getSelection();
                if (branch) {
                    changesets.clear();
                    branchNameCombo.deselectAll();
                } else {
                    changesets.clear();
                    getUrlCombo().deselectAll();
                }
                validatePage();
            }

            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
        };

        this.branchCheckBox.addSelectionListener(branchCheckBoxListener);
    }

    private void addChangesetGroup(Composite composite) {
        // table of changesets
        Group changeSetGroup = SWTWidgetHelper.createGroup(
                composite,
                Messages.getString("TransplantPage.changesetGroup.title"), GridData.FILL_BOTH); //$NON-NLS-1$

        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.heightHint = 200;
        gridData.minimumHeight = 50;
        this.changesetTable = new ChangesetTable(changeSetGroup, SWT.MULTI
                | SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL
                        | SWT.H_SCROLL, project, false);
        this.changesetTable.setLayoutData(gridData);
        this.changesetTable.setEnabled(true);

        SelectionListener changeSetTableListener = new SelectionListener() {

            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }

            public void widgetSelected(SelectionEvent e) {
                ChangeSet[] changeSets = changesetTable.getSelections();
                ChangeSet last = changeSets[0];
                setErrorMessage(null);
                nodeIds.clear();
                for (ChangeSet changeSet : changeSets) {
                    if (last.getParents() != null) {
                        if (last.equals(changeSet)
                                || last.getParents()[0].endsWith(changeSet.getRevision().getChangeset())
                                || (last.getParents().length > 1 &&
                                        last.getParents()[1].endsWith(changeSet.getRevision().getChangeset()))) {
                            nodeIds.add(0, changeSet.getChangeset());
                        } else {
                            setErrorMessage(Messages
                                    .getString("TransplantPage.errorNotSequential")); //$NON-NLS-1$
                            setPageComplete(false);
                            break;
                        }
                    }
                    last = changeSet;
                }

                validatePage();
            }
        };

        changesetTable.addSelectionListener(changeSetTableListener);
        populateChangesetTable();
    }

    private void populateBranchNameCombo() {
        try {
            Branch[] branches = HgBranchClient.getBranches(project);
            for (Branch myBranch : branches) {
                this.branchNameCombo.add(myBranch.getName());
            }
        } catch (HgException e) {
            MercurialEclipsePlugin.showError(e);
            MercurialEclipsePlugin.logError(e);
        }
    }

    private void populateChangesetTable() {
        changesetTable.clearTable();
        changesetTable.setChangesets(changesets
                .toArray(new ChangeSet[changesets.size()]));
    }

    @Override
    public boolean finish(IProgressMonitor monitor) {
        return super.finish(monitor);
    }

    public IProject getProject() {
        return project;
    }

    public void setProject(IProject project) {
        this.project = project;
    }

    public boolean isBranch() {
        return this.branch;
    }

    public List<String> getNodeIds() {
        return nodeIds;
    }

    public String getBranchName() {
        return this.branchName;
    }

    public boolean isAll() {
        return this.all;
    }

    public SortedSet<ChangeSet> getChangesets() {
        return changesets;
    }

}
