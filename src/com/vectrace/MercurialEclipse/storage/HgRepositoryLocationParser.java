/*******************************************************************************
 * Copyright (c) 2009 Intland.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Adam Berkes (Intland) - implementation
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.storage;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;

/**
 * Repository location line format:
 * [u|d]<dateAsLong> <len> uri <len> [e] username <len> [e] password <len> alias/id
 */
public class HgRepositoryLocationParser {

	protected static final String PART_SEPARATOR = " ";
	protected static final String SPLIT_TOKEN = "@@@";
	protected static final String ALIAS_TOKEN = "@alias@";
	protected static final String PASSWORD_TOKEN = ":";
	protected static final String PUSH_PREFIX = "u";
	protected static final String PULL_PREFIX = "d";
	protected static final String ENCRYPTED_PREFIX = "e";

	protected static HgRepositoryLocation parseLine(final String line) {
		if (line == null || line.length() < 1) {
			return null;
		}
		String repositoryLine = line;
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
			HgRepositoryAuthCrypter crypter = HgRepositoryAuthCrypterFactory.create();
			String username = parts.get(1);
			if (username.startsWith(ENCRYPTED_PREFIX + PART_SEPARATOR)) {
				username = crypter.decrypt(username.substring(2));
			}
			String password = parts.get(2);
			if (password.startsWith(ENCRYPTED_PREFIX + PART_SEPARATOR)) {
				password = crypter.decrypt(password.substring(2));
			}
			URI uri = parseLocationToURI(parts.get(0), username, password);
			HgRepositoryLocation location;
			if(uri != null) {
				location = new HgRepositoryLocation(parts.get(3),
						PUSH_PREFIX.equals(direction),
						uri);
			} else {
				location = new HgRepositoryLocation(parts.get(3),
						PUSH_PREFIX.equals(direction), parts.get(0), "", "");
			}
			location.setLastUsage(lastUsage);
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
		line.append(String.valueOf(location.getLocation().length()));
		line.append(PART_SEPARATOR);
		line.append(location.getLocation());
		line.append(PART_SEPARATOR);
		HgRepositoryAuthCrypter crypter = HgRepositoryAuthCrypterFactory.create();
		String user = location.getUser() != null ? location.getUser() : "";
		if (user.length() > 0) {
			user = ENCRYPTED_PREFIX + PART_SEPARATOR + crypter.encrypt(user);
		}
		line.append(String.valueOf(user.length()));
		line.append(PART_SEPARATOR);
		line.append(user);
		line.append(PART_SEPARATOR);
		String password = location.getPassword() != null ? location.getPassword() : "";
		if (password.length() > 0) {
			password = ENCRYPTED_PREFIX + PART_SEPARATOR + crypter.encrypt(password);
		}
		line.append(String.valueOf(password.length()));
		line.append(PART_SEPARATOR);
		line.append(password);
		line.append(PART_SEPARATOR);
		String logicalName = location.getLogicalName() != null ? location.getLogicalName() : "";
		line.append(String.valueOf(logicalName.length()));
		line.append(PART_SEPARATOR);
		line.append(logicalName);
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

		URI uri = parseLocationToURI(location, user, password);
		if (uri != null) {
			return new HgRepositoryLocation(logicalName, isPush, uri);
		}
		return new HgRepositoryLocation(logicalName, isPush, location, user, password);
	}

	protected static URI parseLocationToURI(String location, String user, String password) throws HgException {
		URI uri = null;
		try {
			uri = new URI(location);
		} catch (URISyntaxException e) {
			// Most possibly windows path, return null
			File localPath = new File(location);
			if (localPath.isDirectory()) {
				return null;
			}
			HgException hgex = new HgException("Hg repository location invalid: <" + location + ">");
			throw hgex;
		}
		if (uri.getScheme() != null
				&& !uri.getScheme().equalsIgnoreCase("file")) { //$NON-NLS-1$
			String userInfo = null;
			if (uri.getUserInfo() == null) {
				// This is a hack: ssh doesn't allow us to directly enter
				// in passwords in the URI (even though it says it does)
				if (uri.getScheme().equalsIgnoreCase("ssh")) {
					userInfo = user;
				} else {
					userInfo = createUserinfo(user, password);
				}
			} else {
				// extract user and password from given URI
				String[] authorization = uri.getUserInfo().split(":"); //$NON-NLS-1$
				user = authorization[0];
				if (authorization.length > 1) {
					password = authorization[1];
				}

				// This is a hack: ssh doesn't allow us to directly enter
				// in passwords in the URI (even though it says it does)
				if (uri.getScheme().equalsIgnoreCase("ssh")) {
					userInfo = user;
				} else {
					userInfo = createUserinfo(user, password);
				}
			}
			try {
				return new URI(uri.getScheme(), userInfo,
						uri.getHost(), uri.getPort(), uri.getPath(),
						uri.getQuery(), uri.getFragment());
			} catch (URISyntaxException ex) {
				HgException hgex = new HgException("Failed to parse hg repository: <" + location + ">", ex);
				hgex.initCause(ex);
				throw hgex;
			}
		}
		return uri;
	}

	protected static String getUserNameFromURI(URI uri) {
		String userInfo = uri != null ? uri.getUserInfo() : null;
		if (userInfo != null) {
			if (userInfo.indexOf(PASSWORD_TOKEN) > 0) {
				return userInfo.substring(0, userInfo.indexOf(PASSWORD_TOKEN));
			}
			return userInfo;
		}
		return null;
	}

	protected static String getPasswordFromURI(URI uri) {
		String userInfo = uri != null ? uri.getUserInfo() : null;
		if (userInfo != null) {
			if (userInfo.indexOf(PASSWORD_TOKEN) > 0) {
				return userInfo.substring(userInfo.indexOf(PASSWORD_TOKEN) + 1);
			}
			return userInfo;
		}
		return null;
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
		if (location.getUser() != null ) {
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
