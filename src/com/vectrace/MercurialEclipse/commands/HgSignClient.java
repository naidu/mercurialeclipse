/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch      - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

/**
 * Client for hg sign
 * 
 * @author Bastian Doetsch
 * 
 */
public class HgSignClient {

    /**
     * Calls hg sign. add a signature for the current or given revision If no
     * revision is given, the parent of the working directory is used, or tip if
     * no revision is checked out.
     * 
     * @param project
     *            the current project (working directory)
     * @param cs
     *            ChangeSet, may be null
     * @param key
     *            the keyId to use
     * @param message
     *            commit message
     * @param user
     *            user name for commit
     * @param local
     *            flag, if signing is only local
     * @param force
     *            flag to even sign if sigfile is changed
     * @param noCommit
     *            flag, if commit shall happen (invalidates params message and
     *            user)
     * @param passphrase
     *            the passphrase or null
     * @author Bastian Doetsch
     * @return
     * 
     */
    public static String sign(IProject project, ChangeSet cs, String key,
            String message, String user, boolean local, boolean force,
            boolean noCommit, String passphrase) throws HgException {
        HgCommand command = new HgCommand("sign", project, true);
        File file = new File("me.gpg.tmp");
        String cmd = "gpg.cmd=".concat(
                MercurialUtilities.getGpgExecutable(true)).concat(
                " --batch --no-tty --armor");
        if (passphrase != null && passphrase.length() > 0) {
            FileWriter fw = null;
            try {
                fw = new FileWriter(file);
                fw.write(passphrase.concat("\n"));
                fw.flush();
                cmd = cmd.concat(" --passphrase-file ").concat(
                        file.getCanonicalFile().getCanonicalPath());
            } catch (IOException e) {
                throw new HgException(e.getMessage());
            } finally {
                if (fw != null) {
                    try {
                        fw.close();
                    } catch (Exception e) {
                        MercurialEclipsePlugin.logError(e);
                    }
                }
            }
        }
        command.addOptions("-k", key, "--config", cmd);
        if (local) {
            command.addOptions("-l");
        }
        if (force) {
            command.addOptions("-f");
        }
        if (noCommit) {
            command.addOptions("--no-commit");
        } else {
            command.addOptions("-m", message, "-u", user);
        }
        
        command.addOptions(cs.getChangeset());
        String result;
        try {
            result = command.executeToString();
            return result;
        } finally {
            file.delete();
        }
    }

    public static String getPrivateKeyList() throws HgException {
        List<String> getKeysCmd = new ArrayList<String>();
        getKeysCmd.add(MercurialUtilities.getGpgExecutable(true));
        getKeysCmd.add("-k");
        getKeysCmd.add("-v");
        getKeysCmd.add("0");
        GpgCommand command = new GpgCommand(getKeysCmd, ResourcesPlugin
                .getWorkspace().getRoot().getLocation().toFile(), false);
        return new String(command.executeToBytes());
    }
}
