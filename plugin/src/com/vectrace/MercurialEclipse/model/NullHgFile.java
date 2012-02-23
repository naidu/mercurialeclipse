/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * ge.zhong	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.model;

import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import com.vectrace.MercurialEclipse.commands.HgIdentClient;

/**
 * @author Ge Zhong
 *
 */
public class NullHgFile extends HgFile {

	public NullHgFile(HgRoot hgRoot, ChangeSet changeset, IPath path) {
		super(hgRoot, changeset, path);
	}

	public NullHgFile(HgRoot hgRoot, String revision, IPath path) {
 		super(hgRoot, revision, path);
 	}

	public NullHgFile(HgRoot hgRoot, IFile file) {
 		super(hgRoot, HgIdentClient.VERSION_ZERO, file);
 	}

	@Override
	public InputStream getContents() throws CoreException {
		return EMPTY_STREAM;
	}

	@Override
	public String getName() {
		return super.getName() + ": DOES NOT EXIST";
	}

}
