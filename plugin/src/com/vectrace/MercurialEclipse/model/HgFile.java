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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import com.vectrace.MercurialEclipse.commands.HgCatClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.MercurialRootCache;

/**
 * @author Ge Zhong
 *
 */
public class HgFile extends HgRevisionResource implements IHgFile {

	protected static final ByteArrayInputStream EMPTY_STREAM = new ByteArrayInputStream(new byte[0]);

	// constructors

	/**
	 * @param hgRoot
	 * @param changeset global changeset id
	 * @param path relative path to HgRoot
	 * @throws HgException
	 */
	public HgFile(HgRoot hgRoot, String changeset, IPath path) throws HgException {
		super(hgRoot, changeset, path);
	}

	public HgFile(HgRoot hgRoot, ChangeSet changeset, IPath path) {
		super(hgRoot, changeset, path);
	}

	// operations

	/**
	 * @see com.vectrace.MercurialEclipse.model.IHgFile#getFileExtension()
	 */
	public String getFileExtension() {
		return path.getFileExtension();
	}

	/**
	 * @see org.eclipse.core.resources.IStorage#getContents()
	 */
	@Override
	public InputStream getContents() throws CoreException {
		return new ByteArrayInputStream(super.getContent());
	}

	/**
	 * @see org.eclipse.core.resources.IStorage#getFullPath()
	 */
	public IPath getFullPath() {
		IPath p = this.getHgRoot().getIPath().append(path);

		String extension = p.getFileExtension();
		String version = " [" + changeset.getIndex() + "]";
		if(extension != null) {
			version += "." + extension;
		}
		p = p.append(version);

		return p;
	}

	/**
	 * @see org.eclipse.compare.BufferedContent#createStream()
	 */
	@Override
	protected InputStream createStream() throws CoreException {
		try {
			return HgCatClient.getContent(this);
		} catch (IOException e) {
			throw new HgException("Unable to get contents", e);
		}
	}

	public static HgFile make(ChangeSet cs, IFile file) {
		return new HgFile(cs.getHgRoot(), cs, cs.getHgRoot().getRelativePath(file));
	}

	/**
	 * Make an instance that is the clean version of the given file
	 */
	public static HgFile makeAtCurrentRev(IFile remoteFile) throws HgException {
		HgRoot root = MercurialRootCache.getInstance().getHgRoot(remoteFile);
		ChangeSet cs = LocalChangesetCache.getInstance().getChangesetForRoot(root);

		return new HgFile(cs.getHgRoot(), cs, root.getRelativePath(remoteFile));
	}
}
