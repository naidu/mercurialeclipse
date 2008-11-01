/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *     Bastian Doetsch           - added authentication to push
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.net.URI;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

public class HgPushPullClient extends AbstractClient {

    public static String push(IProject project, HgRepositoryLocation repo,
            boolean force, String revision, int timeout) throws HgException {
        HgCommand command = new HgCommand("push", project, true);
        command
                .setUsePreferenceTimeout(MercurialPreferenceConstants.PUSH_TIMEOUT);

        if (force) {
            command.addOptions("-f");
        }

        if (revision != null && revision.length() > 0) {
            command.addOptions("-r", revision.trim());
        }

        addRepoToHgCommand(repo, command);
        return new String(command.executeToBytes(timeout));
    }

    public static String pull(IResource resource,
            HgRepositoryLocation location, boolean update) throws HgException {
        return pull(resource, location, update, false, false, null, false);
    }

    public static String pull(IResource resource, HgRepositoryLocation repo,
            boolean update, boolean force, boolean timeout,
            ChangeSet changeset, boolean rebase) throws HgException {

        URI uri = repo.getUri();
        String pullSource;
        if (uri != null) {
            pullSource = uri.toASCIIString();
        } else {
            pullSource = repo.getLocation();
        }

        return pull(resource, update, force, timeout, changeset, pullSource,
                rebase);
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
    public static String pull(IResource resource, boolean update,
            boolean force, boolean timeout, ChangeSet changeset,
            String pullSource, boolean rebase) throws HgException {
        IResource workDir = resource;
        if (resource.getType() == IResource.FILE) {
            workDir = resource.getParent();
        }
        HgCommand command = new HgCommand("pull", workDir.getLocation()
                .toFile(), true);

        if (update) {
            command.addOptions("--update");
        } else if (rebase) {
            command.addOptions("--rebase");
        }

        if (force) {
            command.addOptions("--force");
        }
        if (changeset != null) {
            command.addOptions("--rev", changeset.getChangeset());
        }

        command.addOptions(pullSource);

        if (timeout) {
            command
                    .setUsePreferenceTimeout(MercurialPreferenceConstants.PULL_TIMEOUT);
            return new String(command.executeToBytes());
        }
        return new String(command.executeToBytes(Integer.MAX_VALUE));
    }
}
