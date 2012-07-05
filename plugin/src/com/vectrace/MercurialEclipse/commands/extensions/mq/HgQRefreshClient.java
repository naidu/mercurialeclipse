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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

import org.eclipse.core.resources.IResource;

import com.aragost.javahg.commands.ExecutionException;
import com.aragost.javahg.ext.mq.QRefreshCommand;
import com.aragost.javahg.ext.mq.flags.QRefreshCommandFlags;
import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.commands.HgCommand;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

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

	/**
	 * Add the message to the command. If possible a file is created to do this (assumes the command
	 * accepts the -l parameter)
	 *
	 * @return The file that must be deleted
	 * @see #deleteMessage(File)
	 * @deprecated Using JavaHg no need to save to disk
	 */
	@Deprecated
	protected static File addMessage(HgCommand command, String message) {
		File messageFile = saveMessage(message, command);

		if (messageFile != null && messageFile.isFile()) {
			command.addOptions("-l", messageFile.getAbsolutePath());
			return messageFile;
		}

		// fallback in case of unavailable message file
		message = quote(message.trim());
		if (message.length() != 0) {
			command.addOptions("-m", message);
		} else {
			command.addOptions("-m",
					com.vectrace.MercurialEclipse.dialogs.Messages
							.getString("CommitDialog.defaultCommitMessage"));
		}

		return messageFile;
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	private static String quote(String str) {
		if (str != null) {
			str = str.trim();
		}
		if (str == null || str.length() == 0 || !MercurialUtilities.isWindows()) {
			return str;
		}
		// escape quotes, otherwise commit will fail at least on windows
		return str.replace("\"", "\\\""); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	private static File saveMessage(String message, HgCommand command) {
		Writer writer = null;
		try {
			File messageFile = File.createTempFile("hgcommitmsg", ".txt");
			writer = new OutputStreamWriter(new FileOutputStream(messageFile),
					command.getHgRoot().getEncoding());
			writer.write(message.trim());
			return messageFile;
		} catch (IOException ex) {
			MercurialEclipsePlugin.logError(ex);
			return null;
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException ex) {
					MercurialEclipsePlugin.logError(ex);
				}
			}
		}
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	protected static void deleteMessage(File messageFile) {
		// Try to delete normally, and if not successful
		// leave it for the JVM exit - I use it in case
		// mercurial accidentally locks the file.
		if (messageFile != null && !messageFile.delete()) {
			messageFile.deleteOnExit();
		}
	}
}
