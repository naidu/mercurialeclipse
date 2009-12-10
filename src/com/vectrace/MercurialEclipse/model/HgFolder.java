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
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

/**
 * @author bastian
 *
 */
public class HgFolder extends HgFilesystemObject {

	private static final long serialVersionUID = 1L;

	public HgFolder(String pathname) throws IOException, CoreException {
		super(pathname);
	}

	public HgFolder(URI uri) throws IOException, CoreException {
		super(uri);
	}

	public HgFolder(String parent, String child) throws IOException,
			CoreException {
		super(parent, child);
	}

	public HgFolder(File parent, String child) throws IOException,
			CoreException {
		super(parent, child);
	}

	public HgFolder(File file) throws IOException, CoreException {
		super(file);
	}

	public List<File> getProjectFiles() throws IOException, CoreException {
		List<File> pFiles = new ArrayList<File>();
		File[] subFiles = this.listFiles();
		if (subFiles != null) {
			for (File fileString : subFiles) {
				HgFolder file = new HgFolder(fileString.toURI());
				if (file.isDirectory()) {
					pFiles.addAll(file.getProjectFiles());
				} else {
					if (file.getName().equals(".project")) { //$NON-NLS-1$
						pFiles.add(file);
					}
				}
			}
		}
		return pFiles;
	}

	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}
}
