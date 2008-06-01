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

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.vectrace.MercurialEclipse.exception.HgException;

/**
 * Base client class
 * @author bastian
 * 
 */
public abstract class AbstractClient {
    /**
     * @param resource
     * @return
     */
    protected static File getWorkingDirectory(IResource resource) {
        Assert.isNotNull(resource);
        IResource myWorkDir = resource;
        if (resource.getType()==IResource.FILE) {
            myWorkDir = resource.getParent();
        }
        return myWorkDir.getLocation().toFile();
    }

    public AbstractClient() {
    }
    
    /**
     * @return
     * @throws HgException
     */
    public static IFolder getHgRoot(IResource res) throws HgException {
        Assert.isNotNull(res);
        IPath path = new Path(HgRootClient.getHgRoot(res));
        IFolder hgRoot = res.getProject().getFolder(path);
        return hgRoot;
    }
}
