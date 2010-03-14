/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre - implementation
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;

public class UpdateHandler extends RootHandler {

	private String revision;
	private boolean cleanEnabled = false;

	/**
	 * @param hgRoot
	 *            non null
	 * @throws HgException
	 */
	@Override
	public void run(HgRoot hgRoot) throws HgException {
		boolean dirty = HgStatusClient.isDirty(hgRoot);
		if (dirty && cleanEnabled) {
			final String message = "Your working directory contains uncommited changes.\nDo you really want to continue and discard them?";
			final boolean[] result = new boolean[1];
			if (Display.getCurrent() == null) {
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						result[0] = MessageDialog.openQuestion(getShell(),
								"Uncommited Changes", message);
					}
				});
			} else {
				result[0] = MessageDialog.openQuestion(getShell(),
						"Uncommited Changes", message);
			}
			if (!result[0]) {
				return;
			}
		}
		new UpdateJob(revision, cleanEnabled, hgRoot).schedule();
	}

	/**
	 * @param revision
	 *            the revision to use for the '-r' option, can be null
	 */
	public void setRevision(String revision) {
		this.revision = revision;
	}

	/**
	 * @param cleanEnabled
	 *            true to add '-C' option
	 */
	public void setCleanEnabled(boolean cleanEnabled) {
		this.cleanEnabled = cleanEnabled;
	}
}
