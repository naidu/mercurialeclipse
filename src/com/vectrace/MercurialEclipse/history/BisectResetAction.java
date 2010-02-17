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
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.views.console.HgConsoleHolder;

/**
 * @author bastian
 *
 */
final class BisectResetAction extends Action {
	/**
	 *
	 */
	private final MercurialHistoryPage mercurialHistoryPage;

	/**
	 * @param mercurialHistoryPage
	 */
	BisectResetAction(MercurialHistoryPage mercurialHistoryPage) {
		super("Bisect: Reset and stop bisecting");
		this.setDescription("Bisect: Resets the working directory."
				+ "\nBisection stops therefore and can be started anew.");
		this.mercurialHistoryPage = mercurialHistoryPage;
	}

	@Override
	public void run() {
		if (isEnabled()) {
			try {
				final HgRoot root = MercurialTeamProvider
						.getHgRoot(this.mercurialHistoryPage.resource);

				// mark the chosen changeset as good
				new SafeWorkspaceJob("Bisect: Resetting repository " + root) {
					@Override
					protected IStatus runSafe(IProgressMonitor monitor) {
						monitor.beginTask("Calling Mercurial Bisect...", 2);
						try {
							final String result = HgBisectClient.reset(root);
							monitor.worked(1);
							MercurialStatusCache.getInstance().refreshStatus(root, monitor);
							mercurialHistoryPage.refresh();
							if (result.length() > 0) {
								HgConsoleHolder.getInstance().getConsole().messageLineReceived(
										result);
								Display.getDefault().syncExec(new Runnable() {
									public void run() {
										mercurialHistoryPage.clearSelection();
										MessageDialog.openInformation(Display.getDefault()
												.getActiveShell(), "Bisection result", result);
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
			if (HgBisectClient.isBisecting(root)) {
				return true;
			}
			return false;
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
		}
		return false;
	}
}