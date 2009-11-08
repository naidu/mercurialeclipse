/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Stefan Groschupf          - logError
 *     Stefan C                  - Code cleanup
 *     Bastian Doetsch			 - saving repository to projec specific repos.
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import java.io.File;
import java.util.SortedSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Button;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgClients;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

public class PullRepoWizard extends HgWizard {

	private boolean doUpdate;
	private PullPage pullPage;
	private IncomingPage incomingPage;
	private final IProject resource;
	private HgRepositoryLocation repo;
	private boolean doCleanUpdate;

	public PullRepoWizard(IProject resource) {
		super(Messages.getString("PullRepoWizard.title")); //$NON-NLS-1$
		this.resource = resource;
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		pullPage = new PullPage(Messages
				.getString("PullRepoWizard.pullPage.name"), //$NON-NLS-1$
				Messages.getString("PullRepoWizard.pullPage.title"), //$NON-NLS-1$
				Messages.getString("PullRepoWizard.pullPage.description"), //$NON-NLS-1$
				resource.getProject(), null);

		initPage(pullPage.getDescription(), pullPage);
		addPage(pullPage);

		incomingPage = new IncomingPage(Messages
				.getString("PullRepoWizard.incomingPage.name")); //$NON-NLS-1$
		initPage(incomingPage.getDescription(), incomingPage);
		addPage(incomingPage);
	}

	@Override
	public boolean performFinish() {

		// If there is no project set the wizard can't finish
		if (resource.getProject().getLocation() == null) {
			return false;
		}

		pullPage.finish(new NullProgressMonitor());
		incomingPage.finish(new NullProgressMonitor());
		repo = getLocation();

		doUpdate = pullPage.getUpdateCheckBox().getSelection();
		doCleanUpdate = pullPage.getCleanUpdateCheckBox().getSelection();
		boolean force = pullPage.getForceCheckBox().getSelection();

		ChangeSet cs = null;
		if (incomingPage.getRevisionCheckBox().getSelection()) {
			cs = incomingPage.getRevision();
		}

		boolean timeout = pullPage.getTimeoutCheckBox().getSelection();
		boolean merge = pullPage.getMergeCheckBox().getSelection();
		boolean rebase = false;
		Button rebase_button = pullPage.getRebaseCheckBox();
		if (rebase_button != null ) {
			rebase = rebase_button.getSelection();
		}
		boolean showCommitDialog = pullPage.getCommitDialogCheckBox().getSelection();
		boolean svn = false;
		if (pullPage.isShowSvn()) {
			svn = pullPage.getSvnCheckBox().getSelection();
		}
		boolean forest = false;
		File snapFile = null;
		if (pullPage.isShowForest()) {
			forest = pullPage.getForestCheckBox().getSelection();
			String snapFileText = pullPage.getSnapFileCombo().getText();
			if (snapFileText.length() > 0) {
				snapFile = new File(snapFileText);
			}
		}

		File bundleFile = null;
		SortedSet<ChangeSet> changesets = incomingPage.getChangesets();
		if (changesets != null && changesets.size() > 0) {
			bundleFile = changesets.first().getBundleFile();
		}

		PullOperation pullOperation = new PullOperation(getContainer(),
				doUpdate, doCleanUpdate, resource, force, repo, cs, timeout, merge,
				showCommitDialog, bundleFile, forest, snapFile, rebase, svn);

		try {
			getContainer().run(true, false, pullOperation);

			String output = pullOperation.getOutput();

			if (output.length() != 0) {
				HgClients.getConsole().printMessage(output, null);
			}

		} catch (Exception e) {
			Throwable error = e.getCause() == null? e : e.getCause();
			MercurialEclipsePlugin.logError(error);
			MercurialEclipsePlugin.showError(error);
			return false;
		}

		return true;
	}

	private HgRepositoryLocation getLocation() {
		try {
			return MercurialEclipsePlugin.getRepoManager()
					.fromProperties(resource.getProject(), pullPage.getProperties());
		} catch (Exception e) {
			MessageDialog.openInformation(getShell(), Messages
					.getString("PullRepoWizard.malformedURL"), e.getMessage()); //$NON-NLS-1$
			MercurialEclipsePlugin.logInfo(e.getMessage(), e);
			return null;
		}
	}
}
