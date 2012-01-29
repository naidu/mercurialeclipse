/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Ilya Ivanov (Intland) - implementation
 *     Andrei Loskutov        - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.dialogs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgRevertClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.cache.RefreshRootJob;
import com.vectrace.MercurialEclipse.wizards.TransplantWizard;

/**
 * Dialog shows list of rejected patches. Allows user to revert changes and reopen Transplant wizard
 */
public class TransplantRejectsDialog extends RejectsDialog {

	public TransplantRejectsDialog(Shell parentShell, HgRoot hgRoot, String message)
			throws HgException {
		super(parentShell, hgRoot, message, "TransplantRejectsDialog.title",
				"TransplantRejectsDialog.transplantFailed");

		setBlockOnOpen(false);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CANCEL_ID, Messages
				.getString("TransplantRejectsDialog.tryAnotherTransplant"), false);
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
	}

	@Override
	protected void cancelPressed() {
		final Display display = getShell().getDisplay();

		new Job("Reverting transplant") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Reverting transplant", 1);

				try {
					HgRevertClient.performRevertAll(new NullProgressMonitor(), hgRoot);
					new RefreshRootJob(hgRoot, RefreshRootJob.LOCAL_AND_OUTGOING).schedule();
				} catch (HgException e) {
					MercurialEclipsePlugin.logError(e);
				}

				display.syncExec(new Runnable() {
					public void run() {
						TransplantWizard transplantWizard = new TransplantWizard(hgRoot);
						WizardDialog transplantWizardDialog = new WizardDialog(getParentShell(),
								transplantWizard);
						transplantWizardDialog.setBlockOnOpen(false);
						transplantWizardDialog.open();
					}
				});

				monitor.done();
				return Status.OK_STATUS;
			}
		}.schedule();

		close();
	}
}
