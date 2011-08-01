/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian Doetsch	implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands.extensions;

import com.vectrace.MercurialEclipse.commands.HgCommand;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.cache.RefreshRootJob;
import com.vectrace.MercurialEclipse.team.cache.RefreshWorkspaceStatusJob;

/**
 * Calls hg strip
 *
 * @author bastian
 */
public final class HgStripClient {

	private HgStripClient() {
		// hide constructor of utility class.
	}

	public static String stripCurrent(final HgRoot hgRoot, boolean keep, boolean backup, boolean force)
		throws HgException {
		return strip(hgRoot, keep, backup, force, ".");
	}

	/**
	 * strip a revision and all later revs on the same branch
	 */
	public static String strip(final HgRoot hgRoot, boolean keep, boolean backup, boolean force, ChangeSet changeset)
			throws HgException {
		return strip(hgRoot, keep, backup, force, changeset.getChangeset());
	}

	private static String strip(final HgRoot hgRoot, boolean keep, boolean backup, boolean force, String changeset)
	throws HgException {
		HgCommand command = new HgCommand("strip", "Stripping revision " + changeset, hgRoot, true); //$NON-NLS-1$

		command.setUsePreferenceTimeout(MercurialPreferenceConstants.COMMIT_TIMEOUT);

		command.addOptions("--config", "extensions.hgext.mq="); //$NON-NLS-1$ //$NON-NLS-2$

		if (keep) {
			command.addOptions("--keep"); //$NON-NLS-1$
		}
		if (!backup) {
			command.addOptions("--no-backup"); //$NON-NLS-1$
		}
		if (force) {
			command.addOptions("-f"); //$NON-NLS-1$
		}
		command.addOptions(changeset);
		String result = command.executeToString();
		new RefreshWorkspaceStatusJob(hgRoot, RefreshRootJob.ALL).schedule();
		return result;
	}
}
