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

import java.io.File;

import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.commands.HgCommand;
import com.vectrace.MercurialEclipse.commands.HgCommitClient;
import com.vectrace.MercurialEclipse.exception.HgException;

/**
 * @author bastian
 *
 */
public class HgQNewClient extends AbstractClient {
	public static String createNewPatch(IResource resource,
			String commitMessage, String include,
			String exclude, String user, String date, String patchName)
			throws HgException {
		HgCommand command = new HgCommand("qnew", //$NON-NLS-1$
				"Invoking qnew", resource, true);
		File messageFile = null;

		command.addOptions("--config", "extensions.hgext.mq="); //$NON-NLS-1$ //$NON-NLS-2$

		if (commitMessage != null && commitMessage.length() > 0) {
			messageFile = HgCommitClient.addMessage(command, commitMessage);
		}

		command.addOptions("--git"); //$NON-NLS-1$

		if (include != null && include.length() > 0) {
			command.addOptions("--include", include); //$NON-NLS-1$
		}
		if (exclude != null && exclude.length() > 0) {
			command.addOptions("--exclude", exclude); //$NON-NLS-1$
		}
		if (user != null && user.length() > 0) {
			command.addOptions("--user", user); //$NON-NLS-1$
		} else {
			command.addOptions("--currentuser"); //$NON-NLS-1$
		}

		if (date != null && date.length() > 0) {
			command.addOptions("--date", date); //$NON-NLS-1$
		} else {
			command.addOptions("--currentdate"); //$NON-NLS-1$
		}

		command.addOptions(patchName);

		try {
			return command.executeToString();
		} finally {
			HgCommitClient.deleteMessage(messageFile);
		}
	}
}
