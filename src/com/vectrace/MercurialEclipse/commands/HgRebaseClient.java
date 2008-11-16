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
package com.vectrace.MercurialEclipse.commands;

import java.io.File;

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
     * @return the output of the command
     * @throws HgException
     */
    public static String rebase(File repoResource, int sourceRev, int baseRev,
            int destRev, boolean collapse, boolean cont, boolean abort)
            throws HgException {
        HgCommand c = new HgCommand("rebase",
                getWorkingDirectory(repoResource), false);
        c.setUsePreferenceTimeout(MercurialPreferenceConstants.PULL_TIMEOUT);
        c.addOptions("--config", "extensions.hgext.rebase=");
        if (!cont && !abort) {
            if (sourceRev >= 0 && baseRev <= 0) {
                c.addOptions("--source", "" + sourceRev);
            }

            if (sourceRev < 0 && baseRev >= 0) {
                c.addOptions("--base", "" + baseRev);
            }

            if (destRev >= 0) {
                c.addOptions("--dest", "" + destRev);
            }

            if (collapse) {
                c.addOptions("--collapse");
            }
        }

        if (cont && !abort) {
            c.addOptions("--continue");
        }
        if (abort && !cont) {
            c.addOptions("--abort");
        }
        return c.executeToString();
    }

}
