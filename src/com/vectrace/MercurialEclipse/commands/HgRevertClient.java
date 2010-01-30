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

/**
 * @author Andrei
 */
public class HgRevertClient extends AbstractClient {

	/**
	 * @param monitor non null
	 * @param root
	 * @param resources resources to revert
	 * @param cs might be null
	 * @throws HgException
	 */
	public static void performRevert(IProgressMonitor monitor, HgRoot root,
			List<IResource> resources, ChangeSet cs) throws HgException {
		// the last argument will be replaced with a path
		HgCommand command = new HgCommand("revert", root, true); //$NON-NLS-1$
		command.setUsePreferenceTimeout(MercurialPreferenceConstants.COMMIT_TIMEOUT);
		command.addOptions("--no-backup");
		if (cs != null) {
			command.addOptions("--rev", cs.getChangeset());
		}
		command.addFiles(resources);
		monitor.subTask(Messages.getString("ActionRevert.reverting") + root.getName() + "..."); //$NON-NLS-1$ //$NON-NLS-2$
		command.executeToString();
		monitor.worked(1);
	}
}
