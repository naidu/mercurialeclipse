/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands.extensions.mq;

import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.commands.AbstractShellCommand;
import com.vectrace.MercurialEclipse.commands.HgCommand;
import com.vectrace.MercurialEclipse.commands.HgPatchClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;

/**
 * @author bastian
 *
 */
public class HgQPushClient extends AbstractClient {
	public static String pushAll(HgRoot root, boolean force)
			throws HgException {
		AbstractShellCommand command = new HgCommand("qpush", //$NON-NLS-1$
				"Invoking qpush", root, true);
		command.setExecutionRule(new AbstractShellCommand.ExclusiveExecutionRule(root));

		command.addOptions("--config", "extensions.hgext.mq="); //$NON-NLS-1$ //$NON-NLS-2$

		command.addOptions("-a"); //$NON-NLS-1$
		if (force) {
			command.addOptions("--force"); //$NON-NLS-1$
		}
		return command.executeToString();
	}

	public static String push(HgRoot root, boolean force, String patchName)
			throws HgException {
		AbstractShellCommand command = new HgCommand("qpush", //$NON-NLS-1$
				"Invoking qpush", root, true);
		command.setExecutionRule(new AbstractShellCommand.ExclusiveExecutionRule(root));

		command.addOptions("--config", "extensions.hgext.mq="); //$NON-NLS-1$ //$NON-NLS-2$

		if (force) {
			command.addOptions("--force"); //$NON-NLS-1$
		}
		if (!"".equals(patchName)) { //$NON-NLS-1$
			command.addOptions(patchName);
		}
		return command.executeToString();
	}

	public static boolean isPatchApplyConflict(HgException e) {
		// Mercurial 2.0:
		// applying 3489.diff
		// patching file src/file1.text
		// Hunk #1 FAILED at 8
		// 1 out of 1 hunks FAILED -- saving rejects to file src/file1.text.rej
		// patch failed, unable to continue (try -v)
		// patch failed, rejects left in working dir
		// errors during apply, please fix and refresh 3489.diff.

		return e.getMessage().contains("patch failed, rejects left in working dir")
				|| HgPatchClient.isPatchImportConflict(e);
	}
}
