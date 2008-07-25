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
public class HgQPushClient extends AbstractClient {
    public static String pushAll(IResource resource, boolean force)
            throws HgException {
        HgCommand command = new HgCommand("qpush",
                getWorkingDirectory(resource), true);
        
        command.addOptions("--config", "extensions.hgext.mq=");
        
        command.addOptions("-a");
        if (force) {
            command.addOptions("--force");
        }
        return command.executeToString();
    }

    public static String push(IResource resource, boolean force, String patchName)
            throws HgException {
        HgCommand command = new HgCommand("qpush",
                getWorkingDirectory(resource), true);

        command.addOptions("--config", "extensions.hgext.mq=");
        
        if (force) {
            command.addOptions("--force");
        }
        
        command.addOptions(patchName);
        return command.executeToString();
    }
}
