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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;

public class HgCommitClient {

    public static void commitResources(List<IResource> resources, String user,
            String message, IProgressMonitor monitor) throws HgException {

        Map<HgRoot, List<IResource>> resourcesByRoot;
        try {
            resourcesByRoot = HgCommand.groupByRoot(resources);
        } catch (IOException e) {
            throw new HgException(e.getLocalizedMessage(), e);
        }
        for (Map.Entry<HgRoot, List<IResource>> mapEntry : resourcesByRoot.entrySet()) {
            HgRoot root = mapEntry.getKey();
            if (monitor != null) {
                if (monitor.isCanceled()) {
                    break;
                }
                monitor.subTask(Messages.getString("HgCommitClient.commitJob.committing") + root.getName()); //$NON-NLS-1$
            }
            List<IResource> files = mapEntry.getValue();
            commitRepository(root, files, user, message);
        }
    }

    private static void commitRepository(HgRoot root, List<IResource> files,
            String user, String message) throws HgException {
        commit(root, AbstractClient.toFiles(files), user, message);

    }

    public static String commit(HgRoot root, List<File> files, String user,
            String message) throws HgException {

        HgCommand command = new HgCommand("commit", root, true); //$NON-NLS-1$
        command
                .setUsePreferenceTimeout(MercurialPreferenceConstants.COMMIT_TIMEOUT);
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

    public static String commitProject(IProject project, String user,
            String message) throws HgException {
        HgRoot hgroot;
        try {
            hgroot = new HgRoot(HgRootClient.getHgRoot(project));
        } catch (IOException e) {
            throw new HgException(e.getMessage(), e);
        }
        return commit(hgroot, new ArrayList<File>(), user, message);
        // HgCommand command = new HgCommand("commit", project, false);
        // command.setUsePreferenceTimeout(MercurialPreferenceConstants.
        // COMMIT_TIMEOUT);
        // command.addUserName(user);
        // command.addOptions("-m", message);
        // return new String(command.executeToBytes());
    }

}
