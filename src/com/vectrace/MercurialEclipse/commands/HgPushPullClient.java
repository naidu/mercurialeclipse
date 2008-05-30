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

public class HgPushPullClient extends AbstractRepositoryClient {

    public static String push(IProject project, HgRepositoryLocation location,
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

        URI uri = location.getUri();
        command.addFiles(uri.toASCIIString());
        return new String(command.executeToBytes(timeout));
    }

    public static String pull(IResource resource,
            HgRepositoryLocation location, boolean update) throws HgException {
        return pull(resource, location, update, false, false, null);
    }

    public static String pull(IResource resource,
            HgRepositoryLocation location, boolean update, boolean force,
            boolean timeout, ChangeSet changeset) throws HgException {
        IResource workDir = resource;
        if (resource.getType() == IResource.FILE) {
            workDir = resource.getParent();
        }
        HgCommand command = new HgCommand("pull", workDir.getLocation()
                .toFile(), true);

        if (update) {
            command.addOptions("--update");
        }
        if (force) {
            command.addOptions("--force");
        }
        if (changeset != null) {
            command.addOptions("--rev", changeset.getChangeset());
        }

        URI uri = location.getUri();

        command.addFiles(uri.toASCIIString());
        if (timeout) {
            command
                    .setUsePreferenceTimeout(MercurialPreferenceConstants.PULL_TIMEOUT);
            return new String(command.executeToBytes());
        }
        return new String(command.executeToBytes(Integer.MAX_VALUE));
    }
}
