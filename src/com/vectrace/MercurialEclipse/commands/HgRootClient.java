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
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;

/**
 * Calls hg root
 *
 * @author bastian
 *
 */
public class HgRootClient {

    private final static Map<File, HgRoot> roots = new HashMap<File, HgRoot>();

    /**
     * @param resource
     * @return hg root as <b>canonical file</b> (see {@link File#getCanonicalFile()})
     * @throws HgException
     */
    public static HgRoot getHgRoot(IResource resource) throws HgException {
        return getHgRoot(resource.getLocation().toFile());
    }

    /**
     * @param file
     * @return hg root as <b>canonical path</b> (see {@link File#getCanonicalPath()})
     * @throws HgException
     */
    public static HgRoot getHgRoot(File file) throws HgException {
        // HgCommand command = new HgCommand("root", proj, true);
        // return new String(command.executeToBytes(Integer.MAX_VALUE)).replaceAll("\n", "");

        File dir = file.isFile() || !file.exists()? file.getParentFile() : file;
        // test if we have the path "as is" already
        HgRoot hgRoot = roots.get(dir);
        if(hgRoot != null){
            return hgRoot;
        }
        HgRoot testRoot;
        try {
            testRoot = new HgRoot(dir);
        } catch(IOException e) {
            throw new HgException(Messages.getString("HgRootClient.error.cannotGetCanonicalPath")+file.getName()); //$NON-NLS-1$
        }
        // test with canonical version of the same file
        hgRoot = roots.get(testRoot);
        if(hgRoot != null){
            // remember NON canonical version too
            roots.put(dir, hgRoot);
            return hgRoot;
        }
        // search up the parents recursive if we see .hg directory there
        File root = findHgDir(testRoot);
        if (root == null) {
            throw new HgException(file.getName() + Messages.getString("HgRootClient.error.noRoot")); //$NON-NLS-1$
        }
        // .hg parent dire found
        try {
            hgRoot = new HgRoot(root);
        } catch (IOException e) {
            throw new HgException(Messages.getString("HgRootClient.error.cannotGetCanonicalPath")+file.getName()); //$NON-NLS-1$
        }
        roots.put(root, hgRoot);
        return hgRoot;
    }

    /**
     * Searches the .hg directory up in all parents of the given file
     * @param startDir a directory
     * @return directory named ".hg" (in any case), or null if the given file does not
     * have parents wich contains .hg directory
     */
    private static File findHgDir(File startDir) {
        FileFilter hg = new FileFilter() {
            public boolean accept(File path) {
                return path.getName().equalsIgnoreCase(".hg") && path.isDirectory(); //$NON-NLS-1$
            }
        };
        File root = startDir;
        File[] rootContent = root.listFiles(hg);
        while (rootContent != null && rootContent.length == 0) {
            root = root.getParentFile();
            if (root == null) {
                return null;
            }
            rootContent = root.listFiles(hg);
        }
        if(rootContent != null) {
            return root;
        }
        return null;
    }

}
