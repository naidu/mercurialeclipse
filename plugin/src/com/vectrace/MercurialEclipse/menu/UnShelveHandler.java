/*******************************************************************************
 * Copyright (c) 2005-2008 Bastian Doetsch and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchPart;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeWorkspaceJob;
import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.dialogs.RejectsDialog;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.operations.UnShelveOperation;

/**
 * @author bastian
 */
public class UnShelveHandler extends RootHandler {

	@Override
	protected void run(final HgRoot hgRoot) {

		final UnshelveJob job = new UnshelveJob(Messages.getString("UnShelveHandler.Unshelving"),
				hgRoot);

		try {
			if (HgStatusClient.isDirty(hgRoot)) {
				getShell().getDisplay().asyncExec(new Runnable() {
					public void run() {
						if (MessageDialog.openQuestion(getShell(),
								"Outstanding uncommitted changes!",
								"There are outstanding uncommitted changes. Force unshelve?")) {
							job.setForce(true);
							job.schedule();
						}
					}
				});
				return;
			}
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
		}

		job.schedule();
	}

	private final class UnshelveJob extends SafeWorkspaceJob {

		private final HgRoot hgRoot;

		private boolean force;

		private UnshelveJob(String name, HgRoot hgRoot) {
			super(name);
			this.hgRoot = hgRoot;
		}

		public void setForce(boolean force) {
			this.force = force;
		}

		@Override
		protected IStatus runSafe(IProgressMonitor monitor) {
			try {
				final UnShelveOperation op = new UnShelveOperation((IWorkbenchPart) null, hgRoot, force);
				op.run(monitor);

				if (op.isConflict()) {
					getShell().getDisplay().asyncExec(new Runnable() {
						public void run() {
							try {
								new RejectsDialog(getShell(), hgRoot, op.getResult(),
										"UnshelveRejectsDialog.title",
										"UnshelveRejectsDialog.conflict").open();
							} catch (HgException e) {
								// Fallback if couldn't parse rejects
								MessageDialog.openInformation(getShell(), Messages
										.getString("UnShelveHandler.Unshelving"), Messages
										.getString("UnShelveHandler.conflict")
										+ "\n" + op.getResult());
								MercurialEclipsePlugin.logError(e);
							}
						}
					});
				}

				return super.runSafe(monitor);
			} catch (InvocationTargetException e) {
				return new Status(IStatus.WARNING, MercurialEclipsePlugin.ID, 0, e
						.getLocalizedMessage(), e);
			} catch (InterruptedException e) {
				return new Status(IStatus.INFO, MercurialEclipsePlugin.ID, 0, e
						.getLocalizedMessage(), e);
			}
		}
	}
}
