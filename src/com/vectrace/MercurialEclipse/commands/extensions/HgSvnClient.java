/*******************************************************************************
 * Copyright (c) 2005-2008 Bastian Doetsch and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian Doetsch  -   implementation
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands.extensions;

import java.io.File;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.commands.AbstractShellCommand;
import com.vectrace.MercurialEclipse.commands.HgCommand;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.cache.RefreshJob;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * @author bastian
 *
 */
public class HgSvnClient extends AbstractClient {

	public static String pull(IResource resource) throws HgException {
		HgCommand cmd = new HgCommand("svn", //$NON-NLS-1$
				getWorkingDirectory(resource.getLocation().toFile()), false);
		cmd.setUsePreferenceTimeout(MercurialPreferenceConstants.PULL_TIMEOUT);
		cmd.addOptions("pull"); //$NON-NLS-1$
		String result = cmd.executeToString();
		Set<IProject> projects = ResourceUtils.getProjects(cmd.getHgRoot());
		for (final IProject project : projects) {
			// The reason to use "all" instead of only "local + incoming", is that we can pull
			// from another repo as the sync clients for given project may use
			// in this case, we also need to update "outgoing" changesets
			new RefreshJob("Refreshing " + project.getName(), project, RefreshJob.ALL).schedule();
		}
		return result;
	}

	public static String push(File currentWorkingDirectory) throws HgException {
		AbstractShellCommand cmd = new HgCommand("svn", //$NON-NLS-1$
				getWorkingDirectory(currentWorkingDirectory), false);
		cmd.setUsePreferenceTimeout(MercurialPreferenceConstants.PUSH_TIMEOUT);
		cmd.addOptions("push"); //$NON-NLS-1$
		return cmd.executeToString();
	}

	public static String rebase(IResource resource)
			throws HgException {
		HgCommand cmd = new HgCommand("svn", //$NON-NLS-1$
				getWorkingDirectory(resource.getLocation().toFile()), false);
		cmd.setUsePreferenceTimeout(MercurialPreferenceConstants.PUSH_TIMEOUT);
		cmd.addOptions("--config", "extensions.hgext.rebase="); //$NON-NLS-1$ //$NON-NLS-2$
		cmd.addOptions("rebase"); //$NON-NLS-1$
		String result = cmd.executeToString();
		Set<IProject> projects = ResourceUtils.getProjects(cmd.getHgRoot());
		for (final IProject project : projects) {
			new RefreshJob("Refreshing " + project.getName(), project, RefreshJob.LOCAL).schedule();
		}
		return result;
	}

	public static void clone(File currentWorkingDirectory,
			HgRepositoryLocation repo, boolean timeout, String cloneName)
			throws HgException {
		AbstractShellCommand cmd = new HgCommand("svnclone", //$NON-NLS-1$
				getWorkingDirectory(currentWorkingDirectory), false);
		cmd.setUsePreferenceTimeout(MercurialPreferenceConstants.CLONE_TIMEOUT);
		addRepoToHgCommand(repo, cmd);
		if (cloneName != null) {
			cmd.addOptions(cloneName);
		}
		if (timeout) {
			cmd.setUsePreferenceTimeout(MercurialPreferenceConstants.CLONE_TIMEOUT);
			cmd.executeToBytes();
		} else {
			cmd.executeToBytes(Integer.MAX_VALUE);
		}
	}
}
