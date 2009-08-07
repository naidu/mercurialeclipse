/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - initial
 *     Bastian Doetsch           - changes
 *     Brian Wallis              - getMergeStatus
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;

public class HgStatusClient extends AbstractClient {

    public static String getStatus(IContainer root) throws HgException {
        return getStatus(root.getLocation().toFile());
    }
    public static String getStatus(File root) throws HgException {
        AbstractShellCommand command = new HgCommand("status", root, true); //$NON-NLS-1$
        // modified, added, removed, deleted, unknown, ignored, clean
        command.addOptions("-marduic"); //$NON-NLS-1$
        command.setUsePreferenceTimeout(MercurialPreferenceConstants.STATUS_TIMEOUT);
        return command.executeToString();
    }

    public static String getStatusWithoutIgnored(IResource res) throws HgException {
        AbstractShellCommand command = new HgCommand("status", getWorkingDirectory(res.getProject()), //$NON-NLS-1$
                true);
        // modified, added, removed, deleted, unknown, ignored, clean
        command.addOptions("-marduc"); //$NON-NLS-1$
        command.setUsePreferenceTimeout(MercurialPreferenceConstants.STATUS_TIMEOUT);
        if (res.getType() == IResource.FILE) {
            command.addOptions(res.getLocation().toFile().getAbsolutePath());
        }
        return command.executeToString();
    }

    public static String[] getUntrackedFiles(IContainer root)
            throws HgException {
        AbstractShellCommand command = new HgCommand("status", root, true); //$NON-NLS-1$
        command.setUsePreferenceTimeout(MercurialPreferenceConstants.STATUS_TIMEOUT);
        command.addOptions("-u", "-n"); //$NON-NLS-1$ //$NON-NLS-2$
        return command.executeToString().split("\n"); //$NON-NLS-1$
    }

    public static boolean isDirty(List<? extends IResource> resources)
            throws HgException {
        AbstractShellCommand command = new HgCommand("status", true); //$NON-NLS-1$
        command.setUsePreferenceTimeout(MercurialPreferenceConstants.STATUS_TIMEOUT);
        command.addOptions("-mard");// modified, added, removed, deleted //$NON-NLS-1$
        command.addFiles(resources);
        return command.executeToBytes().length != 0;
    }

    public static boolean isDirty(IProject project) throws HgException {
        AbstractShellCommand command = new HgCommand("status", project, true); //$NON-NLS-1$
        command.setUsePreferenceTimeout(MercurialPreferenceConstants.STATUS_TIMEOUT);
        command.addOptions("-mard");// modified, added, removed, deleted //$NON-NLS-1$
        return command.executeToBytes().length != 0;
    }

    public static String getMergeStatus(IResource res) throws HgException {
        AbstractShellCommand command = new HgCommand(
                "identify", getWorkingDirectory(res), true); //$NON-NLS-1$
        // Full global IDs
        command.addOptions("-i","--debug"); //$NON-NLS-1$ //$NON-NLS-2$
        command.setUsePreferenceTimeout(MercurialPreferenceConstants.STATUS_TIMEOUT);
        String versionIds = command.executeToString().trim();

        Pattern p = Pattern.compile("^[0-9a-z]+\\+([0-9a-z]+)\\+$", Pattern.MULTILINE); //$NON-NLS-1$
        Matcher m = p.matcher(versionIds);
        if(m.matches()) {
            return m.group(1);
        }
        return null;
    }

    /**
     * @param array
     * @return
     * @throws HgException
     */
    public static String getStatusWithoutIgnored(File file, List<IResource> files)
            throws HgException {
        AbstractShellCommand command = new HgCommand("status", getWorkingDirectory(file), //$NON-NLS-1$
                true);
        command.setUsePreferenceTimeout(MercurialPreferenceConstants.STATUS_TIMEOUT);
        // modified, added, removed, deleted, unknown, ignored, clean
        command.addOptions("-marduc"); //$NON-NLS-1$
        command.addFiles(files);
        return command.executeToString();
    }

    public static String[] getDirtyFiles(File file)
            throws HgException {
        AbstractShellCommand command = new HgCommand("status", getWorkingDirectory(file), //$NON-NLS-1$
                true);
        command
                .setUsePreferenceTimeout(MercurialPreferenceConstants.STATUS_TIMEOUT);
        command.addOptions("-mard"); //$NON-NLS-1$
        String result = command.executeToString();
        if (result == null || result.length() == 0) {
            return new String[0];
        }
        return result.split("\n"); //$NON-NLS-1$
    }

    /**
     * Returns <b>possible</b> ancestor of the given file, if given file is a result of
     * a copy or rename operation.
     *
     * @param file
     *            successor path (as full absolute file path)
     * @param root
     *            hg root
     * @param firstKnownRevision
     * @return full absolute file path which was the source of the given file one changeset
     *         before given version, or null if the given file was not copied or renamed
     *         at given version.
     * @throws HgException
     */
    public static File getPossibleSourcePath(File file, HgRoot root, int firstKnownRevision) throws HgException{
        AbstractShellCommand command = new HgCommand("status", root, true); //$NON-NLS-1$
        command.setUsePreferenceTimeout(MercurialPreferenceConstants.STATUS_TIMEOUT);
        command.addOptions("-arC"); //$NON-NLS-1$
        command.addOptions("--rev"); //$NON-NLS-1$
        command.addOptions((firstKnownRevision - 1) + ":" + firstKnownRevision); //$NON-NLS-1$
//        command.addFiles(file.getPath());
        String result = command.executeToString();
        if (result == null || result.length() == 0) {
            return null;
        }

        String relativePath = root.toRelative(file);

        String[] statusAndFileNames = result.split("\n"); //$NON-NLS-1$
        for (int i = 0; i < statusAndFileNames.length; i++) {
            if(i + 1 < statusAndFileNames.length && statusAndFileNames[i].endsWith(relativePath)){
                // XXX should not just trim whitespace in the path, if it contains whitespaces, it will not work
                // on the other side it's just idiotic to have filenames with leading or trailing spaces
                return new File(root, statusAndFileNames[i + 1].trim());
            }
        }
        return null;
    }

}
