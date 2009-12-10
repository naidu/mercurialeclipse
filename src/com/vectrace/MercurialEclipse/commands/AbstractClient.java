/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	   Bastian	implementation
 *     Andrei Loskutov (Intland) - bug fixes
 *     Adam Berkes (Intland)     - bug fixes
 *     Philip Graf               - proxy support
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * Base client class
 * @author bastian
 *
 */
public abstract class AbstractClient {

	public AbstractClient() {
		super();
	}

	protected static File getWorkingDirectory(IResource resource) {
		File file = ResourceUtils.getFileHandle(resource);
		return getWorkingDirectory(file);
	}

	protected static File getWorkingDirectory(IPath path) {
		return getWorkingDirectory(path.toFile());
	}

	protected static File getWorkingDirectory(File file) {
		return ResourceUtils.getFirstExistingDirectory(file);
	}

	/**
	 * @param resource
	 * @return hg root as <b>canonical file</b> (see {@link File#getCanonicalFile()})
	 * @throws HgException
	 */
	public static HgRoot getHgRoot(IResource resource) throws HgException {
		File file = ResourceUtils.getFileHandle(resource);
		return HgRootClient.getHgRoot(file);
	}

	public static HgRoot getHgRoot(IPath path) throws HgException {
		return getHgRoot(path.toFile());
	}

	public static HgRoot getHgRoot(File file) throws HgException {
		Assert.isNotNull(file);
		return HgRootClient.getHgRoot(file);
	}

	static List<File> toFiles(List<IResource> files) {
		List<File> toFiles = new ArrayList<File>();
		for (IResource r : files) {
			toFiles.add(r.getLocation().toFile());
		}
		return toFiles;
	}

	static List<String> toPaths(List<File> files) {
		List<String> paths = new ArrayList<String>();
		for (File f : files) {
			paths.add(f.getAbsolutePath());
		}
		return paths;
	}

	/**
	 * Checks whether a command is available in installed Mercurial version by
	 * issuing hg help <commandName>. If Mercurial doesn't answer with
	 * "hg: unknown command", it's available
	 *
	 * @param commandName
	 *            the name of the command, e.g. "rebase"
	 * @param extensionEnabler
	 *            the enablement string for an extension, e.g.
	 *            "hgext.bookmarks="
	 * @return true, if command is available
	 */
	public static boolean isCommandAvailable(String commandName,
			String extensionEnabler) {
		boolean returnValue = false;
		// see bug http://bitbucket.org/mercurialeclipse/main/issue/224/
		// If hg command uses non-null directory, which is NOT under the hg control,
		// MercurialTeamProvider.getAndStoreHgRoot() throws an exception
		AbstractShellCommand command = new HgCommand("help", (File)null, false);
		if (extensionEnabler != null && extensionEnabler.length() != 0) {
			command.addOptions("--config", "extensions." + extensionEnabler); //$NON-NLS-1$ //$NON-NLS-2$
		}
		command.addOptions(commandName);
		String result;
		try {
			result = new String(command.executeToBytes(10000, false));
			if (result.startsWith("hg: unknown command")) { //$NON-NLS-1$
				returnValue = false;
			} else {
				returnValue = true;
			}
		} catch (HgException e) {
			returnValue = false;
		}
		return returnValue;
	}

	/**
	 * Add the proxy-aware repository location to the command
	 * @param repo not null
	 * @param cmd  not null
	 * @throws HgException
	 */
	protected static void addRepoToHgCommand(HgRepositoryLocation repo, AbstractShellCommand cmd) throws HgException {
		URI uri = repo.getUri();
		String location;
		if (uri != null) {
			location = uri.toASCIIString();
			addProxyToHgCommand(uri, cmd);
		} else {
			location = repo.getLocation();
		}
		cmd.addOptions(location);
	}

	/**
	 * If a proxy server is necessary to access the pull or push location, this method will add the
	 * respective command options.
	 *
	 * @param repository
	 *            The URI of the repository, never null
	 * @param command
	 *            not null
	 */
	protected static void addProxyToHgCommand(URI repository, AbstractShellCommand command) {
		IProxyService proxyService = MercurialEclipsePlugin.getDefault().getProxyService();
		if (proxyService != null) {
			// check if there is an applicable proxy for the location
			IProxyData[] proxies = proxyService.getProxyDataForHost(repository.getHost());

			if (proxies.length > 0) {
				// there is at least one applicable proxy, use the first
				IProxyData proxy = proxies[0];

				if (proxy.getHost() != null) {
					// set the host incl. port
					command.addOptions("--config", "http_proxy.host=" + getProxyHost(proxy)); //$NON-NLS-1$ //$NON-NLS-2$

					// check if authentication is required
					if (proxy.isRequiresAuthentication()) {

						// set the user name if available
						if (proxy.getUserId() != null) {
							command.addOptions("--config", "http_proxy.user=" + proxy.getUserId()); //$NON-NLS-1$ //$NON-NLS-2$
						}

						// set the password if available
						if (proxy.getPassword() != null) {
							command.addOptions("--config", "http_proxy.passwd=" //$NON-NLS-1$ //$NON-NLS-2$
									+ proxy.getPassword());
						}
					}
				}
			}
		}
	}

	/**
	 * Returns the proxy host parameter for the {@code http_proxy.host} configuration.
	 *
	 * @param proxy
	 *            The proxy data.
	 * @return The proxy host.
	 */
	private static String getProxyHost(IProxyData proxy) {
		StringBuilder host = new StringBuilder(proxy.getHost());
		if (proxy.getPort() >= 0) {
			host.append(':').append(proxy.getPort());
		}
		return host.toString();
	}
}
