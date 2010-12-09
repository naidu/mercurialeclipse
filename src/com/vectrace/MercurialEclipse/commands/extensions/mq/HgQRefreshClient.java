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

import java.util.List;

import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.commands.AbstractShellCommand;
import com.vectrace.MercurialEclipse.commands.HgCommand;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;

/**
 * @author bastian
 *
 */
public class HgQRefreshClient extends AbstractClient {
	public static String refresh(HgRoot root, boolean shortFlag, List<IResource> files,
			String message) throws HgException {
		AbstractShellCommand command = new HgCommand("qrefresh", //$NON-NLS-1$
				root, true);

		command.addOptions("--config", "extensions.hgext.mq="); //$NON-NLS-1$ //$NON-NLS-2$
		if (shortFlag) {
			command.addOptions("-s"); //$NON-NLS-1$
		}
		if (message != null && message.length() > 0) {
			command.addOptions("-m", message); //$NON-NLS-1$
		}
		command.addFiles(files);
		return command.executeToString();
	}

	public static String refresh(IResource resource, String commitMessage, boolean force,
			boolean git, String include, String exclude, String user, String date)
			throws HgException {
		AbstractShellCommand command = new HgCommand("qrefresh", //$NON-NLS-1$
				resource, true);

		command.addOptions("--config", "extensions.hgext.mq="); //$NON-NLS-1$ //$NON-NLS-2$

		if (commitMessage != null && commitMessage.length() > 0) {
			command.addOptions("--message", commitMessage); //$NON-NLS-1$
		}
		if (force) {
			command.addOptions("--force"); //$NON-NLS-1$
		}
		if (git) {
			command.addOptions("--git"); //$NON-NLS-1$
		}
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

		return command.executeToString();
	}
}
