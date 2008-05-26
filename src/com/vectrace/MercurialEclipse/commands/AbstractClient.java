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
package com.vectrace.MercurialEclipse.commands;

import java.io.File;

import org.eclipse.core.resources.IResource;

/**
 * Base client class
 * @author bastian
 * 
 */
public abstract class AbstractClient {
    /**
     * @param workDir
     * @return
     */
    protected static File getWorkingDirectory(IResource workDir) {
        IResource myWorkDir = workDir;
        if (workDir.getType()==IResource.FILE) {
            myWorkDir = workDir.getParent();
        }
        return myWorkDir.getLocation().toFile();
    }

    public AbstractClient() {
    }
}
