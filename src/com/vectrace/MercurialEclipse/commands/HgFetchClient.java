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
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

public class HgFetchClient {

    public static String fetch(IProject project, HgRepositoryLocation location,
            ChangeSet changeset) throws HgException {
        try {
            HgCommand command = new HgCommand("fetch", project, true);
            command.addOptions("--config", "extensions.fetch=");
            command
                    .setUsePreferenceTimeout(MercurialPreferenceConstants.PULL_TIMEOUT);

            if (changeset != null) {
                command.addOptions("--rev", changeset.getChangeset());
            }

            URL url = location.getUrlObject();

            String user = location.getUser();
            String pass = location.getPassword();
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
            return new String(command.executeToBytes());
        } catch (URISyntaxException e) {
            throw new HgException("URI invalid", e);
        }
    }

}
