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
package com.vectrace.MercurialEclipse.commands;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

/**
 * @author bastian
 *
 */
public class AbstractRepositoryClient extends AbstractClient {

    /**
     * @param location
     * @param user
     * @param pass
     * @return
     * @throws URISyntaxException
     */
    protected static URI getRepositoryURI(HgRepositoryLocation location, String user,
            String pass) throws URISyntaxException {
                URL url = location.getUrlObject();
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
                return uri;
            }

}
