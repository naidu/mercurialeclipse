/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.exception.HgException;

public class HgImportExportClient {

    public static String importPatch(IProject project, String patchLocation)
            throws HgException {
        HgCommand command = new HgCommand("import", project, true);
        command.addFiles(patchLocation);
        return command.executeToString();
    }

    public static String exportPatch(IProject project,
            List<IResource> resources, String patchLocation) throws HgException {
        HgCommand command = new HgCommand("diff", project, true);
        // TODO command.addFiles(resources);
        byte[] result = command.executeToBytes();
        if (patchLocation != null)
            saveFile(patchLocation, result);
        return new String(result);
    }

    private static void saveFile(String patchLocation, byte[] result) throws HgException {
        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(patchLocation);
            stream.write(result);
        } catch (IOException e) {
            throw new HgException(e.getLocalizedMessage(), e);
        } finally {
            if (stream != null)
                try {
                    stream.close();
                } catch (IOException e) {
                }
        }
    }
}
