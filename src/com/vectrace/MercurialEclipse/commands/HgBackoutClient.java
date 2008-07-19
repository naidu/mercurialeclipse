/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import org.eclipse.core.resources.IProject;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;

/**
 * @author bastian
 * 
 */
public class HgBackoutClient {

    /**
     * Backout of a changeset
     * 
     * @param project
     *            the project
     * @param backoutRevision
     *            revision to backout
     * @param merge
     *            flag if merge with a parent is wanted
     * @param parentRevision
     *            revision to merge with
     * @param msg
     *            commit message
     * @return
     * @throws HgException
     */
    public static String backout(IProject project, ChangeSet backoutRevision,
            boolean merge, String msg, String user)
            throws HgException {
        HgCommand command = new HgCommand("backout", project, true);
        boolean useExternalMergeTool = Boolean
        .valueOf(
                HgClients
                        .getPreference(
                                MercurialPreferenceConstants.PREF_USE_EXTERNAL_MERGE,
                                "false")).booleanValue();
        if (!useExternalMergeTool) {
            command.addOptions("--config", "ui.merge=internal:fail");
        }
        command.addOptions("-r", backoutRevision.getChangeset(), "-m", msg,
                "-u", user);
        if (merge) {
            command.addOptions("--merge");
        }
        
        return command.executeToString();
    }

}
