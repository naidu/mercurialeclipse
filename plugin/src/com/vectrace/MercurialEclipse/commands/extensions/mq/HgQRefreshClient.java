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
import java.util.List;

import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.commands.HgCommand;
import com.vectrace.MercurialEclipse.commands.HgCommitClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;

/**
 * @author bastian
 *
 */
public class HgQRefreshClient extends AbstractClient {
	public static String refresh(HgRoot root, boolean shortFlag, List<IResource> files,
			String message, boolean currentDate) throws HgException {
		HgCommand command = new HgCommand("qrefresh", //$NON-NLS-1$
				"Invoking qrefresh", root, true);

		command.addOptions("--config", "extensions.hgext.mq="); //$NON-NLS-1$ //$NON-NLS-2$
		if (shortFlag) {
			command.addOptions("-s"); //$NON-NLS-1$
		}

		File messageFile = null;

		if (message != null && message.length() > 0) {
			messageFile = HgCommitClient.addMessage(command, message);
		}

		if (currentDate) {
			command.addOptions("--currentdate");
		}
		command.addFiles(files);

		try
		{
			return command.executeToString();
		}
		finally
		{
			HgCommitClient.deleteMessage(messageFile);
		}
	}

	public static String refresh(IResource resource, String commitMessage,
			String include, String exclude, String user, String date)
			throws HgException {
		HgCommand command = new HgCommand("qrefresh", //$NON-NLS-1$
				"Invoking qrefresh", resource, true);
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

		try
		{
			return command.executeToString();
		}
		finally
		{
			HgCommitClient.deleteMessage(messageFile);
		}
	}
}
