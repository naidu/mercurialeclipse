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

import com.aragost.javahg.ext.mq.StripCommand;
import com.aragost.javahg.ext.mq.flags.StripCommandFlags;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
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

	public static String stripCurrent(final HgRoot hgRoot, boolean keep, boolean backup, boolean force) {
		return strip(hgRoot, keep, backup, force, ".");
	}

	/**
	 * strip a revision and all later revs on the same branch
	 */
	public static String strip(final HgRoot hgRoot, boolean keep, boolean backup, boolean force, ChangeSet changeset) {
		return strip(hgRoot, keep, backup, force, changeset.getNode());
	}

	private static String strip(final HgRoot hgRoot, boolean keep, boolean backup, boolean force, String changeset) {
		StripCommand command = StripCommandFlags.on(hgRoot.getRepository());

		if (keep) {
			command.keep();
		}
		if (!backup) {
			command.noBackup();
		}
		if (force) {
			command.force();
		}
		command.rev(changeset);

		// Future: should not care about the result
		String result;

		try {
			result = command.execute();
		} finally {
			LocalChangesetCache.getInstance().clear(hgRoot);
			new RefreshWorkspaceStatusJob(hgRoot, RefreshRootJob.ALL).schedule();
		}
		return result;

	}
}
