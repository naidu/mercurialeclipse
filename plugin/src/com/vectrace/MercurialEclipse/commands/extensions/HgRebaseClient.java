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
import java.util.regex.Pattern;

import com.aragost.javahg.ext.rebase.RebaseCommand;
import com.aragost.javahg.ext.rebase.flags.RebaseCommandFlags;
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

	private static final Pattern REBASING_CONFLICT = Pattern.compile("^abort:.*unresolved conflicts", Pattern.MULTILINE);

	/**
	 * Helper method to call
	 * {@link #rebase(HgRoot, int, int, int, boolean, boolean, boolean, boolean, boolean, String)}
	 */
	public static void rebaseCurrentOnTip(HgRoot hgRoot) throws HgException {
		HgRebaseClient.rebase(hgRoot, null, null, null, false, false, false, false, false, null);
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
	 * @param abort
	 *            true, if --abort is to be used
	 * @param keepBranches
	 * @param user
	 *            The user to use for collapse and continued collapse. May be null
	 * @return the output of the command
	 * @throws HgException
	 */
	public static void rebase(HgRoot hgRoot, String sourceNode, String baseNode, String destNode,
			boolean collapse, final boolean cont, final boolean abort, boolean keepBranches,
			boolean keep, String user) throws HgException {

		final RebaseCommand c = RebaseCommandFlags.on(hgRoot.getRepository());

		addMergeToolPreference(c);

		if (!isUseExternalMergeTool()) {
			// Do not invoke external editor for commit message
			// Future: Allow user to specify this
			c.cmdAppend("--config", "ui.editor=echo"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		// User is only applicable for collapse and continued collapse invocations
		if (user != null) {
			c.cmdAppend("--config", "ui.username=" + user); //$NON-NLS-1$ //$NON-NLS-2$
		}

		if (!cont && !abort) {
			// Source or base or neither is set
			if (sourceNode != null && baseNode == null) {
				c.source(sourceNode);
			}
			else if (baseNode != null && sourceNode == null) {
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

		new JavaHgCommandJob<Object>(c, "Rebasing") {
			@Override
			protected Object run() throws Exception {
				if (cont && !abort) {
					c.executeContinue();
				} else if (abort && !cont) {
					c.executeAbort();
				} else {
					c.execute();
				}
				return null;
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
			rebase(hgRoot, null, null, null, false, false, true, false, false, null);
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
		return new File(hgRoot, ".hg" + File.separator + "rebasestate").exists();
	}

	/**
	 * Determine if the given exception indicates a rebase conflict occurred.
	 * <p>
	 * Warning: Will this work on non-English locales?
	 * <p>
	 * Warning: Will hg output change?
	 *
	 * @param e
	 *            The exception to check
	 * @return True if the exception indicates a conflict occurred
	 */
	public static boolean isRebaseConflict(HgException e) {
		String message = e.getMessage();

		// Conflicts are expected:
		// 1.6.x:
		// /bin/sh: simplemerge: command not found
		// merging file1.txt
		// merging file1.txt
		// merging file1.txt failed!
		// abort: fix unresolved conflicts with hg resolve then run hg rebase --continue.
		// Command line: /home/john/runtime-New_configuration/hgtest2/hg -y
		// rebase --config ui.merge=simplemerge --config ui.editor=echo --config
		// extensions.hgext.rebase= --config ui.username=john --base 8 --dest 5, error
		// code: 255

		// 1.8.3 and 1.8.4
		// merging file1-4.txt
		// /bin/sh: simplemerge: command not found
		// merging file1-4.txt failed!
		// abort: unresolved conflicts (see hg resolve, then hg rebase --continue).
		// Command line: /home/john/runtime-New_configuration/hgtest:hg -y rebase --config
		// ui.merge=simplemerge --config ui.editor=echo --config extensions.hgext.rebase= --source
		// 3442 --dest 3441, error code: 255
		return (message != null && REBASING_CONFLICT.matcher(message).find());
	}
}
