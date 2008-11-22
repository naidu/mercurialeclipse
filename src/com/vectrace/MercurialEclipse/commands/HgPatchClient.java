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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;

public class HgPatchClient extends AbstractClient {

    public static String importPatch(IProject project, File patchLocation)
            throws HgException {
        HgCommand command = new HgCommand("import", project, true); //$NON-NLS-1$
        command.addFiles(patchLocation.getAbsolutePath());
        command.addOptions("--no-commit"); //$NON-NLS-1$
        return command.executeToString();
    }

    /**
     * import diff from clipboard<br>
     * TODO export stream direct to hg process inputstream
     * 
     * @param project
     * @return
     * @throws HgException
     */
    public static String importPatch(IProject project) throws HgException {
        File file = null;
        String txt = getClipboardString();
        if (txt == null || txt.trim().length() == 0) {
            return null;
        }
        try {
            file = File.createTempFile("mercurial_", ".patch"); //$NON-NLS-1$ //$NON-NLS-2$
            FileWriter w = new FileWriter(file);
            w.write(txt);
            w.close();
            return importPatch(project, file);
        } catch (IOException e) {
            throw new HgException(Messages
                    .getString("HgPatchClient.error.writeTempFile"), e); //$NON-NLS-1$
        } finally {
            if (file != null) {
                file.delete();
            }
        }
    }

    public static boolean exportPatch(File workDir, List<IResource> resources, File patchFile)
            throws HgException {
        HgCommand command = new HgCommand("diff", getWorkingDirectory(workDir), true); //$NON-NLS-1$
        command.addFiles(resources);
        return command.executeToFile(patchFile, 0, false);
    }

    /**
     * export diff file to clipboard
     * 
     * @param resources
     * @throws HgException
     */
    public static void exportPatch(File workDir, List<IResource> resources)
            throws HgException {
        HgCommand command = new HgCommand(
                "diff", getWorkingDirectory(workDir), true); //$NON-NLS-1$                
        command.addFiles(resources);
        String result = command.executeToString();
        copyToClipboard(result);
    }

    private static void copyToClipboard(final String result) {
        if (MercurialEclipsePlugin.getStandardDisplay().getThread() == Thread
                .currentThread()) {
            Clipboard cb = new Clipboard(MercurialEclipsePlugin
                    .getStandardDisplay());
            cb.setContents(new Object[] { result },
                    new Transfer[] { TextTransfer.getInstance() });
            cb.dispose();
            return;
        }
        MercurialEclipsePlugin.getStandardDisplay().syncExec(new Runnable() {
            public void run() {
                copyToClipboard(result);
            }
        });
    }

    private static String getClipboardString() {
        if (MercurialEclipsePlugin.getStandardDisplay().getThread() == Thread
                .currentThread()) {
            Clipboard cb = new Clipboard(MercurialEclipsePlugin
                    .getStandardDisplay());
            String result = (String) cb.getContents(TextTransfer.getInstance());
            cb.dispose();
            return result;
        }
        final String[] r = { null };
        MercurialEclipsePlugin.getStandardDisplay().syncExec(new Runnable() {
            public void run() {
                r[0] = getClipboardString();
            }
        });
        return r[0];
    }
}
