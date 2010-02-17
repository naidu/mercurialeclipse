/*******************************************************************************
 * Copyright (c) 2010 Bastian Doetsch and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch - Implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.history;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeWorkspaceJob;
import com.vectrace.MercurialEclipse.commands.HgBisectClient;
import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.views.console.HgConsoleHolder;

/**
 * @author bastian
 *
 */
final class BisectMarkBadAction extends Action {
	/**
	 *
	 */
	private final MercurialHistoryPage mercurialHistoryPage;

	/**
	 * @param mercurialHistoryPage
	 */
	BisectMarkBadAction(MercurialHistoryPage mercurialHistoryPage) {
		super("Bisect: Mark selection or working directory as bad");
		this.setDescription("Bisect:Marks the selected changeset as erroneous."
				+ "\nIf bisect hadn't been started before, starts bisect.");
		this.mercurialHistoryPage = mercurialHistoryPage;
	}

	@Override
	public void run() {
		if (isEnabled()) {
			try {
				final HgRoot root = MercurialTeamProvider
						.getHgRoot(this.mercurialHistoryPage.resource);

				MercurialRevision[] selectedRevisions = this.mercurialHistoryPage
						.getSelectedRevisions();

				// the changeset can be a selected revision or the working directory
				final ChangeSet changeSet;
				if (selectedRevisions != null && selectedRevisions.length == 1) {
					changeSet = selectedRevisions[0].getChangeSet();
				} else {
					changeSet = mercurialHistoryPage.getCurrentWorkdirChangeset();
				}

				// mark the chosen changeset as good
				new SafeWorkspaceJob("Bisect: Marking Changeset " + changeSet.toString()
						+ " as bad.") {
					@Override
					protected IStatus runSafe(IProgressMonitor monitor) {
						monitor.beginTask("Calling Mercurial Bisect...", 2);
						try {
							final String result = HgBisectClient.markBad(root, changeSet);
							if (result.startsWith("The first bad revision is:")) {
								HgBisectClient.reset(root);
							}
							monitor.worked(1);
							MercurialStatusCache.getInstance().refreshStatus(root, monitor);
							mercurialHistoryPage.refresh();
							if (result.length() > 0) {
								HgConsoleHolder.getInstance().getConsole().messageLineReceived(
										result);
								Display.getDefault().syncExec(new Runnable() {
									public void run() {
										MessageDialog.openInformation(Display.getDefault()
												.getActiveShell(), "Bisecting result", result);
									}
								});
							}
						} catch (HgException e) {
							MercurialEclipsePlugin.showError(e);
							MercurialEclipsePlugin.logError(e);
						}
						monitor.worked(1);
						monitor.done();
						return super.runSafe(monitor);
					}
				}.schedule();
			} catch (HgException e) {
				MercurialEclipsePlugin.showError(e);
				MercurialEclipsePlugin.logError(e);
			}
		}
	}

	@Override
	public boolean isEnabled() {
		try {
			final HgRoot root = MercurialTeamProvider.getHgRoot(this.mercurialHistoryPage.resource);
			// no selection or dirty working dir -> disable
			if (HgStatusClient.isDirty(root)) {
				return false;
			}
			return true;
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
		}
		return false;
	}
}