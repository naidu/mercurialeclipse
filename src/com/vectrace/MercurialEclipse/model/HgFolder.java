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
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * @author bastian
 *
 */
public class HgFolder extends File {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private List<File> projectFiles;
    
    /**
     * @param pathname
     */
    public HgFolder(String pathname) {
        super(pathname);
    }

    /**
     * @param uri
     */
    public HgFolder(URI uri) {
        super(uri);
    }

    /**
     * @param parent
     * @param child
     */
    public HgFolder(String parent, String child) {
        super(parent, child);
    }

    /**
     * @param parent
     * @param child
     */
    public HgFolder(File parent, String child) {
        super(parent, child);
    }
    

    /**
     * @return the projectFile
     */
    public List<File> getProjectFiles() {
    	List<File> pFiles = new ArrayList<File>();
    	File[] subFiles = this.listFiles();
    	for (File fileString : subFiles) {
    		HgFolder file = new HgFolder(fileString.toURI());
            if (file.isDirectory()) {
            	pFiles.addAll(file.getProjectFiles());
            } else {
            	if (file.getName().equals(".project")) {
            		pFiles.add(file);
            	}
            }
        }
    	projectFiles = pFiles;
        return this.projectFiles;
    }   

}
