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
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;

public class HgStatusClient extends AbstractClient {

    public static String getStatus(IContainer root) throws HgException {
        return getStatus(root.getLocation().toFile());
    }
    public static String getStatus(File root) throws HgException {
        HgCommand command = new HgCommand("status", root, true); //$NON-NLS-1$
        // modified, added, removed, deleted, unknown, ignored, clean
        command.addOptions("-marduic"); //$NON-NLS-1$
        command.setUsePreferenceTimeout(MercurialPreferenceConstants.STATUS_TIMEOUT);
        return command.executeToString();
    }

    public static String getStatus(IResource res) throws HgException {
        HgCommand command = new HgCommand("status", getWorkingDirectory(res), //$NON-NLS-1$
                true);
        // modified, added, removed, deleted, unknown, ignored, clean
        command.addOptions("-marduic"); //$NON-NLS-1$
        command.setUsePreferenceTimeout(MercurialPreferenceConstants.STATUS_TIMEOUT);
        if (res.getType() == IResource.FILE) {
            command.addOptions(res.getName());
        }
        return command.executeToString();
    }

    public static String[] getUntrackedFiles(IContainer root)
            throws HgException {
        HgCommand command = new HgCommand("status", root, true); //$NON-NLS-1$
        command.setUsePreferenceTimeout(MercurialPreferenceConstants.STATUS_TIMEOUT);
        command.addOptions("-u", "-n"); //$NON-NLS-1$ //$NON-NLS-2$
        return command.executeToString().split("\n"); //$NON-NLS-1$
    }

    public static boolean isDirty(List<? extends IResource> resources)
            throws HgException {
        HgCommand command = new HgCommand("status", true); //$NON-NLS-1$
        command.setUsePreferenceTimeout(MercurialPreferenceConstants.STATUS_TIMEOUT);
        command.addOptions("-mard");// modified, added, removed, deleted //$NON-NLS-1$
        command.addFiles(resources);
        return command.executeToBytes().length != 0;
    }

    public static boolean isDirty(IProject project) throws HgException {
        HgCommand command = new HgCommand("status", project, true); //$NON-NLS-1$
        command.setUsePreferenceTimeout(MercurialPreferenceConstants.STATUS_TIMEOUT);
        command.addOptions("-mard");// modified, added, removed, deleted //$NON-NLS-1$
        return command.executeToBytes().length != 0;
    }
    
    public static String getMergeStatus(IResource res) throws HgException {
        HgCommand command = new HgCommand("id", getWorkingDirectory(res), true); //$NON-NLS-1$
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
    public static String getStatus(File file, List<IResource> files)
            throws HgException {
        HgCommand command = new HgCommand("status", getWorkingDirectory(file), //$NON-NLS-1$
                true);
        command.setUsePreferenceTimeout(MercurialPreferenceConstants.STATUS_TIMEOUT);
        // modified, added, removed, deleted, unknown, ignored, clean
        command.addOptions("-marduic"); //$NON-NLS-1$
        command.addFiles(files);
        return command.executeToString();
    }
    
    public static String[] getDirtyFiles(File file)
            throws HgException {
        HgCommand command = new HgCommand("status", getWorkingDirectory(file), //$NON-NLS-1$
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

}
