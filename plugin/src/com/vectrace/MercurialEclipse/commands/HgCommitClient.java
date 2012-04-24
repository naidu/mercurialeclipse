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
 *     Zsolt Koppany (Intland)
 *     Adam Berkes (Intland)
 *     Andrei Loskutov           - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;

import com.aragost.javahg.commands.CommitCommand;
import com.aragost.javahg.commands.flags.CommitCommandFlags;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.storage.HgCommitMessageManager;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.team.cache.RefreshRootJob;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 *
 */
public class HgCommitClient extends AbstractClient {

	/**
	 * Commit given resources and refresh the caches for the associated projects
	 *
	 * Note: refreshes local and outgoing status
	 */
	public static void commitResources(List<IResource> resources, String user, String message,
			IProgressMonitor monitor, boolean closeBranch) throws HgException {
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

			commit(root, AbstractClient.toFiles(files), user, message, closeBranch);
		}
		for (HgRoot root : resourcesByRoot.keySet()) {
			new RefreshRootJob(root, RefreshRootJob.LOCAL_AND_OUTGOING).schedule();
		}
	}

	/**
	 * Commit given hg root with all checked out/added/deleted changes and refresh the caches for
	 * the associated projects
	 *
	 * Note: refreshes local and outgoing status
	 */
	public static void commitResources(HgRoot root, boolean closeBranch, String user,
			String message, IProgressMonitor monitor) throws HgException {
		monitor.subTask(Messages.getString("HgCommitClient.commitJob.committing") + root.getName()); //$NON-NLS-1$
		List<File> emptyList = Collections.emptyList();
		try {
			commit(root, emptyList, user, message, closeBranch);
		} finally {
			new RefreshRootJob(root, RefreshRootJob.LOCAL_AND_OUTGOING).schedule();
		}
	}

	/**
	 * Performs commit. No refresh of any cashes is done afterwards.
	 *
	 * <b>Note</b> clients should not use this method directly, it is NOT private for tests only
	 */
	protected static void commit(HgRoot hgRoot, List<File> files, String user, String message,
			boolean closeBranch) throws HgException {
		CommitCommand command = CommitCommandFlags.on(hgRoot.getRepository());

		user = MercurialUtilities.getDefaultUserName(user);
		command.user(user);

		if (closeBranch) {
			command.closeBranch();
		}

		command.message(message);

		command.execute(files.toArray(new File[files.size()]));
		HgCommitMessageManager.updateDefaultCommitName(hgRoot, user);
	}

	/**
	 * Commit given project after the merge and refresh the caches. Implementation note: after
	 * merge, no files should be specified.
	 *
	 * Note: refreshes local and outgoing status
	 */
	public static void commit(HgRoot hgRoot, String user, String message) throws HgException {
		CommitCommand command = CommitCommandFlags.on(hgRoot.getRepository());
		user = MercurialUtilities.getDefaultUserName(user);
		command.user(user);
		command.message(message);

		try {
			command.execute();
			HgCommitMessageManager.updateDefaultCommitName(hgRoot, user);
		} finally {
			new RefreshRootJob(hgRoot, RefreshRootJob.LOCAL_AND_OUTGOING).schedule();
		}
	}
}
