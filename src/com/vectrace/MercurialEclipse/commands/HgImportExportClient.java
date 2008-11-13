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

import java.io.File;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import com.vectrace.MercurialEclipse.exception.HgException;

public class HgImportExportClient {

    public static String importPatch(IProject project, String patchLocation)
            throws HgException {
        HgCommand command = new HgCommand("import", project, true);
        command.addFiles(patchLocation);
        return command.executeToString();
    }

    public static boolean exportPatch(List<IResource> resources, File patchFile)
            throws HgException {
        HgCommand command = new HgCommand("diff", ResourcesPlugin
                .getWorkspace().getRoot(), true);
        command.addFiles(resources);
        return command.executeToFile(patchFile, 0, false);
    }

    public static boolean exportPatch(List<IResource> resources)
            throws HgException {
        HgCommand command = new HgCommand("diff", ResourcesPlugin
                .getWorkspace().getRoot(), true);
        command.addFiles(resources);
        // String result = command.executeToString();
        // TODO Clip.copy
        return true;
    }
}
