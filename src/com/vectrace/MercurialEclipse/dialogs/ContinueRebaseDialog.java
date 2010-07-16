/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * John Peberdy	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.dialogs;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.vectrace.MercurialEclipse.commands.extensions.HgRebaseClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.cache.RefreshRootJob;
import com.vectrace.MercurialEclipse.team.cache.RefreshWorkspaceStatusJob;
import com.vectrace.MercurialEclipse.ui.CommitFilesChooser;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;

/**
 * Dialog for showing which files will be committed for a rebase.
 * <p>
 * Future: For --collapse rebases allow the user to set commit message.
 */
public class ContinueRebaseDialog extends CommitDialog {

	public ContinueRebaseDialog(Shell shell, HgRoot hgRoot) {
		super(shell, hgRoot, null);

		options.defaultCommitMessage = "";
		options.showAmend = false;
		options.showCloseBranch = false;
		options.showRevert = false;
		options.showCommitMessage = false;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Control control = super.createDialogArea(parent);
		getShell().setText(Messages.getString("RebaseDialog.window.title")); //$NON-NLS-1$
		setTitle(Messages.getString("RebaseDialog.title")); // ";
		setMessage(Messages.getString("RebaseDialog.message"));
		return control;
	}

	@Override
	protected CommitFilesChooser createFilesList(Composite container) {
		SWTWidgetHelper.createLabel(container, Messages.getString("RebaseDialog.fileList"));
		return new CommitFilesChooser(root, container, false, false, false, false);
	}

	@Override
	protected String performCommit(String messageToCommit, boolean closeBranch)
			throws CoreException {
		return continueRebase(messageToCommit);
	}

	@Override
	protected String performCommit(String messageToCommit, boolean closeBranch, ChangeSet cs)
			throws CoreException {
		return continueRebase(messageToCommit);
	}

	private String continueRebase(String messageToCommit) throws HgException {
		try {
			return HgRebaseClient.continueRebase(root, getUser());
		} finally {
			new RefreshWorkspaceStatusJob(root, RefreshRootJob.ALL).schedule();
		}
	}
}
