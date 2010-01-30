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

import java.io.File;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.Branch;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

public class HgBranchClient extends AbstractClient {

	private static final Pattern GET_BRANCHES_PATTERN = Pattern
			.compile("^(.+[^ ]) +([0-9]+):([a-f0-9]+)( +(.+))?$"); //$NON-NLS-1$

	public static Branch[] getBranches(IProject project) throws HgException {
		AbstractShellCommand command = new HgCommand("branches", project, false); //$NON-NLS-1$
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
	 *
	 * @param resource
	 * @param name
	 * @param user
	 *            if null, uses the default user
	 * @throws HgException
	 */
	public static String addBranch(IResource resource, String name,
			String user, boolean force) throws HgException {
		HgCommand command = new HgCommand("branch", getWorkingDirectory(resource), false); //$NON-NLS-1$
		command.setExecutionRule(new AbstractShellCommand.ExclusiveExecutionRule(command.getHgRoot()));
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
	public static String getActiveBranch(File workingDir) throws HgException {
		AbstractShellCommand command = new HgCommand("branch", getWorkingDirectory(workingDir), false); //$NON-NLS-1$
		return command.executeToString().replaceAll("\n", "");
	}

	/**
	 * @param root non null
	 * @param repository non null
	 * @param branch non null
	 * @return true if the given branch is known at remote repository
	 */
	public static boolean isKnownRemote(HgRoot root,
			HgRepositoryLocation repository, String branch) {
		if(branch == null || Branch.isDefault(branch)){
			return true;
		}

		// we are using "hg incoming" to check if remote repository knows the given branch
		// unfortunately I didn't found more elegant way to get this infor from hg for
		// REMOTE repository, because neither "hg branch" nor "hg branches" works then
		AbstractShellCommand command = new HgCommand("incoming", root, //$NON-NLS-1$
				false);
		command.setUsePreferenceTimeout(MercurialPreferenceConstants.PULL_TIMEOUT);

		// limit to one version
		command.addOptions("-l", "1");
		// try to access the the branch
		command.addOptions("-r", branch);

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
			return true;
		} catch (HgException hg) {
			return false;
		}
	}
}
