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
package com.vectrace.MercurialEclipse.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;

import com.vectrace.MercurialEclipse.utils.IniFile;

/**
 * Hg root represents the root of hg repository as <b>canonical path</b>
 * (see {@link File#getCanonicalPath()})
 *
 * @author bastian
 */
public class HgRoot extends File {

    private static final long serialVersionUID = 2L;
    private Charset encoding;
    private Charset fallbackencoding;
    private File config;

    public HgRoot(String pathname) throws IOException {
        this(new File(pathname));
    }

    public HgRoot(File file) throws IOException {
        super(file.getCanonicalPath());
        init();
    }

    private void init() {
        // next in line is HGENCODING in environment
        String enc = System.getProperty("HGENCODING");

        // next is platform encoding as available in JDK
        if (enc == null || enc.length() == 0) {
            enc = Charset.defaultCharset().name();
        }

        setEncoding(Charset.forName(enc));

        // set fallbackencoding to windows standard codepage
        String fallback = getConfigItem("ui", "fallbackencoding");

        if (fallbackencoding == null) {
            if (fallback == null || fallback.length() == 0) {
                fallback = "windows-1251";
            }
            fallbackencoding = Charset.forName(fallback);
        }
    }

    public void setEncoding(Charset charset) {
        this.encoding = charset;
    }

    /**
     * @return the encoding
     */
    public Charset getEncoding() {
        return encoding;
    }

    /**
     * @return
     */
    public File getConfig() {
        if (config == null) {
            File hgrc = new File(this, ".hg/hgrc");
            if (hgrc.exists()) {
                config = hgrc;
                return hgrc;
            }
        }
        return null;
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

    /**
     * @return the fallbackencoding
     */
    public Charset getFallbackencoding() {
        return fallbackencoding;
    }
}
