/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *     Bastian Doetsch           - added authentication to push
 *     Andrei Loskutov           - bug fixes
 *     Ilya Ivanov (Intland) 	 - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.util.List;

import org.eclipse.core.runtime.jobs.Job;

import com.aragost.javahg.Changeset;
import com.aragost.javahg.commands.PullCommand;
import com.aragost.javahg.commands.PushCommand;
import com.aragost.javahg.commands.flags.PullCommandFlags;
import com.aragost.javahg.commands.flags.PushCommandFlags;
import com.aragost.javahg.merge.MergeContext;
import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.extensions.HgRebaseClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.menu.UpdateJob;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.cache.RefreshRootJob;
import com.vectrace.MercurialEclipse.team.cache.RefreshWorkspaceStatusJob;
import com.vectrace.MercurialEclipse.views.MergeView;

/**
 *
 */
public class HgPushPullClient extends AbstractClient {

	public static void push(HgRoot hgRoot, IHgRepositoryLocation repo, boolean force,
			ChangeSet changeset, int timeout) throws HgException {
		push(hgRoot, repo, force, changeset, timeout, null);
	}

	public static void push(HgRoot hgRoot, IHgRepositoryLocation repo, boolean force,
			ChangeSet changeset, int timeout, String branch) throws HgException {

		final PushCommand command = PushCommandFlags.on(hgRoot.getRepository());

		if (isInsecure()) {
			command.insecure();
		}

		if (force) {
			command.force();
		}

		if (changeset != null) {
			command.rev(changeset.getNode());
		}

		boolean newBranch = MercurialEclipsePlugin.getDefault().getPreferenceStore()
				.getBoolean(MercurialPreferenceConstants.PREF_PUSH_NEW_BRANCH);

		if (newBranch) {
			command.newBranch();
		}

		if (branch != null) {
			command.branch(branch);
		}

		final String remote = setupForRemote(repo, command);

		new JavaHgCommandJob<List<Changeset>>(command,
				makeDescription("Pushing", changeset, branch)) {
			@Override
			protected List<Changeset> run() throws Exception {
				return command.execute(remote);
			}
		}.execute(timeout);
	}

	public static void pull(HgRoot hgRoot, ChangeSet changeset, IHgRepositoryLocation repo,
			boolean update, boolean rebase, boolean force, boolean timeout, boolean merge)
			throws HgException {
		pull(hgRoot, changeset, repo, update, rebase, force, timeout, merge, null);
	}

	/**
	 * Does a pull, then if any of update, rebase, or merge are true does subsequent calls.
	 */
	public static void pull(HgRoot hgRoot, ChangeSet changeset, IHgRepositoryLocation repo,
			boolean update, boolean rebase, boolean force, boolean useTimeout, boolean merge,
			String branch) throws HgException {
		final PullCommand command = PullCommandFlags.on(hgRoot.getRepository());

		if (isInsecure()) {
			command.insecure();
		}

		if (force) {
			command.force();
		}

		if (changeset != null) {
			command.rev(changeset.getNode());
		}

		if (branch != null) {
			command.branch(branch);
		}

		// Do the pull
		{
			final String remote = setupForRemote(repo, command);

			int timeout = useTimeout ? HgClients
					.getTimeOut(MercurialPreferenceConstants.PULL_TIMEOUT) : Integer.MAX_VALUE;
			String description = makeDescription("Pulling", changeset, branch);

			List<Changeset> pulled = new JavaHgCommandJob<List<Changeset>>(command, description) {
				@Override
				protected List<Changeset> run() throws Exception {
					return command.execute(remote);
				}
			}.execute(timeout).getValue();

			if (pulled.isEmpty()) {
				// Nothing to do
				return;
			}
		}

		// Perform the follow up operations and refresh

		// The reason to use "all" instead of only "local + incoming", is that we can pull
		// from another repo as the sync clients for given project may use
		// in this case, we also need to update "outgoing" changesets
		Job refreshJob;

		if (update || rebase || merge) {
			refreshJob = new RefreshWorkspaceStatusJob(hgRoot, RefreshRootJob.ALL);
		} else {
			refreshJob = new RefreshRootJob(hgRoot, RefreshRootJob.ALL);
		}

		if (update) {
			try {
				HgUpdateClient.updateWithoutRefresh(hgRoot, null, false);

				if (HgResolveClient.autoResolve(hgRoot)) {
					HgUpdateClient.showConflictMessage();
				}
			} catch (HgException e) {
				if (HgUpdateClient.isCrossesBranchError(e)) {
					UpdateJob.handleMultipleHeads(hgRoot, false);
				}
			}
		} else if (rebase) {
			try {
				HgRebaseClient.rebaseCurrentOnTip(hgRoot);

				// TODO: autoresolve
			} catch (HgException e) {
				if (HgRebaseClient.isRebaseConflict(e)) {
					refreshJob.addJobChangeListener(MergeView.makeConflictJobChangeListener(hgRoot,
							null, false));
				}
			}
		} else if (merge) {
			MergeContext ctx = HgMergeClient.merge(hgRoot, "tip", false);

			if (HgResolveClient.autoResolve(hgRoot, ctx)) {
				refreshJob.addJobChangeListener(MergeView.makeConflictJobChangeListener(hgRoot,
						null, true));
			}
		}

		refreshJob.schedule();
	}

	private static String makeDescription(String op, ChangeSet changeset, String branch) {
		if (changeset == null) {
			return op + " all changes" + ((branch == null) ? "" : " in " + branch);
		}

		return op + " changeset " + changeset.getNode();
	}

	protected static void applyChangeset(AbstractShellCommand command, ChangeSet changeset) {
		if (changeset != null) {
			String cs = changeset.getNode();

			if (cs != null && (cs = cs.trim()).length() > 0) {
				command.addOptions("-r", cs); //$NON-NLS-1$
			}
		}
	}
}
