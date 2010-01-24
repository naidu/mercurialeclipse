/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov (Intland) - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.dialogs;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.vectrace.MercurialEclipse.menu.CommitMergeHandler;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.storage.HgCommitMessageManager;
import com.vectrace.MercurialEclipse.ui.CommitFilesChooser;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;

/**
 * @author Andrei
 */
public class MergeDialog extends CommitDialog {
	private final HgRoot hgRoot;

	public MergeDialog(Shell shell, HgRoot hgRoot, String defaultCommitMessage) {
		super(shell, null);
		Assert.isNotNull(hgRoot);
		this.hgRoot = hgRoot;
		setDefaultCommitMessage(defaultCommitMessage);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Control control = super.createDialogArea(parent);

		getShell().setText(Messages.getString("MergeDialog.window.title")); //$NON-NLS-1$
		setTitle(Messages.getString("MergeDialog.title")); //$NON-NLS-1$";
		setMessage(Messages.getString("MergeDialog.message")); //$NON-NLS-1$";
		return control;
	}

	@Override
	protected void createFilesList(Composite container) {
		SWTWidgetHelper.createLabel(container, Messages.getString("CommitDialog.selectFiles")); //$NON-NLS-1$
		commitFilesList = new CommitFilesChooser(hgRoot, container, false, true, true, false);
	}

	@Override
	protected void createRevertCheckBox(Composite container) {
		// does nothing
	}

	@Override
	protected void createCloseBranchCheckBox(Composite container) {
		// don't create it as we don't want it in merge dialog
	}

	@Override
	protected String performCommit(String messageToCommit, boolean closeBranch) throws CoreException {
		return CommitMergeHandler.commitMerge(hgRoot, messageToCommit);
	}

	@Override
	protected String getInitialCommitUserName() {
		return HgCommitMessageManager.getDefaultCommitName(hgRoot);
	}
}
