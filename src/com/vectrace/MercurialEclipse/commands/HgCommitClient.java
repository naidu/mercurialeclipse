/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *     Bastian Doetsch
 *     StefanC
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;

import com.vectrace.MercurialEclipse.dialogs.Messages;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.cache.RefreshJob;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class HgCommitClient extends AbstractClient {

    /**
     * Commit given resources and refresh the caches for the assotiated projects
     */
    public static void commitResources(List<IResource> resources, String user,
            String message, IProgressMonitor monitor) throws HgException {

        Map<HgRoot, List<IResource>> resourcesByRoot = ResourceUtils.groupByRoot(resources);

        for (Map.Entry<HgRoot, List<IResource>> mapEntry : resourcesByRoot.entrySet()) {
            HgRoot root = mapEntry.getKey();
            if (monitor != null) {
                if (monitor.isCanceled()) {
                    break;
                }
                monitor.subTask(Messages.getString("HgCommitClient.commitJob.committing") + root.getName()); //$NON-NLS-1$
            }
            List<IResource> files = mapEntry.getValue();
            commit(root, AbstractClient.toFiles(files), user, message);
        }
        Map<IProject, List<IResource>> byProject = ResourceUtils.groupByProject(resources);
        Set<IProject> keySet = byProject.keySet();
        for (IProject project : keySet) {
            new RefreshJob("Refreshing " + project.getName(), project, RefreshJob.LOCAL_AND_OUTGOING).schedule();
        }
    }

    /**
     * Preforms commit. No refresh of any cashes is done afterwards.
     *
     * <b>Note</b> clients should not use this method directly, it is NOT private
     *  for tests only
     */
    protected static String commit(HgRoot root, List<File> files, String user,
            String message) throws HgException {

        HgCommand command = new HgCommand("commit", root, true); //$NON-NLS-1$
        command.setUsePreferenceTimeout(MercurialPreferenceConstants.COMMIT_TIMEOUT);
        command.addUserName(quote(user));
        command.addOptions("-m", quote(message)); //$NON-NLS-1$
        command.addFiles(AbstractClient.toPaths(files));
        return command.executeToString();
    }

    static String quote(String str) {
        if (str == null || str.length() == 0) {
            return str;
        }
        return str.replaceAll("\"", "\\\\\""); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Commit given project after the merge and refresh the caches.
     * Implementation note: after merge, no files should be specified.
     */
    public static String commitProject(IProject project, String user,
            String message) throws HgException {
        HgCommand command = new HgCommand("commit", project, true); //$NON-NLS-1$
        command.setUsePreferenceTimeout(MercurialPreferenceConstants.COMMIT_TIMEOUT);
        command.addUserName(quote(user));
        command.addOptions("-m", quote(message)); //$NON-NLS-1$
        String result = command.executeToString();
        new RefreshJob("Refreshing " + project.getName(), project, RefreshJob.LOCAL_AND_OUTGOING).schedule();
        return result;
    }

}
