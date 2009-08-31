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
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.AbstractShellCommand;
import com.vectrace.MercurialEclipse.commands.HgCommand;
import com.vectrace.MercurialEclipse.commands.HgPushPullClient;
import com.vectrace.MercurialEclipse.commands.RefreshWorkspaceStatusJob;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.cache.RefreshJob;

public class HgFpushPullClient extends HgPushPullClient {

    public static String fpush(File forestRoot, HgRepositoryLocation repo,
            String revision, int timeout, File snapFile) throws CoreException {

        AbstractShellCommand command = new HgCommand("fpush", forestRoot, true);
        command.setUsePreferenceTimeout(MercurialPreferenceConstants.PUSH_TIMEOUT);
        if (snapFile != null) {
            try {
                command.addOptions("--snapfile", snapFile.getCanonicalPath());
            } catch (IOException e) {
                throw new HgException(e.getLocalizedMessage(), e);
            }
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
        AbstractShellCommand command = new HgCommand("fpull", forestRoot, true);

        if (update) {
            command.addOptions("--update");
        }
        if (changeset != null) {
            command.addOptions("--rev", changeset.getChangeset());
        }

        if (snapFile != null) {
            try {
                command.addOptions("--snapfile", snapFile.getCanonicalPath());
            } catch (IOException e) {
                throw new HgException(e.getLocalizedMessage(), e);
            }
        }

        if (walkHg) {
            command.addOptions("--walkhg", "true");
        }

        if (partial) {
            command.addOptions("--partial");
        }

        command.addOptions(pullSource);

        String result;
        if (timeout) {
            command.setUsePreferenceTimeout(MercurialPreferenceConstants.PULL_TIMEOUT);
            result = new String(command.executeToBytes());
        } else {
            result = new String(command.executeToBytes(Integer.MAX_VALUE));
        }
            Set<IProject> projects = MercurialEclipsePlugin.getRepoManager().getAllRepoLocationProjects(repo);
            for (final IProject project : projects) {
                if(update) {
                    RefreshWorkspaceStatusJob job = new RefreshWorkspaceStatusJob(project);
                    job.addJobChangeListener(new JobChangeAdapter(){
                       @Override
                        public void done(IJobChangeEvent event) {
                            new RefreshJob("Refreshing " + project.getName(), project, RefreshJob.LOCAL_AND_INCOMING).schedule();
                        }
                    });
                    job.schedule();
                } else {
                    new RefreshJob("Refreshing " + project.getName(), project, RefreshJob.LOCAL_AND_INCOMING).schedule();
                }
            }
        return result;
    }
}
