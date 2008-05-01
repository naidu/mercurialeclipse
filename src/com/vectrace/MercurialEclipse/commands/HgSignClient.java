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

import org.eclipse.core.resources.IProject;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;

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
        String cmd = "gpg.cmd=gpg --batch --no-tty";
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
        String result;
        try {
            result = command.executeToString();
            return result;
        } finally {
            file.delete();
        }
    }
}
