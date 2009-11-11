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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import com.vectrace.MercurialEclipse.menu.CommitMergeHandler;
import com.vectrace.MercurialEclipse.ui.CommitFilesChooser;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;

/**
 * @author Andrei
 */
public class MergeDialog extends CommitDialog {
	private final IProject mergeProject;

	public MergeDialog(Shell shell, IProject mergeProject, String defaultCommitMessage) {
		super(shell, null);
		Assert.isNotNull(mergeProject);
		this.mergeProject = mergeProject;
		setDefaultCommitMessage(defaultCommitMessage);
	}

	@Override
	protected void createFilesList(Composite container) {
		SWTWidgetHelper.createLabel(container, Messages.getString("CommitDialog.selectFiles")); //$NON-NLS-1$
		List<IResource> resources = new ArrayList<IResource>();
		resources.add(mergeProject);
		commitFilesList = new CommitFilesChooser(container, false, resources, true, true);
	}

	@Override
	protected void createRevertCheckBox(Composite container) {
		// does nothing
	}

	@Override
	protected void performCommit(String messageToCommit) throws CoreException {
		CommitMergeHandler.commitMerge(mergeProject.getProject(), messageToCommit);
	}

	@Override
	protected String getInitialCommitUserName() {
		return getDefaultCommitName(mergeProject);
	}
}
