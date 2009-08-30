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
import java.io.IOException;
import java.net.URI;

import com.vectrace.MercurialEclipse.commands.HgClients;

/**
 * @author bastian
 *
 */
public class HgFilesystemObject extends File {
    private final HgRoot hgRoot;

    private static final long serialVersionUID = 1L;

    public HgFilesystemObject(String pathname) {
        super(pathname);
        this.hgRoot = HgClients.getHgRoot(this);
    }

    public HgFilesystemObject(File file) throws IOException {
        super(file.getCanonicalPath());
        this.hgRoot = HgClients.getHgRoot(this);
    }

    public HgFilesystemObject(URI uri) {
        super(uri);
        this.hgRoot = HgClients.getHgRoot(this);
    }

    public HgFilesystemObject(String parent, String child) {
        super(parent, child);
        this.hgRoot = HgClients.getHgRoot(this);
    }

    public HgFilesystemObject(File parent, String child) {
        super(parent, child);
        this.hgRoot = HgClients.getHgRoot(this);
    }

    public HgRoot getHgRoot() {
        return hgRoot;
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
