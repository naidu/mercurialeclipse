/*******************************************************************************
 * Copyright (c) 2005-2008 Bastian Doetsch and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands.extensions;

import java.io.File;

import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.commands.AbstractShellCommand;
import com.vectrace.MercurialEclipse.commands.HgCommand;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.storage.HgCommitMessageManager;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;

/**
 * @author bastian
 *
 */
public class HgRebaseClient extends AbstractClient {

	/**
	 * Calls hg rebase
	 *
	 * @param hgRoot
	 *            a hg root that is to be rebased.
	 * @param sourceRev
	 *            --source option, -1 if not set
	 * @param baseRev
	 *            --base option, -1 if not set
	 * @param destRev
	 *            --dest option, -1 if not set
	 * @param collapse
	 *            true, if --collapse is to be used
	 * @param cont
	 *            true, if --continue is to be used
	 * @param abort
	 *            true, if --abort is to be used
	 * @param keepBranches
	 * @return the output of the command
	 * @throws HgException
	 */
	public static String rebase(HgRoot hgRoot, int sourceRev, int baseRev,
			int destRev, boolean collapse, boolean cont, boolean abort, boolean keepBranches,
			boolean useExternalMergeTool, String user)
			throws HgException {
		AbstractShellCommand c = new HgCommand("rebase", hgRoot, false);//$NON-NLS-1$
		c.setExecutionRule(new AbstractShellCommand.ExclusiveExecutionRule(hgRoot));
		c.setUsePreferenceTimeout(MercurialPreferenceConstants.PULL_TIMEOUT);
		if (!useExternalMergeTool) {
			// we use simplemerge, so no tool is started. We
			// need this option, though, as we still want the Mercurial merge to
			// take place.
			c.addOptions("--config", "ui.merge=simplemerge"); //$NON-NLS-1$ //$NON-NLS-2$

			// Do not invoke external editor for commit message
			// Future: Allow user to specify this
			c.addOptions("--config", "ui.editor=echo"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		c.addOptions("--config", "extensions.hgext.rebase="); //$NON-NLS-1$ //$NON-NLS-2$

		// User is only applicable for collapse and continued collapse invocations
		c.addOptions("--config", "ui.username=" + user); //$NON-NLS-1$ //$NON-NLS-2$

		if (!cont && !abort) {
			if (sourceRev >= 0 && baseRev <= 0) {
				c.addOptions("--source", "" + sourceRev); //$NON-NLS-1$ //$NON-NLS-2$
			}

			if (sourceRev < 0 && baseRev >= 0) {
				c.addOptions("--base", "" + baseRev); //$NON-NLS-1$ //$NON-NLS-2$
			}

			if (destRev >= 0) {
				c.addOptions("--dest", "" + destRev); //$NON-NLS-1$ //$NON-NLS-2$
			}

			if (collapse) {
				c.addOptions("--collapse"); //$NON-NLS-1$
			}
		}

		if (cont && !abort) {
			c.addOptions("--continue"); //$NON-NLS-1$
		}
		if (abort && !cont) {
			c.addOptions("--abort"); //$NON-NLS-1$
		}

		if(keepBranches) {
			c.addOptions("--keepbranches");
		}

		MercurialStatusCache.getInstance().setMergeViewDialogShown(false);

		return c.executeToString();
	}

	public static String rebase(HgRoot hgRoot, int sourceRev, int baseRev, int destRev,
			boolean collapse, boolean cont, boolean abort, boolean keepBranches,
			boolean useExternalMergeTool) throws HgException {
		return rebase(hgRoot, sourceRev, baseRev, destRev, collapse, cont, abort, keepBranches,
				useExternalMergeTool, HgCommitMessageManager.getDefaultCommitName(hgRoot));
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

	public static String continueRebase(HgRoot hgRoot, String user) throws HgException {
		return rebase(hgRoot, -1, -1, -1, false, true, false, false, false, user);
	}

	public static String abortRebase(HgRoot hgRoot) throws HgException {
		return rebase(hgRoot, -1, -1, -1, false, false, true, false, false);
	}
}
