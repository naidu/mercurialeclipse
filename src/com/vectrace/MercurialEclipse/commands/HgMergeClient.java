/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch           - implementation
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;

public class HgMergeClient extends AbstractClient {

	public static String merge(HgRoot root, String revision, boolean useExternalMergeTool, boolean forced)
			throws HgException {
		AbstractShellCommand command = new HgCommand("merge", root, false);
		command.setUsePreferenceTimeout(MercurialPreferenceConstants.IMERGE_TIMEOUT);
		if (!useExternalMergeTool) {
			// we use simplemerge, so no tool is started. We
			// need this option, though, as we still want the Mercurial merge to
			// take place.
			command.addOptions("--config", "ui.merge=simplemerge"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		if (revision != null) {
			command.addOptions("-r", revision); //$NON-NLS-1$
		}
		if (forced) {
			command.addOptions("-f"); //$NON-NLS-1$
		}

		try {
			String result = command.executeToString();
			return result;
		} catch (HgException e) {
			// if conflicts aren't resolved and no merge tool is started, hg
			// exits with 1
			if (e.getStatus().getCode() != 1) {
				throw e;
			}
			return e.getMessage();
		}
	}
}
