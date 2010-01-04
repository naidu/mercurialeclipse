/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch           - implementation
 *     Andrei Loskutov (Intland) - bug fixes
 *     Adam Berkes (Intland)     - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.FlaggedAdaptable;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class HgResolveClient extends AbstractClient {

	/**
	 * List merge state of files after merge
	 */
	public static List<FlaggedAdaptable> list(IResource res) throws HgException {
		AbstractShellCommand command = new HgCommand("resolve", getWorkingDirectory(res), //$NON-NLS-1$
				false);
		command
				.setUsePreferenceTimeout(MercurialPreferenceConstants.IMERGE_TIMEOUT);
		command.addOptions("-l"); //$NON-NLS-1$
		String[] lines = command.executeToString().split("\n"); //$NON-NLS-1$
		List<FlaggedAdaptable> result = new ArrayList<FlaggedAdaptable>();
		if (lines.length != 1 || !"".equals(lines[0])) { //$NON-NLS-1$
			HgRoot hgRoot = getHgRoot(res);
			IProject project = res.getProject();
			for (String line : lines) {
				// Status line is always hg root relative. For those projects
				// which has different project root (always deeper than hg root)
				// hg root relative path must be converted
				IResource iFile = ResourceUtils.convertRepoRelPath(hgRoot, project, line.substring(2));
				FlaggedAdaptable fa = new FlaggedAdaptable(iFile, line
						.charAt(0));
				result.add(fa);
			}
		}
		return result;
	}

	/**
	 * List merge state of files after merge
	 */
	public static List<FlaggedAdaptable> list(HgRoot hgRoot) throws HgException {
		AbstractShellCommand command = new HgCommand("resolve", hgRoot, false);
		command.setUsePreferenceTimeout(MercurialPreferenceConstants.IMERGE_TIMEOUT);
		command.addOptions("-l"); //$NON-NLS-1$
		String[] lines = command.executeToString().split("\n"); //$NON-NLS-1$
		List<FlaggedAdaptable> result = new ArrayList<FlaggedAdaptable>();
		if (lines.length != 1 || !"".equals(lines[0])) { //$NON-NLS-1$
			for (String line : lines) {
				// Status line is always hg root relative. For those projects
				// which has different project root (always deeper than hg root)
				// hg root relative path must be converted
				String repoRelPath = line.substring(2);
				IResource iFile = ResourceUtils.getFileHandle(hgRoot.toAbsolute(new Path(repoRelPath)));
				FlaggedAdaptable fa = new FlaggedAdaptable(iFile, line.charAt(0));
				result.add(fa);
			}
		}
		return result;
	}

	/**
	 * Mark a resource as resolved ("R")
	 */
	public static String markResolved(IFile ifile) throws HgException {
		File file = ifile.getLocation().toFile();
		try {
			AbstractShellCommand command = new HgCommand("resolve", //$NON-NLS-1$
					getWorkingDirectory(file), false);
			command
					.setUsePreferenceTimeout(MercurialPreferenceConstants.IMERGE_TIMEOUT);
			command.addOptions("-m", file.getCanonicalPath()); //$NON-NLS-1$
			String result = command.executeToString();
			refreshStatus(ifile);
			return result;
		} catch (IOException e) {
			throw new HgException(e.getLocalizedMessage(), e);
		}
	}

	/**
	 * Try to resolve all unresolved files
	 */
	public static String resolveAll(IResource res) throws HgException {
		File file = res.getLocation().toFile();
		AbstractShellCommand command = new HgCommand("resolve", getWorkingDirectory(file), //$NON-NLS-1$
				false);

		boolean useExternalMergeTool = Boolean.valueOf(
				HgClients.getPreference(
						MercurialPreferenceConstants.PREF_USE_EXTERNAL_MERGE,
						"false")).booleanValue(); //$NON-NLS-1$
		if (!useExternalMergeTool) {
			// we use an non-existent UI Merge tool, so no tool is started. We
			// need this option, though, as we still want the Mercurial merge to
			// take place.
			command.addOptions("--config", "ui.merge=simplemerge"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		command
				.setUsePreferenceTimeout(MercurialPreferenceConstants.IMERGE_TIMEOUT);
		String result = command.executeToString();
		refreshStatus(res);
		return result;

	}

	/**
	 * Mark a resource as unresolved ("U")
	 */
	public static String markUnresolved(IFile ifile) throws HgException {
		File file = ifile.getLocation().toFile();
		try {
			AbstractShellCommand command = new HgCommand("resolve", //$NON-NLS-1$
					getWorkingDirectory(file), false);
			command
					.setUsePreferenceTimeout(MercurialPreferenceConstants.IMERGE_TIMEOUT);
			command.addOptions("-u", file.getCanonicalPath()); //$NON-NLS-1$
			String result = command.executeToString();
			refreshStatus(ifile);
			return result;
		} catch (IOException e) {
			throw new HgException(e.getLocalizedMessage(), e);
		}
	}

	private static void refreshStatus(IResource res) throws HgException {
		MercurialStatusCache.getInstance().refreshStatus(res, null);
		try {
			res.touch(null);
		} catch (CoreException e) {
			MercurialEclipsePlugin.logError(e);
		}
	}

}
