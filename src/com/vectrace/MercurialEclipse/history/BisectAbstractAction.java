/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.history;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeUiJob;
import com.vectrace.MercurialEclipse.SafeWorkspaceJob;
import com.vectrace.MercurialEclipse.commands.HgBisectClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;

/**
 * @author bastian
 *
 */
public abstract class BisectAbstractAction extends Action {

	/**
	 *
	 */
	MercurialHistoryPage mercurialHistoryPage;

	/**
	 *
	 */
	public BisectAbstractAction() {
		super();
	}

	/**
	 * @param text
	 */
	public BisectAbstractAction(String text) {
		super(text);
	}

	/**
	 * @param text
	 * @param image
	 */
	public BisectAbstractAction(String text, ImageDescriptor image) {
		super(text, image);
	}

	/**
	 * @param text
	 * @param style
	 */
	public BisectAbstractAction(String text, int style) {
		super(text, style);
	}

	/**
	 * @return
	 */
	private MercurialRevision getRevision() {
		MercurialRevision[] selectedRevisions = this.mercurialHistoryPage.getSelectedRevisions();
		if (selectedRevisions != null && selectedRevisions.length == 1) {
			return selectedRevisions[0];
		}
		ChangeSet cs = this.mercurialHistoryPage.getCurrentWorkdirChangeset();
		return (MercurialRevision) this.mercurialHistoryPage.getMercurialHistory().getFileRevision(
				cs.getChangeset());
	}

	abstract String callBisect(final HgRoot root, final ChangeSet cs) throws HgException;

	@Override
	public void run() {
		try {
			final HgRoot root = MercurialTeamProvider.getHgRoot(mercurialHistoryPage.resource);
			final MercurialRevision rev = getRevision();
			final ChangeSet cs = rev.getChangeSet();
			new SafeWorkspaceJob("Bisecting...") {
				@Override
				protected IStatus runSafe(IProgressMonitor monitor) {
					try {
						final String result = callBisect(root, cs);

						if (result.startsWith("The first bad revision is:")) {
							HgBisectClient.reset(root);
						}

						MercurialStatusCache.getInstance().refreshStatus(root, monitor);

						new SafeUiJob("Show Bisection result") {
							@Override
							protected IStatus runSafe(IProgressMonitor m) {
								if (result.length() > 0) {
									MessageDialog.openInformation(getDisplay().getActiveShell(),
											"Bisection result", result);
								}
								updateHistory(rev, root);
								return super.runSafe(m);
							}
						}.schedule(100);
					} catch (HgException e) {
						MercurialEclipsePlugin.showError(e);
						MercurialEclipsePlugin.logError(e);
					}

					return super.runSafe(monitor);
				}
			}.schedule();
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
			MercurialEclipsePlugin.showError(e);
		}
	}

	/**
	 * @param root
	 *
	 */
	protected void updateHistory(MercurialRevision rev, HgRoot root) {
		mercurialHistoryPage.clearSelection();
		mercurialHistoryPage.refresh();
	}
}