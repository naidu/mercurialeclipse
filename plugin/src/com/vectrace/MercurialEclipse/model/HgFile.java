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

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgCatClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;

/**
 * @author Ge Zhong
 *
 */
public class HgFile extends HgRevisionResource implements IHgFile {

	protected static final ByteArrayInputStream EMPTY_STREAM = new ByteArrayInputStream(new byte[0]);

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
		byte[] result = null;
		// Setup and run command
		if (changeset.getDirection() == Direction.INCOMING && changeset.getBundleFile() != null) {
			// incoming: overlay repository with bundle and extract then via cat
			try {
				result = HgCatClient.getContentFromBundle(this,
						changeset.getRevision().getNode(),
						changeset.getBundleFile());
			} catch (IOException e) {
				throw new HgException("Unable to determine canonical path for " + changeset.getBundleFile(), e);
			}
		} else {
			try {
				result = HgCatClient.getContent(this);
			} catch (HgException e) {
				MercurialEclipsePlugin.logError(e);
			}
		}

		if(result != null){
			return new ByteArrayInputStream(result);
		}
		return EMPTY_STREAM;
	}

	public static HgFile make(HgRoot root, IFile file, String cs) throws HgException {
		return new HgFile(root, cs, root.getRelativePath(file));
	}

	public static HgFile make(ChangeSet cs, IFile file) {
		return new HgFile(cs.getHgRoot(), cs, cs.getHgRoot().getRelativePath(file));
	}

}
