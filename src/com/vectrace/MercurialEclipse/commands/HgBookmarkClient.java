/*******************************************************************************
 * Copyright (c) 2008 Bastian Doetsch and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.Bookmark;

/**
 * @author bastian
 * 
 */
public class HgBookmarkClient extends AbstractClient {

    /**
     * @param file
     *            file or directory within repo
     * @return a List of bookmarks
     * @throws HgException
     */
    public static List<Bookmark> getBookmarks(File file) throws HgException {
        HgCommand cmd = new HgCommand("bookmarks", getWorkingDirectory(file), //$NON-NLS-1$
                true);
        cmd.addOptions("--config", "extensions.hgext.bookmarks="); //$NON-NLS-1$ //$NON-NLS-2$
        String result = cmd.executeToString();
        ArrayList<Bookmark> bookmarks = convert(result);
        return bookmarks;
    }

    /**
     * @param result
     * @return
     */
    private static ArrayList<Bookmark> convert(String result) {
        ArrayList<Bookmark> bookmarks = new ArrayList<Bookmark>();
        if (!result.startsWith("no bookmarks set")) { //$NON-NLS-1$
            String[] split = result.split("\n"); //$NON-NLS-1$
            for (String string : split) {
                bookmarks.add(new Bookmark(string));
            }
        }
        return bookmarks;
    }

    /**
     * @param file
     * @param text
     * @param targetRev
     * @throws HgException
     */
    public static String create(File file, String name, String targetChangeset)
            throws HgException {
        HgCommand cmd = new HgCommand("bookmarks", getWorkingDirectory(file), //$NON-NLS-1$
                true);
        cmd.addOptions("--config", "extensions.hgext.bookmarks="); //$NON-NLS-1$ //$NON-NLS-2$
        cmd.addOptions("--rev", targetChangeset, name); //$NON-NLS-1$
        String result = cmd.executeToString();
        return result;
    }
    
    /**
     * @param file
     * @param text
     * @param targetRev
     * @throws HgException
     */
    public static String rename(File file, String name, String newName)
            throws HgException {
        HgCommand cmd = new HgCommand("bookmarks", getWorkingDirectory(file), //$NON-NLS-1$
                true);
        cmd.addOptions("--config", "extensions.hgext.bookmarks="); //$NON-NLS-1$ //$NON-NLS-2$
        cmd.addOptions("--rename", name, newName); //$NON-NLS-1$
        String result = cmd.executeToString();
        return result;
    }
    
    /**
     * @param file
     * @param text
     * @param targetRev
     * @throws HgException
     */
    public static String delete(File file, String name)
            throws HgException {
        HgCommand cmd = new HgCommand("bookmarks", getWorkingDirectory(file), //$NON-NLS-1$
                true);
        cmd.addOptions("--config", "extensions.hgext.bookmarks="); //$NON-NLS-1$ //$NON-NLS-2$
        cmd.addOptions("--delete", name); //$NON-NLS-1$
        String result = cmd.executeToString();
        return result;
    }

}
