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
	 */
	public static String rebase(File repoResource, int sourceRev, int baseRev,
			int destRev, boolean collapse, boolean cont, boolean abort, boolean keepBranchesCheckBox)
			throws HgException {
		AbstractShellCommand c = new HgCommand("rebase", //$NON-NLS-1$
				getWorkingDirectory(repoResource), false);
		c.setUsePreferenceTimeout(MercurialPreferenceConstants.PULL_TIMEOUT);
		c.addOptions("--config", "extensions.hgext.rebase="); //$NON-NLS-1$ //$NON-NLS-2$
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
		return c.executeToString();
	}

}
