/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian Doetsch	implementation
 *     Andrei Loskutov - bug fixes
 *     Ilya Ivanov (Intland) - modifications
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

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
import org.eclipse.ui.PartInitException;

import com.aragost.javahg.merge.BackoutConflictResolvingContext;
import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgBackoutClient;
import com.vectrace.MercurialEclipse.commands.HgParentClient;
import com.vectrace.MercurialEclipse.commands.HgResolveClient;
import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.menu.CommitMergeHandler;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.storage.HgCommitMessageManager;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.team.cache.RefreshRootJob;
import com.vectrace.MercurialEclipse.team.cache.RefreshWorkspaceStatusJob;
import com.vectrace.MercurialEclipse.ui.ChangesetTable;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;
import com.vectrace.MercurialEclipse.views.MergeView;

/**
 * @author bastian
 */
public class BackoutWizardPage extends HgWizardPage {

	private ChangesetTable changesetTable;
	private Text messageTextField;
	private Button mergeCheckBox;
	private final HgRoot hgRoot;
	private Text userTextField;
	private ChangeSet selectedChangeSet;

	public BackoutWizardPage(String pageName, String title, ImageDescriptor image,
			HgRoot hgRoot, ChangeSet selectedChangeSet) {

		super(pageName, title, image);
		this.hgRoot = hgRoot;
		this.selectedChangeSet = selectedChangeSet;
	}

	public void createControl(Composite parent) {
		Composite composite = SWTWidgetHelper.createComposite(parent, 2);

		// list view of changesets
		Group changeSetGroup = SWTWidgetHelper.createGroup(composite, Messages
				.getString("BackoutWizardPage.changeSetGroup.title"), GridData.FILL_BOTH); //$NON-NLS-1$

		changesetTable = new ChangesetTable(changeSetGroup, hgRoot);
		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.heightHint = 200;
		gridData.minimumHeight = 50;
		changesetTable.setLayoutData(gridData);

		changesetTable.highlightParents(HgParentClient.getParentIndexes(hgRoot));

		SelectionListener listener = new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				onChangesetSelected(changesetTable.getSelection());
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		};

		changesetTable.setEnabled(true);

		// now the options
		Group optionGroup = SWTWidgetHelper.createGroup(composite, Messages
				.getString("BackoutWizardPage.optionGroup.title")); //$NON-NLS-1$

		SWTWidgetHelper.createLabel(optionGroup, Messages
				.getString("BackoutWizardPage.userLabel.text")); //$NON-NLS-1$
		userTextField = SWTWidgetHelper.createTextField(optionGroup);
		userTextField.setText(HgCommitMessageManager.getDefaultCommitName(hgRoot));

		SWTWidgetHelper.createLabel(optionGroup, Messages
				.getString("BackoutWizardPage.commitLabel.text")); //$NON-NLS-1$
		messageTextField = SWTWidgetHelper.createTextField(optionGroup);

		// --merge merge with old dirstate parent after backout
		mergeCheckBox = SWTWidgetHelper.createCheckBox(optionGroup,
				Messages.getString("BackoutWizardPage.mergeCheckBox.text")); //$NON-NLS-1$
		mergeCheckBox.setSelection(true);

		// Hack because initial selection event is not fired
		{
			if (selectedChangeSet == null) {
				selectedChangeSet = changesetTable.getStrategy().getChangeSet(0);
			}
			changesetTable.setSelection(selectedChangeSet);
			onChangesetSelected(selectedChangeSet);
		}
		changesetTable.addSelectionListener(listener);

		setControl(composite);
		setPageComplete(true);
	}

	protected void onChangesetSelected(final ChangeSet backoutRevision) {
		if (backoutRevision != null) {
			// Update commit message
			getShell().getDisplay().asyncExec(new Runnable() {
				public void run() {
					messageTextField.setText(Messages.getString(
					"BackoutWizardPage.defaultCommitMessage") //$NON-NLS-1$
					+ " " + backoutRevision.toString()); //$NON-NLS-1$
					setPageComplete(true);
				}
			});
		} else {
			setPageComplete(false);
		}
	}

	@Override
	public void setPageComplete(boolean complete) {
		if(complete){
			complete = validate();
		}
		super.setPageComplete(complete);
	}

	private boolean validate() {
		ChangeSet backoutRevision = changesetTable.getSelection();

		mergeCheckBox.setEnabled(backoutRevision != null && !backoutRevision.isCurrent());
		setErrorMessage(null);

		if (backoutRevision == null) {
			// Do nothing
		} else if (messageTextField.getText().length() == 0) {
			setErrorMessage("Please enter a commit message");
		} else if(HgStatusClient.isDirty(hgRoot)){
			setErrorMessage("Outstanding uncommitted changes! Backout is not possible.");
		} else {
			// All ok
			return true;
		}

		return false;
	}

	/**
	 * @see com.vectrace.MercurialEclipse.wizards.HgWizardPage#finish(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public boolean finish(IProgressMonitor monitor) {
		if (!validate()) {
			return false;
		}

		String msg = messageTextField.getText();
		ChangeSet backoutRevision = changesetTable.getSelection();
		boolean merge = mergeCheckBox.getSelection() && !backoutRevision.isCurrent();

		try {
			BackoutConflictResolvingContext ctx = HgBackoutClient.backout(hgRoot, backoutRevision,
					merge, msg, userTextField.getText());

			if (HgResolveClient.autoResolve(hgRoot, ctx)) {
				// Commit the merge / update
				if (HgStatusClient.isDirty(hgRoot)) {
					MercurialStatusCache.getInstance().refreshStatus(hgRoot, monitor);
					new CommitMergeHandler().run(hgRoot);
				}
				// Else no conflict and Mercurial auto-committed
			} else {
				if (merge) {
					try {
						MergeView.showMergeConflict(hgRoot, getShell());
					} catch (PartInitException e1) {
						MercurialEclipsePlugin.logError(e1);
					}
				} else {
					MessageDialog
							.openInformation(null, "Unresolved conflicts",
									"You have unresolved conflicts. Use Synchronize View to edit conflicts");
				}
			}
		} catch (HgException e) {
			MessageDialog.openError(getShell(), Messages
					.getString("BackoutWizardPage.backoutError"), e //$NON-NLS-1$
					.getMessage());
			MercurialEclipsePlugin.logError(e);

			return false;
		} finally {
			new RefreshWorkspaceStatusJob(hgRoot, RefreshRootJob.LOCAL_AND_OUTGOING).schedule();
		}
		return true;
	}

}
