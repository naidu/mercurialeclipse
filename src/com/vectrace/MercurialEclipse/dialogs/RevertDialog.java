/*******************************************************************************
 * Copyright (c) 2007-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     StefanC - implementation
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.dialogs;

import static com.vectrace.MercurialEclipse.ui.SWTWidgetHelper.getFillGD;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.ui.ChangesetTable;
import com.vectrace.MercurialEclipse.ui.CommitFilesChooser;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;

public class RevertDialog extends TitleAreaDialog {

	private List<IResource> resources;
	private CommitFilesChooser selectFilesList;
	private List<IResource> selection;
	private List<IResource> untrackedSelection;
	private ChangesetTable csTable;
	private ChangeSet changeset;

	public static final String FILE_MODIFIED = Messages.getString("CommitDialog.modified"); //$NON-NLS-1$
	public static final String FILE_ADDED = Messages.getString("CommitDialog.added"); //$NON-NLS-1$
	public static final String FILE_REMOVED = Messages.getString("CommitDialog.removed"); //$NON-NLS-1$
	public static final String FILE_UNTRACKED = Messages.getString("CommitDialog.untracked"); //$NON-NLS-1$
	public static final String FILE_DELETED = Messages.getString("CommitDialog.deletedInWorkspace"); //$NON-NLS-1$

	/**
	 * Create the dialog
	 *
	 * @param parentShell
	 */
	public RevertDialog(Shell parentShell) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	/**
	 * Create contents of the dialog
	 *
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = SWTWidgetHelper.createComposite(parent, 1);
		GridData gd = getFillGD(400);
		container.setLayoutData(gd);
		super.createDialogArea(parent);
		createFilesList(container);
		csTable = new ChangesetTable(container, resources.get(0));
		csTable.setEnabled(false);
		setTitle(Messages.getString("RevertDialog.title")); //$NON-NLS-1$
		setMessage(Messages.getString("RevertDialog.message")); //$NON-NLS-1$
		return container;
	}

	private void createFilesList(Composite container) {
		selectFilesList = new CommitFilesChooser(container, true, resources, true, true);
		selectFilesList.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent event) {
				List<IResource> checkedResources = selectFilesList.getCheckedResources(FILE_ADDED, FILE_DELETED, FILE_MODIFIED, FILE_REMOVED);
				if (checkedResources.size()!=1) {
					csTable.setEnabled(false);
					csTable.clearTable();
				} else {
					csTable.setResource(checkedResources.get(0));
					csTable.setEnabled(true);
				}
			}
		});
	}

	public void setFiles(List<IResource> resources) {
		this.resources = resources;

	}

	@Override
	protected void okPressed() {
		selection = selectFilesList.getCheckedResources(FILE_ADDED, FILE_DELETED, FILE_MODIFIED, FILE_REMOVED);
		untrackedSelection = selectFilesList.getCheckedResources(FILE_UNTRACKED);
		changeset = csTable.getSelection();

		if(!untrackedSelection.isEmpty()){
			boolean confirm = MessageDialog.openConfirm(getShell(), "Please confirm delete",
					"You have selected to revert untracked files." +
					"\nThis files will be now deleted.\n\nContinue?");
			if(!confirm){
				return;
			}
		}
		super.okPressed();

	}

	public void setFiles(IResource[] commitResources) {
		setFiles(Arrays.asList(commitResources));
	}

	public List<IResource> getSelectionForHgRevert() {
		return selection;
	}

	public List<IResource> getUntrackedSelection() {
		return untrackedSelection;
	}

	/**
	 * @return the csTable
	 */
	public ChangesetTable getCsTable() {
		return csTable;
	}

	/**
	 * @return
	 */
	public ChangeSet getChangeset() {
		return changeset;
	}
}
