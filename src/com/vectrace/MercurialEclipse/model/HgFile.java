/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Charles O'Farrell - implementation
 *******************************************************************************/

package com.vectrace.MercurialEclipse.model;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;

public class HgFile extends HgFilesystemObject {
    private static final long serialVersionUID = 1L;

    public HgFile(File file) throws IOException, CoreException {
        super(file);
    }

    public File getFile() {
        return this;
    }    
}
