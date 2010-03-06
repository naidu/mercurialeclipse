/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian Doetsch	implementation
 * Adam Berkes (Intland) - various fixes
 * Andrei Loskutov (Intland) - bugfixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

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
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.cache.IncomingChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.ui.ChangesetTable;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;

/**
 * @author bastian
 *
 */
public class TransplantPage extends ConfigurationWizardMainPage {

	private final List<String> nodeIds;
	private boolean branch;
	private String branchName;
	private boolean all;
	private ChangesetTable changesetTable;
	private Button branchCheckBox;
	private Combo branchNameCombo;
	private Button allCheckBox;
	private final SortedSet<ChangeSet> changesets;

	public TransplantPage(String pageName, String title,
			ImageDescriptor titleImage, HgRoot hgRoot) {
		super(pageName, title, titleImage);
		setHgRoot(hgRoot);
		nodeIds = new ArrayList<String>();
		changesets = new TreeSet<ChangeSet>(Collections.reverseOrder());
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		Composite composite = (Composite) getControl();

		ModifyListener urlModifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				IHgRepositoryLocation repoLocation;
				try {
					repoLocation = MercurialEclipsePlugin.getRepoManager()
							.getRepoLocation(getUrlText());
				} catch (HgException e1) {
					// bad URI?
					setErrorMessage(e1.getMessage());
					return;
				}
				setErrorMessage(null);
				try {
					Set<ChangeSet> changes = IncomingChangesetCache
							.getInstance().getChangeSets(getHgRoot(), repoLocation, null);
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
							.getOrFetchChangeSetsByBranch(getHgRoot(), branchName));
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
		allCheckBox = SWTWidgetHelper.createCheckBox(branchGroup, Messages
				.getString("TransplantPage.allCheckBox.title")); //$NON-NLS-1$
		allCheckBox.setEnabled(false);

		SelectionListener allCheckBoxListener = new SelectionListener() {

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			public void widgetSelected(SelectionEvent e) {
				all = allCheckBox.getSelection();
				validatePage();
			}
		};

		allCheckBox.addSelectionListener(allCheckBoxListener);
	}

	private void createBranchCheckBox(Group branchGroup) {
		branchCheckBox = SWTWidgetHelper.createCheckBox(branchGroup, Messages
				.getString("TransplantPage.branchCheckBox.title")); //$NON-NLS-1$

		SelectionListener branchCheckBoxListener = new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				branch = branchCheckBox.getSelection();
				getUrlCombo().setEnabled(!branch);
				getUserCombo().setEnabled(!branch);
				passwordText.setEnabled(!branch);
				allCheckBox.setEnabled(branch);
				branchNameCombo.setEnabled(branch);
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

		branchCheckBox.addSelectionListener(branchCheckBoxListener);
	}

	private void addChangesetGroup(Composite composite) {
		// table of changesets
		Group changeSetGroup = SWTWidgetHelper.createGroup(
				composite,
				Messages.getString("TransplantPage.changesetGroup.title"), GridData.FILL_BOTH); //$NON-NLS-1$

		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.heightHint = 200;
		gridData.minimumHeight = 50;
		changesetTable = new ChangesetTable(changeSetGroup, SWT.MULTI
				| SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL
						| SWT.H_SCROLL, getHgRoot(), false);
		changesetTable.setLayoutData(gridData);
		changesetTable.setEnabled(true);

		SelectionListener changeSetTableListener = new SelectionListener() {

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			public void widgetSelected(SelectionEvent e) {
				setErrorMessage(null);
				nodeIds.clear();
				ChangeSet[] changeSets = changesetTable.getSelections();
				if(changeSets == null || changeSets.length == 0){
					return;
				}
				ChangeSet last = changeSets[0];
				for (ChangeSet changeSet : changeSets) {
					String[] parents = last.getParents();
					if (parents != null && parents.length > 0) {
						String changesetId = changeSet.getRevision().getChangeset();
						if (last.equals(changeSet) || parents[0].endsWith(changesetId)
								|| (parents.length > 1 && parents[1].endsWith(changesetId))) {
							nodeIds.add(0, changeSet.getChangeset());
						} else {
							setErrorMessage(Messages.getString("TransplantPage.errorNotSequential")); //$NON-NLS-1$
							setPageComplete(false);
							return;
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
			Branch[] branches = HgBranchClient.getBranches(getHgRoot());
			for (Branch myBranch : branches) {
				branchNameCombo.add(myBranch.getName());
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
