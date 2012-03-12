/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre - implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import com.vectrace.MercurialEclipse.HgRevision;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;

public class UpdateHandler extends RunnableHandler {

	private String revision;
	private boolean cleanEnabled;
	private boolean handleCrossBranches = true;

	public UpdateHandler() {

	}

	public UpdateHandler(boolean handleCrossBranches) {
		this.handleCrossBranches = handleCrossBranches;
	}

	/**
	 * @param hgRoot
	 *            non null
	 * @throws HgException
	 */
	@Override
	public void run(HgRoot hgRoot) throws HgException {
		UpdateJob job = new UpdateJob(revision, cleanEnabled, hgRoot, handleCrossBranches);

		if (cleanEnabled && !job.confirmDataLoss(getShell())) {
			return;
		}

		job.schedule();
	}

	/**
	 * @param revision
	 *            the revision to use for the '-r' option, can be null
	 */
	public void setRevision(String revision) {
		this.revision = revision;
	}

	public void setRevision(HgRevision rev) {
		this.revision = rev.getNode();
	}

	/**
	 * @param cleanEnabled
	 *            true to add '-C' option
	 */
	public void setCleanEnabled(boolean cleanEnabled) {
		this.cleanEnabled = cleanEnabled;
	}
}
