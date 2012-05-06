/*******************************************************************************
 * Copyright (c) 2005-2008 Bastian Doetsch and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch           - implementation
 *     Andrei Loskutov           - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands.extensions;

import java.io.File;

import com.aragost.javahg.ext.rebase.RebaseCommand;
import com.aragost.javahg.ext.rebase.flags.RebaseCommandFlags;
import com.aragost.javahg.ext.rebase.merge.RebaseConflictResolvingContext;
import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.commands.HgClients;
import com.vectrace.MercurialEclipse.commands.JavaHgCommandJob;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.team.cache.RefreshRootJob;
import com.vectrace.MercurialEclipse.team.cache.RefreshWorkspaceStatusJob;

/**
 * @author bastian
 */
public class HgRebaseClient extends AbstractClient {

	/**
	 * Helper method to call
	 * {@link #rebase(HgRoot, int, int, int, boolean, boolean, boolean, boolean, String)}
	 */
	public static RebaseConflictResolvingContext rebaseCurrentOnTip(HgRoot hgRoot) throws HgException {
		return HgRebaseClient.rebase(hgRoot, null, null, null, false, false, false, false, null);
	}

	/**
	 * Calls hg rebase.
	 * <p>
	 * Doesn't support supplying custom commit messages for collapse and continued collapse.
	 *
	 * @param hgRoot
	 *            a hg root that is to be rebased.
	 * @param sourceNode
	 *            --source option, -1 if not set
	 * @param baseNode
	 *            --base option, -1 if not set
	 * @param destNode
	 *            --dest option, -1 if not set
	 * @param collapse
	 *            true, if --collapse is to be used
	 * @param cont
	 *            true, if --continue is to be used
	 * @param keepBranches
	 * @param user
	 *            The user to use for collapse and continued collapse. May be null
	 * @return the output of the command
	 * @throws HgException
	 */
	public static RebaseConflictResolvingContext rebase(HgRoot hgRoot, String sourceNode,
			String baseNode, String destNode, boolean collapse, final boolean cont,
			boolean keepBranches, boolean keep, String user) throws HgException {

		final RebaseCommand c = RebaseCommandFlags.on(hgRoot.getRepository());

		if (!isUseExternalMergeTool()) {
			// TODO: autoresolve!!!
			c.cmdAppend("--config", "ui.merge=simplemerge"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		if (!isUseExternalMergeTool()) {
			// Do not invoke external editor for commit message
			// Future: Allow user to specify this
			c.cmdAppend("--config", "ui.editor=echo"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		// User is only applicable for collapse and continued collapse invocations
		if (user != null) {
			c.cmdAppend("--config", "ui.username=" + user); //$NON-NLS-1$ //$NON-NLS-2$
		}

		if (!cont) {
			// Source or base or neither is set
			if (sourceNode != null && baseNode == null) {
				c.source(sourceNode);
			} else if (baseNode != null && sourceNode == null) {
				c.base(baseNode);
			}

			if (destNode != null) {
				c.dest(destNode);
			}

			if (collapse) {
				c.collapse();
			}
		}

		if (keepBranches) {
			c.keepbranches();
		}
		if (keep) {
			c.keep();
		}

		MercurialUtilities.setOfferAutoCommitMerge(true);

		return new JavaHgCommandJob<RebaseConflictResolvingContext>(c, "Rebasing") {
			@Override
			protected RebaseConflictResolvingContext run() throws Exception {
				if (cont) {
					return c.executeContinue();
				}
				return c.execute();
			}
		}.execute(HgClients.getTimeOut(MercurialPreferenceConstants.PULL_TIMEOUT)).getValue();
	}

	/**
	 * Invoke hg rebase --abort. Note: Refreshes the workspace.
	 *
	 * @param hgRoot
	 *            The hg root to use
	 * @throws HgException
	 *             On error
	 */
	public static void abortRebase(HgRoot hgRoot) throws HgException {
		try {
			RebaseCommandFlags.on(hgRoot.getRepository()).abort();
		} finally {
			new RefreshWorkspaceStatusJob(hgRoot, RefreshRootJob.ALL).schedule();
		}
	}

	/**
	 * Check to see if we are in the middle of a rebase. <br/>
	 * Assume the presence of the <code>/.hg/rebasestate</code> file means that we are
	 *
	 * @param hgRoot
	 * @return <code>true</code> if we are currently rebasing
	 */
	public static boolean isRebasing(HgRoot hgRoot) {
		return hgRoot.getRepository().workingCopy().getParent2() != null
				&& new File(hgRoot, ".hg" + File.separator + "rebasestate").exists();
	}
}
