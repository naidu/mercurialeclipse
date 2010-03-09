/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.Branch;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.cache.RemoteKey;

public class HgBranchClient extends AbstractClient {

	private static final Pattern GET_BRANCHES_PATTERN = Pattern
			.compile("^(.+[^ ]) +([0-9]+):([a-f0-9]+)( +(.+))?$"); //$NON-NLS-1$

	private static Map<RemoteKey, Boolean> KNOWN_BRANCHES = new ConcurrentHashMap<RemoteKey, Boolean>();

	public static Branch[] getBranches(HgRoot hgRoot) throws HgException {
		AbstractShellCommand command = new HgCommand("branches", hgRoot, false); //$NON-NLS-1$
		command.addOptions("-v"); //$NON-NLS-1$
		String[] lines = command.executeToString().split("\n"); //$NON-NLS-1$
		int length = lines.length;
		Branch[] branches = new Branch[length];
		for (int i = 0; i < length; i++) {
			Matcher m = GET_BRANCHES_PATTERN.matcher(lines[i]);
			if (m.matches()) {
				Branch branch = new Branch(m.group(1), Integer.parseInt(m.group(2)), m
						.group(3), (m.group(5) == null || !m.group(5).equals("(inactive)"))); //$NON-NLS-1$
				branches[i] = branch;
			} else {
				throw new HgException("Parse exception: '" + lines[i] + "'"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return branches;
	}

	/**
	 * @param user
	 *            if null, uses the default user
	 * @throws HgException
	 */
	public static String addBranch(HgRoot hgRoot, String name,
			String user, boolean force) throws HgException {
		HgCommand command = new HgCommand("branch", hgRoot, false); //$NON-NLS-1$
		command.setExecutionRule(new AbstractShellCommand.ExclusiveExecutionRule(hgRoot));
		if (force) {
			command.addOptions("-f"); //$NON-NLS-1$
		}
		command.addOptions(name);
		return command.executeToString();
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
	public static String getActiveBranch(HgRoot workingDir) throws HgException {
		AbstractShellCommand command = new HgCommand("branch", workingDir, false); //$NON-NLS-1$
		return command.executeToString().replaceAll("\n", "");
	}

	/**
	 * @param key non null
	 * @return true if the given branch is known at remote repository
	 */
	public static boolean isKnownRemote(RemoteKey key) {
		String branch = key.getBranch();
		if(Branch.isDefault(branch)){
			// default is always there
			return true;
		}
		Boolean result = KNOWN_BRANCHES.get(key);
		if(Boolean.TRUE.equals(result)){
			return true;
		}

		// we are using "hg incoming" to check if remote repository knows the given branch
		// unfortunately I didn't found more elegant way to get this infor from hg for
		// REMOTE repository, because neither "hg branch" nor "hg branches" works then
		AbstractShellCommand command = new HgCommand("incoming", key.getRoot(), //$NON-NLS-1$
				false);
		command.setUsePreferenceTimeout(MercurialPreferenceConstants.PULL_TIMEOUT);

		// limit to one version
		command.addOptions("-l", "1");
		// try to access the the branch
		command.addOptions("-r", branch);

		IHgRepositoryLocation repository = key.getRepo();
		URI uri;
		try {
			uri = repository.getUri();
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
			return false;
		}

		if (uri != null) {
			command.addOptions(uri.toASCIIString());
		} else {
			command.addOptions(repository.getLocation());
		}
		try {
			command.executeToString();
			// remember branch, don't ask each time. The probability that somebody "closes" it is very low
			KNOWN_BRANCHES.put(key, Boolean.TRUE);
			return true;
		} catch (HgException e) {
			String message = e.getMessage();
			// not sure how reliable this is. On non-default language systems the first
			// part won't work. The second part (exit code) may rely on different OS implementations...

			// Incoming responds with an exception if there are no incoming changesets...
			// but exception is different if no branch exists on remote. So trying to
			// distinguish between "no changesets" and "unknown branch (== unknown revision)"
			if (message.contains("no changes found") && !message.contains("unknown revision")
					&& e.getStatus().getCode() == 1) {
				KNOWN_BRANCHES.put(key, Boolean.TRUE);
				return true;
			}
			// TODO we should improve it later, if we can keep the "unknown branches" state up to date
			// right now don't save "unknown" state, as it may change at any time
			return false;
		}
	}
}
