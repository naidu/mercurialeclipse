/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - impl
 *     Bastian Doetsch           - small changes
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands.extensions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.commands.AbstractShellCommand;
import com.vectrace.MercurialEclipse.commands.HgClients;
import com.vectrace.MercurialEclipse.commands.HgCommand;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.FlaggedAdaptable;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;

public class HgIMergeClient extends AbstractClient {

	public static String merge(IProject project, String revision)
			throws HgException {
		AbstractShellCommand command = new HgCommand("imerge", project, false); //$NON-NLS-1$
		command
				.setUsePreferenceTimeout(MercurialPreferenceConstants.IMERGE_TIMEOUT);

		boolean useExternalMergeTool = Boolean.valueOf(
				HgClients.getPreference(
						MercurialPreferenceConstants.PREF_USE_EXTERNAL_MERGE,
						"false")).booleanValue(); //$NON-NLS-1$

		command.addOptions("--config", "extensions.imerge="); //$NON-NLS-1$ //$NON-NLS-2$

		if (!useExternalMergeTool) {
			// we use an non-existent UI Merge tool, so no tool is started. We
			// need this option, though, as we still want the Mercurial merge to
			// take place.
			command.addOptions("--config", "ui.merge=simplemerge"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		if (revision != null) {
			command.addOptions("-r", revision); //$NON-NLS-1$
		}
		return new String(command.executeToBytes());
	}

	public static List<FlaggedAdaptable> getMergeStatus(IResource res)
			throws HgException {
		AbstractShellCommand command = new HgCommand("imerge", getWorkingDirectory(res), //$NON-NLS-1$
				false);
		command.addOptions("--config", "extensions.imerge="); //$NON-NLS-1$ //$NON-NLS-2$
		command.addOptions("status"); //$NON-NLS-1$
		command
				.setUsePreferenceTimeout(MercurialPreferenceConstants.IMERGE_TIMEOUT);
		String[] lines = command.executeToString().split("\n"); //$NON-NLS-1$
		ArrayList<FlaggedAdaptable> result = new ArrayList<FlaggedAdaptable>();
		if (lines.length != 1 || !"all conflicts resolved".equals(lines[0])) { //$NON-NLS-1$
			for (String line : lines) {
				FlaggedAdaptable flagged = new FlaggedAdaptable(res
						.getProject().getFile(line.substring(2)), line
						.charAt(0));
				result.add(flagged);
			}
		}
		return result;
	}

	public static String markResolved(IResource file) throws HgException {
		AbstractShellCommand command = new HgCommand("imerge", getWorkingDirectory(file), //$NON-NLS-1$
				false);
		command.addOptions("--config", "extensions.imerge="); //$NON-NLS-1$ //$NON-NLS-2$
		command.addOptions("resolve"); //$NON-NLS-1$
		command
				.setUsePreferenceTimeout(MercurialPreferenceConstants.IMERGE_TIMEOUT);

		command.addFiles(file.getProjectRelativePath().toOSString());
		String result = command.executeToString();
		refreshStatus(file);
		return result;
	}

	private static void refreshStatus(IResource res) throws HgException {
		MercurialStatusCache.getInstance().refreshStatus(res, null);
		try {
			res.touch(null);
		} catch (CoreException e) {
			MercurialEclipsePlugin.logError(e);
		}
	}

	public static String markUnresolved(IResource file) throws HgException {
		AbstractShellCommand command = new HgCommand("imerge", getWorkingDirectory(file), //$NON-NLS-1$
				false);
		command.addOptions("--config", "extensions.imerge="); //$NON-NLS-1$ //$NON-NLS-2$
		command.addOptions("unresolve"); //$NON-NLS-1$
		command
				.setUsePreferenceTimeout(MercurialPreferenceConstants.IMERGE_TIMEOUT);
		command.addFiles(file.getProjectRelativePath().toOSString());
		String result = command.executeToString();
		refreshStatus(file);
		return result;
	}

}
