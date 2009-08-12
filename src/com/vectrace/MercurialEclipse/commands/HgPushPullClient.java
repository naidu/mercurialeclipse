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

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.cache.OutgoingChangesetCache;

public class HgPushPullClient extends AbstractClient {

    public static String push(IProject project, HgRepositoryLocation repo,
            boolean force, String revision, int timeout) throws HgException {
        AbstractShellCommand command = new HgCommand("push", project, true); //$NON-NLS-1$
        command
                .setUsePreferenceTimeout(MercurialPreferenceConstants.PUSH_TIMEOUT);

        if (force) {
            command.addOptions("-f"); //$NON-NLS-1$
        }

        if (revision != null && revision.length() > 0) {
            command.addOptions("-r", revision.trim()); //$NON-NLS-1$
        }

        addRepoToHgCommand(repo, command);
        String result = new String(command.executeToBytes(timeout));

        updateAfterPush(result, project, repo);
        return result;
    }

    protected static void updateAfterPush(String result, IProject project, HgRepositoryLocation repo) throws HgException {
        if (result.length() != 0) {
            HgClients.getConsole().printMessage(result, null);
        }

        // It appears good. Stash the repo location.
        MercurialEclipsePlugin.getRepoManager().addRepoLocation(project,
                repo);

        OutgoingChangesetCache.getInstance().clear(repo);
    }

    public static String pull(IResource resource,
            HgRepositoryLocation location, boolean update) throws HgException {
        return pull(resource, null, location, update, false, false, false);
    }

    public static String pull(IResource resource, ChangeSet changeset,
            HgRepositoryLocation repo, boolean update, boolean rebase,
            boolean force, boolean timeout) throws HgException {

        URI uri = repo.getUri();
        String pullSource;
        if (uri != null) {
            pullSource = uri.toASCIIString();
        } else {
            pullSource = repo.getLocation();
        }

        return pull(resource, changeset, pullSource, update, rebase, force,
                timeout);
    }

    public static String pull(IResource resource, ChangeSet changeset,
            String pullSource, boolean update, boolean rebase,
            boolean force, boolean timeout) throws HgException {
        IResource workDir = resource;
        if (resource.getType() == IResource.FILE) {
            workDir = resource.getParent();
        }
        AbstractShellCommand command = new HgCommand("pull", workDir.getLocation() //$NON-NLS-1$
                .toFile(), true);

        if (update) {
            command.addOptions("--update"); //$NON-NLS-1$
        } else if (rebase) {
            command.addOptions("--config", "extensions.hgext.rebase="); //$NON-NLS-1$ //$NON-NLS-2$
            command.addOptions("--rebase"); //$NON-NLS-1$
        }

        if (force) {
            command.addOptions("--force"); //$NON-NLS-1$
        }
        if (changeset != null) {
            command.addOptions("--rev", changeset.getChangeset()); //$NON-NLS-1$
        }

        command.addOptions(pullSource);

        String result;
        if (timeout) {
            command
                    .setUsePreferenceTimeout(MercurialPreferenceConstants.PULL_TIMEOUT);
            result = new String(command.executeToBytes());
        } else {
            result= new String(command.executeToBytes(Integer.MAX_VALUE));
        }
        if(update) {
            new RefreshWorkspaceStatusJob(resource.getProject()).schedule();
        }
        return result;
    }
}
