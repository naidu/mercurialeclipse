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
import java.io.IOException;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.commands.AbstractParseChangesetClient;
import com.vectrace.MercurialEclipse.commands.AbstractShellCommand;
import com.vectrace.MercurialEclipse.commands.HgCommand;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;

/**
 * @author bastian
 *
 */
public class HgRebaseClient extends AbstractClient {

	/**
	 * Calls hg rebase
	 *
	 * @param repoResource
	 *            a file or directory in the repository that is to be rebased.
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
	 * @param keepBranchesCheckBox
	 * @return the output of the command
	 * @throws HgException
	 * @throws IOException
	 */
	public static String rebase(File repoResource, int sourceRev, int baseRev,
			int destRev, boolean collapse, boolean cont, boolean abort, boolean keepBranchesCheckBox,
			boolean useExternalMergeTool)
			throws HgException, IOException {
		AbstractShellCommand c = new HgCommand("rebase", //$NON-NLS-1$
				getWorkingDirectory(repoResource), false);
		c.setUsePreferenceTimeout(MercurialPreferenceConstants.PULL_TIMEOUT);
		c.addOptions("--config", "extensions.hgext.rebase="); //$NON-NLS-1$ //$NON-NLS-2$
		c.addOptions("--config", "ui.merge.interactive=False"); //$NON-NLS-1$ //$NON-NLS-2$
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

		if(keepBranchesCheckBox) {
			c.addOptions("--keepbranches");
		}

		boolean computeFullStatus = MercurialEclipsePlugin.getDefault().getPreferenceStore().getBoolean(MercurialPreferenceConstants.SYNC_COMPUTE_FULL_REMOTE_FILE_STATUS);
		int style = computeFullStatus? AbstractParseChangesetClient.STYLE_WITH_FILES : AbstractParseChangesetClient.STYLE_WITH_FILES_FAST;
		c.addOptions("--style", //$NON-NLS-1$
				AbstractParseChangesetClient.getStyleFile(style).getCanonicalPath());

		if (!useExternalMergeTool) {
			// we use simplemerge, so no tool is started. We
			// need this option, though, as we still want the Mercurial merge to
			// take place.
			c.addOptions("--config", "ui.merge=simplemerge"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		return c.executeToString();
	}

	/** Check to see if we are in the middle of a rebase. <br/>
	 * Assume the presence of the <code>/.hg/rebasestate</code> file means that we are
	 *
	 * @param repoResource
	 * @return <code>true</code> if we are currently rebasing
	 */
	public static boolean isRebasing(File repoResource)
	{
		try {
			return new File(repoResource.getCanonicalPath() + "/.hg/rebasestate").exists();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			MercurialEclipsePlugin.logError(e);
		}
		return false;
	}

	/**
	 * @param canonicalFile
	 * @return
	 * @throws IOException
	 * @throws HgException
	 */
	public static String continueRebase(File canonicalFile) throws HgException, IOException {

		return rebase(canonicalFile, -1, -1, -1, false, true, false, false, false);
	}
}
