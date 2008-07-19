/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian Doetsch	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.exception.HgException;

/**
 * Calls hg root
 * 
 * @author bastian
 * 
 */
public class HgRootClient {
    public static String getHgRoot(IResource resource) throws HgException {
        return getHgRoot2(resource);
        // HgCommand command = new HgCommand("root", proj, true);
        // return new
        // String(command.executeToBytes(Integer.MAX_VALUE)).replaceAll("\n",
        // "");
    }

    private static String getHgRoot2(IResource resource) throws HgException {
        File root = getHgRootAsFile(resource);
        try {
            return root.getCanonicalPath();
        } catch (IOException e) {
            throw new HgException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * @param resource
     * @return
     * @throws HgException
     */
    public static File getHgRootAsFile(IResource resource) throws HgException {
        File root = resource.getLocation().toFile();
        root = getHgRoot(root);
        return root;
    }

    /**
     * @param root
     * @return
     * @throws HgException
     */
    public static File getHgRoot(File file) throws HgException {
        File root = file;
        if (root.isFile()) {
            root = root.getParentFile();
        }

        FilenameFilter hg = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.equalsIgnoreCase(".hg");
            }
        };

        if (root != null) {
            String[] rootContent = root.list(hg);
            while (rootContent != null && rootContent.length < 1) {
                root = root.getParentFile();
                if (root == null) {
                    break;
                }
                rootContent = root.list(hg);
            }
        }
        if (root == null) {
            throw new HgException(file.getName() + " does not have a hg root");
        }
        return root;
    }

}
