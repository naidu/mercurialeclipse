/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.PlatformUI;

import com.aragost.javahg.commands.UpdateCommand;
import com.aragost.javahg.commands.UpdateResult;
import com.aragost.javahg.commands.flags.UpdateCommandFlags;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.cache.RefreshRootJob;
import com.vectrace.MercurialEclipse.team.cache.RefreshWorkspaceStatusJob;

/**
 * TODO: check conflict handling
 */
public class HgUpdateClient extends AbstractClient {

	public static void cleanUpdate(final HgRoot hgRoot, String revision) throws HgException
	{
		update(hgRoot, revision, true);
	}

	/**
	 * Perform an update and refresh the workspace. Handles unresolved conflicts and shows the user a message
	 */
	public static void update(final HgRoot hgRoot, String revision, boolean clean)
			throws HgException {
		try {
			updateWithoutRefresh(hgRoot, revision, clean);
		} catch (HgException e) {
			handleConflicts(e);
		} finally {
			new RefreshWorkspaceStatusJob(hgRoot, RefreshRootJob.LOCAL).schedule();
		}
	}

	/**
	 * If the exception is an update conflict informs the user, otherwise rethrows the exception.
	 */
	public static void handleConflicts(HgException e) throws HgException {
		if (isWorkspaceUpdateConflict(e)) {
			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
				public void run() {
					MessageDialog
							.openInformation(null, "Unresolved conflicts",
									"There are unresolved conflicts after update. Use Synchronize View to edit conflicts");
				}
			});
		} else {
			throw e;
		}
	}

	/**
	 * Perform an update.
	 *
	 * @param hgRoot
	 *            The root to use
	 * @param revision
	 *            The revision, may be null
	 * @param clean
	 *            Whether a clean update should be done
	 * @return The result of the invocation
	 */
	public static void updateWithoutRefresh(HgRoot hgRoot, String revision, boolean clean) throws HgException {
		final UpdateCommand command = UpdateCommandFlags.on(hgRoot.getRepository());

		if (revision != null && revision.trim().length() > 0) {
			command.rev(revision);
		}

		if (clean) {
			command.clean();
		}

		addMergeToolPreference(command);

		new JavaHgCommandJob<UpdateResult>(command, makeDescription(revision, clean)) {
			@Override
			protected UpdateResult run() throws Exception {
				return command.execute();
			}
		}.execute(HgClients.getTimeOut(MercurialPreferenceConstants.UPDATE_TIMEOUT));
	}

	private static String makeDescription(String revision, boolean clean) {
		revision = (revision == null || revision.trim().length() == 0) ? null : revision.trim();

		if (revision != null) {
			return ((clean) ? "Clean update" : "Updating") +  " to " + revision;
		}

		return (clean) ? "Clean update" : "Updating working directory";
	}

	public static boolean isWorkspaceUpdateConflict(HgException e) {
		return e.getMessage().contains("use 'hg resolve' to retry unresolved file merges");
	}

	public static boolean isCrossesBranchError(HgException e) {
		return e.getMessage().contains("abort: crosses branches") && e.getStatus().getCode() == -1;
	}
}
