/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	   Bastian	implementation
 *     Andrei Loskutov - bug fixes
 *     Adam Berkes (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import com.aragost.javahg.commands.BackoutCommand;
import com.aragost.javahg.commands.flags.BackoutCommandFlags;
import com.aragost.javahg.merge.BackoutConflictResolvingContext;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.storage.HgCommitMessageManager;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

/**
 * @author bastian
 */
public class HgBackoutClient extends AbstractClient {

	/**
	 * Backout of a changeset
	 *
	 * @param hgRoot
	 *            the project
	 * @param backoutRevision
	 *            revision to backout
	 * @param merge
	 *            flag if merge with a parent is wanted
	 * @param msg
	 *            commit message
	 */
	public static BackoutConflictResolvingContext backout(final HgRoot hgRoot, ChangeSet backoutRevision,
			boolean merge, String msg, String user) {

		user = MercurialUtilities.getDefaultUserName(user);

		BackoutCommand command = BackoutCommandFlags.on(hgRoot.getRepository()).rev(backoutRevision.getNode()).message(msg).user(user);

		if (merge) {
			command.merge();
		}

		BackoutConflictResolvingContext ctx = command.execute();

		HgCommitMessageManager.updateDefaultCommitName(hgRoot, user);

		return ctx;
	}
}
