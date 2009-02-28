/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *     Steeven Lee               - import/export stuff
 *     Bastian Doetsch           - additions
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import org.eclipse.compare.patch.ApplyPatchOperation;
import org.eclipse.compare.patch.IFilePatch;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import com.vectrace.MercurialEclipse.exception.HgException;

public class HgPatchClient extends AbstractClient {

    public static String importPatch(IProject project, File patchLocation,
            ArrayList<String> options) throws HgException {
        assert patchLocation != null && options != null;
        HgCommand command = new HgCommand("import", project, true); //$NON-NLS-1$
        command.addFiles(patchLocation.getAbsolutePath());
        command.addOptions(options.toArray(new String[options.size()]));
        return command.executeToString();
    }

    public static boolean exportPatch(File workDir, List<IResource> resources,
            File patchFile, ArrayList<String> options) throws HgException {
        HgCommand command = new HgCommand(
                "diff", getWorkingDirectory(workDir), true); //$NON-NLS-1$
        command.addFiles(resources);
        command.addOptions(options.toArray(new String[options.size()]));
        return command.executeToFile(patchFile, 0, false);
    }

    /**
     * export diff file to clipboard
     * 
     * @param resources
     * @return
     * @throws HgException
     */
    public static String exportPatch(File workDir, List<IResource> resources,
            ArrayList<String> options) throws HgException {
        HgCommand command = new HgCommand(
                "diff", getWorkingDirectory(workDir), true); //$NON-NLS-1$                
        command.addFiles(resources);
        command.addOptions(options.toArray(new String[options.size()]));
        return command.executeToString();
    }
    
    public static String getDiff(File workDir) throws HgException {
        HgCommand command = new HgCommand(
                "diff", getWorkingDirectory(workDir), true); //$NON-NLS-1$ 
        return command.executeToString();
    }

    /**
     * @param outgoingPatch
     * @return
     * @throws HgException
     */
    public static IFilePatch[] getFilePatches(String outgoingPatch)
            throws HgException {
        if (outgoingPatch == null) {
            return new IFilePatch[0];
        }
        Matcher matcher = HgOutgoingClient.DIFF_START_PATTERN.matcher(outgoingPatch);
        if (matcher.find()) {
            final String strippedPatch = outgoingPatch.substring(matcher.start(),
                    outgoingPatch.length());
            try {
                return HgPatchClient.createPatches(strippedPatch);
            } catch (CoreException e) {
                throw new HgException(e);
            }
        }
        return new IFilePatch[0];
    }
    
    public IFilePatch[] getFilePatchesFromDiff(File file) throws HgException {
        HgCommand command = new HgCommand(
                "diff", getWorkingDirectory(getWorkingDirectory(file)), true); //$NON-NLS-1$         
        String patchString = command.executeToString();
        return getFilePatches(patchString);
    }

    public static IFilePatch[] createPatches(final String patch)
            throws CoreException {
        return ApplyPatchOperation.parsePatch(new IStorage() {
            public InputStream getContents() throws CoreException {
                return new ByteArrayInputStream(patch.getBytes());
            }
    
            public IPath getFullPath() {
                return null;
            }
    
            public String getName() {
                return null;
            }
    
            public boolean isReadOnly() {
                return true;
            }
    
            public Object getAdapter(
                    @SuppressWarnings("unchecked") Class adapter) {
                return null;
            }
        });
    }
}
