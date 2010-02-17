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
import org.eclipse.swt.widgets.Display;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeUiJob;
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
	ChangeSet getRevision() {
		MercurialRevision[] selectedRevisions = this.mercurialHistoryPage.getSelectedRevisions();

		// the changeset can be a selected revision or the working directory
		final ChangeSet changeSet;
		if (selectedRevisions != null && selectedRevisions.length == 1) {
			changeSet = selectedRevisions[0].getChangeSet();
		} else {
			changeSet = mercurialHistoryPage.getCurrentWorkdirChangeset();
		}
		return changeSet;
	}

	abstract String callBisect(final HgRoot root, final ChangeSet cs) throws HgException;

	@Override
	public void run() {
		try {
			final HgRoot root = MercurialTeamProvider.getHgRoot(this.mercurialHistoryPage.resource);

			final ChangeSet changeSet = getRevision();

			// mark the chosen changeset
			final String result = callBisect(root, changeSet);

			if (result.startsWith("The first bad revision is:")) {
				HgBisectClient.reset(root);
			}
			MercurialStatusCache.getInstance().refreshStatus(root, null);
			mercurialHistoryPage.refresh();
			if (result.length() > 0) {
				new SafeUiJob(mercurialHistoryPage.getControl().getDisplay(),
						"Show Bisection result") {
					@Override
					protected IStatus runSafe(IProgressMonitor m) {
						MessageDialog.openInformation(Display.getDefault().getActiveShell(),
								"Bisection result", result);
						return super.runSafe(m);
					}
				}.schedule();
			}
		} catch (HgException e) {
			MercurialEclipsePlugin.showError(e);
			MercurialEclipsePlugin.logError(e);
		}
	}

}