/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *     Andrei Loskutov (Intland) - bug fixes
 *     Martin Olsen (Schantz)  -  Synchronization of Multiple repositories
 *******************************************************************************/
package com.vectrace.MercurialEclipse.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.resource.ImageDescriptor;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.utils.IniFile;
import com.vectrace.MercurialEclipse.utils.StringUtils;

/**
 * Hg root represents the root of hg repository as <b>canonical path</b>
 * (see {@link File#getCanonicalPath()})
 *
 * @author bastian
 */
public class HgRoot extends HgPath implements IHgRepositoryLocation {
	private static final String HG_HGRC = ".hg/hgrc";

	private static final long serialVersionUID = 2L;
	private Charset encoding;
	private Charset fallbackencoding;
	private File config;
	private String user;
	private String defaultURL;

	public HgRoot(String pathname) throws IOException {
		super(pathname);
	}

	public HgRoot(File file) throws IOException {
		super(file);
	}

	public void setEncoding(Charset charset) {
		this.encoding = charset;
	}

	/**
	 * @return never null, root specific encoding (may differ from the OS default encoding)
	 */
	public Charset getEncoding() {
		if(encoding == null){
			setEncoding(MercurialEclipsePlugin.getDefaultEncoding());
		}
		return encoding;
	}

	/**
	 * Gets the resource hgrc as a {@link java.io.File}.
	 *
	 * @return the {@link java.io.File} referencing the hgrc file, <code>null</code> if it doesn't exist.
	 */
	public File getConfig() {
		if (config == null) {
			File hgrc = new File(this, HG_HGRC);
			if (hgrc.isFile()) {
				config = hgrc;
				return hgrc;
			}
		}
		return config;
	}

	public String getConfigItem(String section, String key) {
		getConfig();
		if (config != null) {
			try {
				IniFile iniFile = new IniFile(config.getAbsolutePath());
				return iniFile.getKeyValue(section, key);
			} catch (FileNotFoundException e) {
			}
		}
		return null;
	}

	public Charset getFallbackencoding() {
		if(fallbackencoding == null){
			// set fallbackencoding to windows standard codepage
			String fallback = getConfigItem("ui", "fallbackencoding");
			if (fallback == null || fallback.length() == 0) {
				fallback = "windows-1251";
			}
			fallbackencoding = Charset.forName(fallback);
		}
		return fallbackencoding;
	}

	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	public int compareTo(IHgRepositoryLocation loc) {
		if(getLocation() == null) {
			return -1;
		}
		if(loc.getLocation() == null){
			return 1;
		}
		return getLocation().compareToIgnoreCase(loc.getLocation());
	}

	public String getLocation() {
		return getAbsolutePath();
	}

	public URI getUri() throws HgException {
		return toURI();
	}

	public String getLogicalName() {
		return null;
	}

	public String getPassword() {
		return null;
	}

	public String getUser() {
		if(user == null){
			String configItem = getConfigItem("ui", "username");
			if(StringUtils.isEmpty(configItem)){
				// set to empty string to avoid multiple reads from file
				user = "";
			} else {
				user = configItem.trim();
			}
		}
		return StringUtils.isEmpty(user)? null : user;
	}

	public String getDefaultUrl() {
		if(defaultURL == null){
			String configItem = getConfigItem("paths", "default");
			if(StringUtils.isEmpty(configItem)){
				defaultURL = "";
			} else {
				defaultURL = configItem.trim();
			}
		}
		return StringUtils.isEmpty(defaultURL)? null : defaultURL;
	}

	@Override
	public Object[] getChildren(Object o) {
		IProject[] projects = MercurialTeamProvider.getKnownHgProjects(this).toArray(
				new IProject[0]);
		if (projects.length == 1) {
			if (getIPath().equals(projects[0].getLocation())) {
				return projects;
			}
		}
		return super.getChildren(o);
	}

	@Override
	public ImageDescriptor getImageDescriptor(Object object) {
		return MercurialEclipsePlugin.getImageDescriptor("root.gif");
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Object getAdapter(Class adapter) {
		if(adapter == IHgRepositoryLocation.class){
			return this;
		}
		return super.getAdapter(adapter);
	}

	public boolean isLocal() {
		return true;
	}

	public boolean isDefaultLocation(IHgRepositoryLocation location) {
		if (location instanceof HgRoot) {
			HgRoot repos = (HgRoot) location;
			if (repos.getDefaultUrl().equals(getDefaultUrl())) {
				return true;
			}
		}
		if (location instanceof HgRepositoryLocation) {
			HgRepositoryLocation repos = (HgRepositoryLocation) location;
			String defaultLocaction = getDefaultUrl();
			if(defaultLocaction.contains("@")) {
				String[] parts = defaultLocaction.split("://");
				defaultLocaction = parts[0] + "://" + parts[1].substring(parts[1].indexOf("@") + 1);
			}
			if (repos.toString().equals(defaultLocaction)) {
				return true;
			}
		}
		return false;
	}
}
