/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *     Bastian Doetsch
 *     StefanC
 *     Zsolt Koppany zsolt.koppany@intland.com
 *     Adam Berkes adam.berkes@intland.com
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.dialogs.Messages;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.cache.RefreshJob;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class HgCommitClient extends AbstractClient {
	/**
	 * Commit given resources and refresh the caches for the assotiated projects
	 */
	public static void commitResources(List<IResource> resources, String user, String message, IProgressMonitor monitor) throws HgException {
		Map<HgRoot, List<IResource>> resourcesByRoot = ResourceUtils.groupByRoot(resources);

		for (Map.Entry<HgRoot, List<IResource>> mapEntry : resourcesByRoot.entrySet()) {
			HgRoot root = mapEntry.getKey();
			if (monitor != null) {
				if (monitor.isCanceled()) {
					break;
				}
				monitor.subTask(Messages.getString("HgCommitClient.commitJob.committing") + root.getName()); //$NON-NLS-1$
			}
			List<IResource> files = mapEntry.getValue();
			commit(root, AbstractClient.toFiles(files), user, message);
		}
		for (HgRoot root : resourcesByRoot.keySet()) {
			Set<IProject> projects = ResourceUtils.getProjects(root);
			for (IProject iProject : projects) {
				new RefreshJob("Refreshing " + iProject.getName(), iProject, RefreshJob.LOCAL_AND_OUTGOING).schedule();
			}
		}
	}

	/**
	 * Preforms commit. No refresh of any cashes is done afterwards.
	 *
	 * <b>Note</b> clients should not use this method directly, it is NOT private
	 *  for tests only
	 */
	protected static String commit(HgRoot root, List<File> files, String user, String message) throws HgException {
		HgCommand command = new HgCommand("commit", root, true); //$NON-NLS-1$
		command.setUsePreferenceTimeout(MercurialPreferenceConstants.COMMIT_TIMEOUT);
		command.addUserName(quote(user));
		File messageFile = saveMessage(message);
		try {
			addMessage(command, messageFile, message);
			command.addFiles(AbstractClient.toPaths(files));
			String result = command.executeToString();
			return result;
		} finally {
			deleteMessage(messageFile);
		}
	}

	private static void addMessage(HgCommand command, File messageFile, String message) {
		if (messageFile != null && messageFile.isFile()) {
			command.addOptions("-l", messageFile.getAbsolutePath());
			return;
		}
		// fallback in case of unavailable message file
		message = quote(message.trim());
		if (message.length() != 0) {
			command.addOptions("-m", message);
		} else {
			command.addOptions("-m", Messages.getString("CommitDialog.defaultCommitMessage"));
		}
	}

	/**
	 * Commit given project after the merge and refresh the caches.
	 * Implementation note: after merge, no files should be specified.
	 */
	public static String commitProject(IProject project, String user, String message) throws HgException {
		HgCommand command = new HgCommand("commit", project, true); //$NON-NLS-1$
		command.setUsePreferenceTimeout(MercurialPreferenceConstants.COMMIT_TIMEOUT);
		command.addUserName(quote(user));
		File messageFile = saveMessage(message);
		try {
			addMessage(command, messageFile, message);
			String result = command.executeToString();
			Set<IProject> projects = ResourceUtils.getProjects(command.getHgRoot());
			for (IProject iProject : projects) {
				new RefreshJob("Refreshing " + iProject.getName(), iProject, RefreshJob.LOCAL_AND_OUTGOING).schedule();
			}
			return result;
		} finally {
			deleteMessage(messageFile);
		}
	}

	private static String quote(String str) {
		if (str == null || str.length() == 0) {
			return str;
		}
		// escape quotes, otherwise commit will fail at least on windows
		return str.replace("\"", "\\\""); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static File saveMessage(String message) {
		BufferedWriter writer = null;
		try {
			File messageFile = File.createTempFile("hgcommitmsg", ".txt");
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(messageFile),"UTF8"));
			writer.write(message);
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

	private static void deleteMessage(File messageFile) {
		// Try to delete normally, and if not successfull
		// leave it for the JVM exit - I use it in case
		// mercurial accidentally locks the file.
		if (!messageFile.delete()) {
			messageFile.deleteOnExit();
		}
	}
}
