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

/**
 * @author bastian
 *
 */
public class HgRoot extends File {

    private static final long serialVersionUID = 1L;

    /**
     * @param pathname
     */
    public HgRoot(String pathname) {
        super(pathname);        
    }
    
    public HgRoot(File file) throws IOException {
        this(file.getCanonicalPath());
    }

    /**
     * @param uri
     */
    public HgRoot(URI uri) {
        super(uri);
    }

    /**
     * @param parent
     * @param child
     */
    public HgRoot(String parent, String child) {
        super(parent, child);
    }

    /**
     * @param parent
     * @param child
     */
    public HgRoot(File parent, String child) {
        super(parent, child);
    }

}
