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
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.exception.HgException;

/**
 * Calls hg root
 *
 * @author bastian
 *
 */
public class HgRootClient {

    private final static Map<String, File> roots = new HashMap<String, File>();

    /**
     * @param resource
     * @return hg root as <b>canonical path</b> (see {@link File#getCanonicalPath()})
     * @throws HgException
     */
    public static String getHgRoot(IResource resource) throws HgException {
        File root = getHgRootAsFile(resource);
        try {
            return root.getCanonicalPath();
        } catch (IOException e) {
            throw new HgException(e.getLocalizedMessage(), e);
        }
        // HgCommand command = new HgCommand("root", proj, true);
        // return new
        // String(command.executeToBytes(Integer.MAX_VALUE)).replaceAll("\n",
        // "");
    }

    /**
     * @param resource
     * @return hg root as <b>canonical file</b> (see {@link File#getCanonicalFile()})
     * @throws HgException
     */
    public static File getHgRootAsFile(IResource resource) throws HgException {
        File root = resource.getLocation().toFile();
        root = getHgRoot(root);
        return root;
    }

    /**
     * @param file
     * @return hg root as <b>canonical path</b> (see {@link File#getCanonicalPath()})
     * @throws HgException
     */
    public static File getHgRoot(File file) throws HgException {
        String canonicalPath;
        try {
            if(file.isFile()) {
                canonicalPath = file.getParentFile().getCanonicalPath();
            } else {
                canonicalPath = file.getCanonicalPath();
            }
        } catch(IOException e) {
            throw new HgException(Messages.getString("HgRootClient.error.cannotGetCanonicalPath")+file.getName()); //$NON-NLS-1$
        }
        if(roots.containsKey(canonicalPath)) {
            return roots.get(canonicalPath);
        }
        File root = new File(canonicalPath);

        FilenameFilter hg = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.equalsIgnoreCase(".hg"); //$NON-NLS-1$
            }
        };

        String[] rootContent = root.list(hg);
        while (rootContent != null && rootContent.length == 0) {
            root = root.getParentFile();
            if (root == null) {
                break;
            }
            rootContent = root.list(hg);
        }
        if (root == null) {
            throw new HgException(file.getName() + Messages.getString("HgRootClient.error.noRoot")); //$NON-NLS-1$
        }
        roots.put(canonicalPath, root);
        return root;
    }

}
