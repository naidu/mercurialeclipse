/*******************************************************************************
 * Copyright (c) 2005-2010 Andrei Loskutov (Intland) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov (Intland) - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.Messages;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;

/**
 * @author Andrei
 */
public class HgRevertClient extends AbstractClient {

	/**
	 * @param monitor non null
	 * @param hgRoot the root of all given resources
	 * @param resources resources to revert
	 * @param cs might be null
	 * @throws HgException
	 */
	public static void performRevert(IProgressMonitor monitor, HgRoot hgRoot,
			List<IResource> resources, ChangeSet cs) throws HgException {
		monitor.subTask(Messages.getString("ActionRevert.reverting") + " " + hgRoot.getName() + "..."); //$NON-NLS-1$ //$NON-NLS-2$
		// if there are too many resources, do several calls
		int size = resources.size();
		int delta = AbstractShellCommand.MAX_PARAMS - 1;
		for (int i = 0; i < size && !monitor.isCanceled(); i += delta) {
			// the last argument will be replaced with a path
			HgCommand command = new HgCommand("revert", //$NON-NLS-1$
					"Reverting resource " + i + " of " + size, hgRoot, true);
			command.setExecutionRule(new AbstractShellCommand.ExclusiveExecutionRule(hgRoot));
			command.setUsePreferenceTimeout(MercurialPreferenceConstants.COMMIT_TIMEOUT);
			command.addOptions("--no-backup");
			if (cs != null) {
				command.addOptions("--rev", cs.getChangeset());
			}
			command.addFiles(resources.subList(i, Math.min(i + delta, size)));
			command.executeToString();
		}
		monitor.worked(1);

		MercurialStatusCache.getInstance().setMergeViewDialogShown(false);
	}

	public static void performRevertAll(IProgressMonitor monitor, HgRoot hgRoot) throws HgException {
		monitor.subTask(Messages.getString("ActionRevert.reverting") + " " + hgRoot.getName() + "..."); //$NON-NLS-1$ //$NON-NLS-2$

		HgCommand command = new HgCommand("revert", "Reverting all resources", hgRoot, true); //$NON-NLS-1$
		command.setExecutionRule(new AbstractShellCommand.ExclusiveExecutionRule(hgRoot));
		command.setUsePreferenceTimeout(MercurialPreferenceConstants.COMMIT_TIMEOUT);
		command.addOptions("--all");
		command.addOptions("--no-backup");
		command.executeToString();

		MercurialStatusCache.getInstance().setMergeViewDialogShown(false);
	}
}
