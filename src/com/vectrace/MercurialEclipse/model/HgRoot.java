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
import java.nio.charset.Charset;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.utils.IniFile;

/**
 * Hg root represents the root of hg repository as <b>canonical path</b>
 * (see {@link File#getCanonicalPath()})
 *
 * @author bastian
 */
public class HgRoot extends File {
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
	private final Path path;

	public HgRoot(String pathname) throws IOException {
		this(new File(pathname));
	}

	public HgRoot(File file) throws IOException {
		super(file.getCanonicalPath());
		path = new Path(getAbsolutePath());
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

	/**
	 * Converts given path to the relative
	 * @param child a possible child path
	 * @return a hg root relative path of a given file, if the given file is located under this root,
	 * otherwise the path of a given file
	 */
	public String toRelative(File child){
		// first try with the unresolved path. In most cases it's enough
		String fullPath = child.getAbsolutePath();
		if(!fullPath.startsWith(getPath())){
			try {
				// ok, now try to resolve all the links etc. this takes A LOT of time...
				fullPath = child.getCanonicalPath();
				if(!fullPath.startsWith(getPath())){
					return child.getPath();
				}
			} catch (IOException e) {
				MercurialEclipsePlugin.logError(e);
				return child.getPath();
			}
		}
		// +1 is to remove the file separator / at the start of the relative path
		return fullPath.substring(getPath().length() + 1);
	}

	/**
	 * @return the {@link IPath} object corresponding to this root, never null
	 */
	public IPath getIPath() {
		return path;
	}

	public IPath toAbsolute(IPath relative){
		return path.append(relative);
	}

	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}
}
