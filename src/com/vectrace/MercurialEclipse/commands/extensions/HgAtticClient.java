/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands.extensions;

import java.io.File;

import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.commands.AbstractShellCommand;
import com.vectrace.MercurialEclipse.commands.HgCommand;
import com.vectrace.MercurialEclipse.exception.HgException;

/**
 * @author bastian
 * 
 */
public class HgAtticClient extends AbstractClient {

    public static String shelve(File repoFile, String commitMessage,
            boolean git, String user, String name) throws HgException {
        AbstractShellCommand cmd = new HgCommand("attic-shelve",// $NON-NLS-1$
                getWorkingDirectory(repoFile), false);

        if (commitMessage != null && commitMessage.length() > 0) {
            cmd.addOptions("-m", commitMessage);// $NON-NLS-1$
        }

        if (git) {
            cmd.addOptions("--git");// $NON-NLS-1$
        }
        if (user != null && user.length() > 0) {
            cmd.addOptions("-u", user);// $NON-NLS-1$
        }

        cmd.addOptions("--currentdate", name);// $NON-NLS-1$
        return cmd.executeToString();
    }

    public static String unshelve(File repoFile, boolean guessRenamedFiles,
            boolean delete, String name) throws HgException {
        AbstractShellCommand cmd = new HgCommand("attic-unshelve",// $NON-NLS-1$
                getWorkingDirectory(repoFile), false);

        if (guessRenamedFiles) {
            cmd.addOptions("--similarity");// $NON-NLS-1$
        }

        if (delete) {
            cmd.addOptions("--delete"); // $NON-NLS-1$
        }

        cmd.addOptions(name);
        return cmd.executeToString();
    }

    public static String refresh(File repoFile, String name) throws HgException {
        AbstractShellCommand cmd = new HgCommand("shelve",// $NON-NLS-1$
                getWorkingDirectory(repoFile), false);

        cmd.addOptions("--refresh", name);// $NON-NLS-1$
        return cmd.executeToString();
    }

}
