/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *     Bastian Doetsch           - added authentication to push
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.resources.IProject;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

public class HgPushPullClient extends AbstractRepositoryClient {

    public static String push(IProject project, HgRepositoryLocation location,
            String user, String pass, boolean force, String revision, int timeout)
            throws HgException {
        try {
            HgCommand command = new HgCommand("push", project, true);
            command.setUsePreferenceTimeout(MercurialPreferenceConstants.PUSH_TIMEOUT);
            

            if (force) {
                command.addOptions("-f");
            }

            if (revision != null && revision.length() > 0) {
                command.addOptions("-r", revision.trim());
            }

            URI uri = getRepositoryURI(location, user, pass);
            command.addFiles(uri.toASCIIString());            
            return new String(command.executeToBytes(timeout));
        } catch (URISyntaxException e) {
            throw new HgException("URI invalid", e);
        }
    }

    public static String pull(IProject project, HgRepositoryLocation location,
            boolean update) throws HgException {
        try {
            HgCommand command = new HgCommand("pull", project, true);
            command.setUsePreferenceTimeout(MercurialPreferenceConstants.PULL_TIMEOUT);
            if (update) {
                command.addOptions("-u");
            }
            URI uri = getRepositoryURI(location, location.getUser(), location.getPassword());
            command.addFiles(uri.toASCIIString());
            return command.executeToString();
        } catch (URISyntaxException e) {
           throw new HgException("URI invalid",e);
        }
    }    
}
