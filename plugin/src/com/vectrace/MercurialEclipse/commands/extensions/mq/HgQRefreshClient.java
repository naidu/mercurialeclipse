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

import com.aragost.javahg.commands.ExecutionException;
import com.aragost.javahg.ext.mq.QRefreshCommand;
import com.aragost.javahg.ext.mq.flags.QRefreshCommandFlags;
import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;

/**
 * @author bastian
 *
 */
public class HgQRefreshClient extends AbstractClient {
	public static void refresh(HgRoot root, boolean shortFlag, List<IResource> files,
			String message, boolean currentDate) throws HgException {
		QRefreshCommand command = QRefreshCommandFlags.on(root.getRepository());

		if (shortFlag) {
			command.shortOpt();
		}

		if (message != null && message.length() > 0) {
			command.message(message);
		}

		if (currentDate) {
			command.currentdate();
		}

		execute(command, files);
	}

	public static void refresh(HgRoot root, String commitMessage, List<IResource> resources,
			String user, String date) throws HgException {
		QRefreshCommand command = QRefreshCommandFlags.on(root.getRepository());

		if (commitMessage != null && commitMessage.length() > 0) {
			command.message(commitMessage);
		}

		if (user != null && user.length() > 0) {
			command.user(user);
		} else {
			command.currentuser();
		}

		if (date != null && date.length() > 0) {
			command.date(date);
		} else {
			command.currentdate();
		}

		// TODO: this will refresh dirty files in the patch regardless of whether they're selected
		command.shortOpt();

		execute(command, resources);
	}

	private static void execute(QRefreshCommand command, List<IResource> files) throws HgException {
		try {
			command.execute(toFileArray(files));
		} catch (ExecutionException ex) {
			throw new HgException(ex.getLocalizedMessage(), ex);
		}
	}
}
