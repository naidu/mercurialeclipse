/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * @author adam.berkes <adam.berkes@intland.com>
 *******************************************************************************/
package com.vectrace.MercurialEclipse.storage;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;

/**
 * Repository location line format:
 * [u|d]<dateAsLong> <len> uri <len> username <len> password <len> alias/id[ <len> project]
 */
public class HgRepositoryLocationParser {

    protected static final String PART_SEPARATOR = " ";
    protected static final String SPLIT_TOKEN = "@@@";
    protected static final String ALIAS_TOKEN = "@alias@";
    protected static final String PASSWORD_TOKEN = ":";
    protected static final String PUSH_PREFIX = "u";
    protected static final String PULL_PREFIX = "d";

    protected static HgRepositoryLocation parseLine(final String line) {
        if (line == null || line.length() < 1) {
            return null;
        }
        String repositoryLine = new String(line);
        //get direction indicator
        String direction = repositoryLine.substring(0,1);
        repositoryLine = repositoryLine.substring(1);
        try {
            //get date
            Date lastUsage = new Date(Long.valueOf(repositoryLine.substring(0, repositoryLine.indexOf(PART_SEPARATOR))).longValue());
            repositoryLine = repositoryLine.substring(repositoryLine.indexOf(PART_SEPARATOR) + 1);
            List<String> parts = new ArrayList<String>(5);
            while (repositoryLine != null && repositoryLine.length() > 0) {
                int len = Integer.valueOf(repositoryLine.substring(0, repositoryLine.indexOf(PART_SEPARATOR))).intValue();
                repositoryLine = repositoryLine.substring(repositoryLine.indexOf(PART_SEPARATOR) + 1);
                String partValue = repositoryLine.substring(0, len);
                repositoryLine = repositoryLine.substring(repositoryLine.length() > len ? len + 1 : repositoryLine.length());
                parts.add(partValue);
            }
            URI uri = null;
            try {
                // first parse url/uri and then
                // regenerate it with correct user info
                uri = new URI(parts.get(0));
                uri = new URI(uri.getScheme(),
                        createUserinfo(parts.get(1), parts.get(2)),
                        uri.getHost(),
                        uri.getPort(),
                        uri.getPath(),
                        uri.getQuery(),
                        uri.getFragment());
            } catch (URISyntaxException e) {
                // uri stays null
            }
            HgRepositoryLocation location = new HgRepositoryLocation(parts.get(3),
                    direction.equals(PUSH_PREFIX),
                    uri,
                    parts.get(0),
                    parts.get(1),
                    parts.get(2));
            location.setLastUsage(lastUsage);
            if (parts.size() > 4) {
                location.setProjectName(parts.get(4));
            }
            return location;
        } catch(Throwable th) {
            MercurialEclipsePlugin.logError(th);
            return null;
        }
    }

    protected static String createLine(final HgRepositoryLocation location) {
        StringBuilder line = new StringBuilder(location.isPush() ? PUSH_PREFIX : PULL_PREFIX);
        line.append(location.getLastUsage() != null ? location.getLastUsage().getTime() : new Date().getTime());
        line.append(PART_SEPARATOR);
        // remove authentication from location
        if (location.getUri() != null) {
            try {
                String locationUriString = new URI(location.getUri().getScheme(),
                        null,
                        location.getUri().getHost(),
                        location.getUri().getPort(),
                        location.getUri().getPath(),
                        null,
                        location.getUri().getFragment()).toASCIIString();
                line.append(String.valueOf(locationUriString.length()));
                line.append(PART_SEPARATOR);
                line.append(locationUriString);
                line.append(PART_SEPARATOR);
            } catch (URISyntaxException ex) {
                MercurialEclipsePlugin.logError(ex);
                line.append(String.valueOf(location.getLocation().length()));
                line.append(PART_SEPARATOR);
                line.append(location.getLocation());
                line.append(PART_SEPARATOR);
            }
        } else {
            line.append(String.valueOf(location.getLocation().length()));
            line.append(PART_SEPARATOR);
            line.append(location.getLocation());
            line.append(PART_SEPARATOR);
        }
        String user = location.getUser() != null ? location.getUser() : "";
        line.append(String.valueOf(user.length()));
        line.append(PART_SEPARATOR);
        line.append(user);
        line.append(PART_SEPARATOR);
        String password = location.getPassword() != null ? location.getPassword() : "";
        line.append(String.valueOf(password.length()));
        line.append(PART_SEPARATOR);
        line.append(password);
        line.append(PART_SEPARATOR);
        String logicalName = location.getLogicalName() != null ? location.getLogicalName() : "";
        line.append(String.valueOf(logicalName.length()));
        line.append(PART_SEPARATOR);
        line.append(logicalName);
        if (location.getProjectName() != null) {
            line.append(PART_SEPARATOR);
            line.append(String.valueOf(location.getProjectName().length()));
            line.append(PART_SEPARATOR);
            line.append(location.getProjectName());
        }
        return line.toString();
    }

    protected static HgRepositoryLocation parseLocation(String logicalName, boolean isPush, String location, String user, String password) throws HgException {
        return parseLine(logicalName, isPush, location, user, password);
    }

    protected static HgRepositoryLocation parseLocation(boolean isPush, String location, String user, String password) throws HgException {
        return parseLocation(null, isPush, location, user, password);
    }

    protected static HgRepositoryLocation parseLocation(boolean isPush, String location) throws HgException {
        return parseLocation(null, isPush, location, null, null);
    }

    protected static HgRepositoryLocation parseLine(String logicalName, boolean isPush, String location, String user, String password) throws HgException {
        URI locationUri = null;
        String[] repoInfo = location.split(SPLIT_TOKEN);

        if ((user == null || user.length() == 0)
                && repoInfo.length > 1) {
            String userInfo = repoInfo[1];
            if (userInfo.contains(ALIAS_TOKEN)) {
                userInfo = userInfo.substring(0, userInfo.indexOf(ALIAS_TOKEN));
            }
            String[] splitUserInfo = userInfo.split(PASSWORD_TOKEN);
            user = splitUserInfo[0];
            if (splitUserInfo.length > 1) {
                password = splitUserInfo[1];
            } else {
                password = null;
            }
            location = repoInfo[0];
        }

        String[] alias = location.split(ALIAS_TOKEN);
        if (alias.length == 2
                && (logicalName == null || logicalName.length() == 0)) {
            logicalName = alias[1];
            if (location.contains(ALIAS_TOKEN)) {
                location = location.substring(0, location.indexOf(ALIAS_TOKEN));
            }
        }

        try {
            locationUri = new URI(location);
        } catch (URISyntaxException e) {

        }
        if (locationUri != null) {
            if (locationUri.getScheme() != null
                    && !locationUri.getScheme().equalsIgnoreCase("file")) { //$NON-NLS-1$
                String userInfo = null;
                if (locationUri.getUserInfo() == null) {
                    // This is a hack: ssh doesn't allow us to directly enter
                    // in passwords in the URI (even though it says it does)
                    if (locationUri.getScheme().equalsIgnoreCase("ssh")) {
                        userInfo = user;
                    } else {
                        userInfo = createUserinfo(user, password);
                    }

                } else {
                    // extract user and password from given URI
                    String[] authorization = locationUri.getUserInfo().split(":"); //$NON-NLS-1$
                    user = authorization[0];
                    if (authorization.length > 1) {
                        password = authorization[1];
                    }

                    // This is a hack: ssh doesn't allow us to directly enter
                    // in passwords in the URI (even though it says it does)
                    if (locationUri.getScheme().equalsIgnoreCase("ssh")) {
                        userInfo = user;
                    } else {
                        userInfo = createUserinfo(user, password);
                    }
                }
                try {
                    locationUri = new URI(locationUri.getScheme(), userInfo,
                            locationUri.getHost(), locationUri.getPort(), locationUri.getPath(),
                            locationUri.getQuery(), locationUri.getFragment());
                } catch (URISyntaxException e) {
                    HgException hgex = new HgException("Failed to create hg repository", e);
                    hgex.initCause(e);
                    throw hgex;
                }
            }
        }
        HgRepositoryLocation repo = new HgRepositoryLocation(logicalName, isPush, locationUri, location, user, password);
        return repo;
    }

    private static String createUserinfo(String user1, String password1) {
        String userInfo = null;
        if (user1 != null && user1.length() > 0) {
            // pass gotta be separated by a colon
            if (password1 != null && password1.length() != 0) {
                userInfo = user1 + PASSWORD_TOKEN + password1;
            } else {
                userInfo = user1;
            }
        }
        return userInfo;
    }

    @Deprecated
    public static String createSaveString(HgRepositoryLocation location) {
        StringBuilder line = new StringBuilder(location.getLocation());
        if (location.getUri() != null && location.getUri().getUserInfo() != null) {
            line.append(SPLIT_TOKEN);
            line.append(location.getUri().getUserInfo());
        } else if (location.getUser() != null ) {
            line.append(SPLIT_TOKEN);
            line.append(location.getUser());
            if (location.getPassword() != null) {
                line.append(PASSWORD_TOKEN);
                line.append(location.getPassword());
            }
        }
        if (location.getLogicalName() != null && location.getLogicalName().length() > 0) {
            line.append(ALIAS_TOKEN);
            line.append(location.getLogicalName());
        }
        return line.toString();
    }
}
