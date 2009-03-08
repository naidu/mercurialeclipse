/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch           - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands.extensions.forest;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.commands.AbstractShellCommand;
import com.vectrace.MercurialEclipse.commands.HgCommand;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

public class HgFpushPullClient extends AbstractClient {

    public static String fpush(File forestRoot, HgRepositoryLocation repo,
            String revision, int timeout, File snapFile) throws HgException {
        try {
            AbstractShellCommand command = new HgCommand("fpush",
                    forestRoot, true);
            command
                    .setUsePreferenceTimeout(MercurialPreferenceConstants.PUSH_TIMEOUT);

            if (snapFile != null) {
                command.addOptions("--snapfile", snapFile.getCanonicalPath());
            }

            if (revision != null && revision.length() > 0) {
                command.addOptions("-r", revision.trim());
            }

            URI uri = repo.getUri();
            if (uri != null) {
                command.addOptions(uri.toASCIIString());
            } else {
                command.addOptions(repo.getLocation());
            }

            return new String(command.executeToBytes(timeout));
        } catch (IOException e) {
            throw new HgException(e.getLocalizedMessage(), e);
        }
    }

    public static String fpull(File forestRoot, HgRepositoryLocation repo,
            boolean update, boolean timeout, ChangeSet changeset,
            boolean walkHg, File snapFile, boolean partial) throws HgException {
        URI uri = repo.getUri();
        String pullSource;
        if (uri != null) {
            pullSource = uri.toASCIIString();
        } else {
            pullSource = repo.getLocation();
        }

        return fpull(forestRoot, update, timeout, changeset, walkHg, snapFile,
                partial, pullSource);
    }

    /**
     * @param resource
     * @param update
     * @param force
     * @param timeout
     * @param changeset
     * @param pullSource
     * @return
     * @throws HgException
     */
    public static String fpull(File forestRoot, boolean update,
            boolean timeout,
            ChangeSet changeset, boolean walkHg, File snapFile,
            boolean partial, String pullSource) throws HgException {

        try {
            AbstractShellCommand command = new HgCommand("fpull",
                    forestRoot, true);

            if (update) {
                command.addOptions("--update");
            }

            if (changeset != null) {
                command.addOptions("--rev", changeset.getChangeset());
            }

            if (snapFile != null) {
                command.addOptions("--snapfile", snapFile.getCanonicalPath());
            }

            if (walkHg) {
                command.addOptions("--walkhg", "true");
            } 

            if (partial) {
                command.addOptions("--partial");
            }

            command.addOptions(pullSource);

            if (timeout) {
                command
                        .setUsePreferenceTimeout(MercurialPreferenceConstants.PULL_TIMEOUT);
                return new String(command.executeToBytes());
            }
            return new String(command.executeToBytes(Integer.MAX_VALUE));
        } catch (IOException e) {
            MercurialEclipsePlugin.logError(e);
            throw new HgException(e.getLocalizedMessage(), e);
        }
    }
}
