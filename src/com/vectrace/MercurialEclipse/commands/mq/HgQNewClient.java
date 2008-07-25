/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands.mq;

import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.commands.HgCommand;
import com.vectrace.MercurialEclipse.exception.HgException;

/**
 * @author bastian
 * 
 */
public class HgQNewClient extends AbstractClient {
    public static String createNewPatch(IResource resource,
            String commitMessage, boolean force, boolean git, String include,
            String exclude, String user, String date, String patchName)
            throws HgException {
        HgCommand command = new HgCommand("qnew",
                getWorkingDirectory(resource), true);

        command.addOptions("--config", "extensions.hgext.mq=");
        
        if (commitMessage != null && commitMessage.length() > 0) {
            command.addOptions("--message", commitMessage);
        }
        if (force) {
            command.addOptions("--force");
        }
        if (git) {
            command.addOptions("--git");
        }
        if (include != null && include.length() > 0) {
            command.addOptions("--include", include);
        }
        if (exclude != null && exclude.length() > 0) {
            command.addOptions("--exclude", exclude);
        }
        if (user != null && user.length() > 0) {
            command.addOptions("--user", user);
        } else {
            command.addOptions("--currentuser");
        }

        if (date != null && date.length() > 0) {
            command.addOptions("--date", date);
        } else {
            command.addOptions("--currentdate");
        }

        command.addOptions(patchName);

        return command.executeToString();
    }
}
