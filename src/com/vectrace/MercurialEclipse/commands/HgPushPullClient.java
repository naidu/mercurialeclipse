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
import java.net.URL;

import org.eclipse.core.resources.IProject;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

public class HgPushPullClient {

    public static String push(IProject project, HgRepositoryLocation location,
            String user, String pass, boolean force, String revision)
            throws HgException {
        try {
            HgCommand command = new HgCommand("push", project, true);
            URL url = location.getUrlObject();

            if (force) {
                command.addOptions("-f");
            }

            if (revision != null && revision.length() > 0) {
                command.addOptions("-r", revision.trim());
            }

            String userInfo = user;
            if (user != null && user.length() == 0) {
                // URI parts are undefinied, if they are null.
                userInfo = null;
            } else if (user != null) {
                // pass gotta be separated by a colon
                if (pass != null && pass.length() != 0) {
                    userInfo = userInfo.concat(":").concat(pass);
                }
            }

            URI uri = new URI(url.getProtocol(), userInfo, url.getHost(), url
                    .getPort(), url.getPath(), null, null);

            command.addFiles(uri.toASCIIString());
            return command.executeToString();
        } catch (URISyntaxException e) {
            throw new HgException("URI invalid", e);
        }
    }

    public static String pull(IProject project, HgRepositoryLocation location,
            boolean update) throws HgException {
        HgCommand command = new HgCommand("pull", project, true);
        if (update) {
            command.addOptions("-u");
        }
        command.addFiles(location.getUrl());
        return command.executeToString();
    }

}
