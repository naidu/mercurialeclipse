/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch              - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.FlaggedAdaptable;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.ResourceProperties;

public class HgResolveClient extends AbstractClient {

    /**
     * List merge state of files after merge
     * 
     * @param res
     * @return
     * @throws HgException
     */
    public static List<FlaggedAdaptable> list(IResource res) throws HgException {
        HgCommand command = new HgCommand("resolve", getWorkingDirectory(res), //$NON-NLS-1$
                false);
        command
                .setUsePreferenceTimeout(MercurialPreferenceConstants.IMERGE_TIMEOUT);
        command.addOptions("-l"); //$NON-NLS-1$
        String[] lines = command.executeToString().split("\n"); //$NON-NLS-1$
        List<FlaggedAdaptable> result = new ArrayList<FlaggedAdaptable>();
        if (lines.length != 1 || !"".equals(lines[0])) { //$NON-NLS-1$
            for (String line : lines) {
                IFile iFile = res.getProject().getFile(line.substring(2));
                FlaggedAdaptable fa = new FlaggedAdaptable(iFile, line
                        .charAt(0));
                result.add(fa);
            }
        }
        return result;
    }

    /**
     * Mark a resource as resolved ("R")
     * 
     * @param res
     * @return
     * @throws HgException
     */
    public static String markResolved(File file) throws HgException {
        try {
            HgCommand command = new HgCommand("resolve", //$NON-NLS-1$
                    getWorkingDirectory(file), false);
            command
                    .setUsePreferenceTimeout(MercurialPreferenceConstants.IMERGE_TIMEOUT);
            command.addOptions("-m", file.getCanonicalPath()); //$NON-NLS-1$
            return command.executeToString();
        } catch (IOException e) {
            MercurialEclipsePlugin.logError(e);
            throw new HgException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Try to resolve all unresolved files
     * 
     * @param file
     * @return
     * @throws HgException
     */
    public static String resolveAll(File file) throws HgException {
        HgCommand command = new HgCommand("resolve", getWorkingDirectory(file), //$NON-NLS-1$
                false);
        
        boolean useExternalMergeTool = Boolean.valueOf(
                HgClients.getPreference(
                        MercurialPreferenceConstants.PREF_USE_EXTERNAL_MERGE,
                        "false")).booleanValue(); //$NON-NLS-1$
        if (!useExternalMergeTool) {
            // we use an non-existent UI Merge tool, so no tool is started. We
            // need this option, though, as we still want the Mercurial merge to
            // take place.
            command.addOptions("--config", "ui.merge=MercurialEclipse"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        command
                .setUsePreferenceTimeout(MercurialPreferenceConstants.IMERGE_TIMEOUT);
        return command.executeToString();

    }

    /**
     * Mark a resource as unresolved ("U")
     * 
     * @param res
     * @return
     * @throws HgException
     */
    public static String markUnresolved(File file) throws HgException {
        try {
            HgCommand command = new HgCommand("resolve", //$NON-NLS-1$
                    getWorkingDirectory(file), false);
            command
                    .setUsePreferenceTimeout(MercurialPreferenceConstants.IMERGE_TIMEOUT);
            command.addOptions("-u", file.getCanonicalPath()); //$NON-NLS-1$
            return command.executeToString();
        } catch (IOException e) {
            MercurialEclipsePlugin.logError(e);
            throw new HgException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Checks whether hg resolve is supported. The result is stored in a session
     * property on the workspace so that the check is only called once a
     * session. Changing hg version while leaving Eclipse running results in
     * undefined behavior.
     * 
     * @return true if resolve is supported, false if not
     */
    public static boolean checkAvailable() throws HgException {
        try {
            boolean returnValue;
            IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace()
                    .getRoot();
            Object prop = workspaceRoot
                    .getSessionProperty(ResourceProperties.RESOLVE_AVAILABLE);
            if (prop != null) {
                boolean useResolve = ((Boolean) prop).booleanValue();
                returnValue = useResolve;
            } else {
                HgCommand command = new HgCommand("help", ResourcesPlugin //$NON-NLS-1$
                        .getWorkspace().getRoot(), false);
                command.addOptions("resolve"); //$NON-NLS-1$
                String result;
                try {
                    result = new String(command.executeToBytes(10000, false));
                    if (result.startsWith("hg: unknown command 'resolve'")) { //$NON-NLS-1$
                        returnValue = false;
                    } else {
                        returnValue = true;
                    }
                } catch (HgException e) {
                    returnValue = false;
                }
                workspaceRoot.setSessionProperty(
                        ResourceProperties.RESOLVE_AVAILABLE, Boolean
                                .valueOf(returnValue));
            }
            return returnValue;
        } catch (CoreException e) {
            MercurialEclipsePlugin.logError(e);
            throw new HgException(e);
        }

    }
}
