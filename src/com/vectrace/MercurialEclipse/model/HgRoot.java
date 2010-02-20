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
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.utils.IniFile;

/**
 * Hg root represents the root of hg repository as <b>canonical path</b>
 * (see {@link File#getCanonicalPath()})
 *
 * @author bastian
 */
public class HgRoot extends HgPath implements IHgRepositoryLocation {
	private static final String HG_HGRC = ".hg/hgrc";
	private static final String HGENCODING;
	static {
		// next in line is HGENCODING in environment
		String enc = System.getProperty("HGENCODING");

		// next is platform encoding as available in JDK
		if (enc == null || enc.length() == 0) {
			HGENCODING = Charset.defaultCharset().name();
		} else {
			HGENCODING = enc;
		}
	}

	private static final long serialVersionUID = 2L;
	private Charset encoding;
	private Charset fallbackencoding;
	private File config;

	public HgRoot(String pathname) throws IOException {
		super(pathname);
	}

	public HgRoot(File file) throws IOException {
		super(file);
	}

	public void setEncoding(Charset charset) {
		this.encoding = charset;
	}

	public Charset getEncoding() {
		if(encoding == null){
			setEncoding(Charset.forName(HGENCODING));
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
		return null;
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
}
