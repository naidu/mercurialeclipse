/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Andrei Loskutov - bug fixes
 * Zsolt Koppany   (Intland)
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.util.List;

import com.aragost.javahg.commands.Branch;
import com.aragost.javahg.commands.BranchCommand;
import com.aragost.javahg.commands.flags.BranchCommandFlags;
import com.aragost.javahg.commands.flags.BranchesCommandFlags;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;

public class HgBranchClient extends AbstractClient {

	/**
	 * Returns all available (not closed) branches
	 * @param hgRoot non null
	 * @return never null, but possibly empty array
	 * @throws HgException
	 */
	public static Branch[] getBranches(HgRoot hgRoot) throws HgException {
		List<Branch> branches = BranchesCommandFlags.on(
				hgRoot.getRepository()).execute();

		return branches.toArray(new Branch[branches.size()]);
	}

	/**
	 * @param user
	 *            if null, uses the default user
	 * @throws HgException
	 */
	public static void addBranch(HgRoot hgRoot, String name, boolean force) {
		BranchCommand command = BranchCommandFlags.on(hgRoot.getRepository());

		if (force) {
			command.force();
		}

		command.set(name);
	}

	/**
	 * Get active branch of working directory
	 *
	 * @param workingDir
	 *            a file or a directory within the local repository
	 * @return the branch name, never null
	 * @throws HgException
	 *             if a hg error occurred
	 */
	public static String getActiveBranch(HgRoot hgRoot) {
		return BranchCommandFlags.on(hgRoot.getRepository()).get();
	}
}
