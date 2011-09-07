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
import java.io.UnsupportedEncodingException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IStorage;
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
public class HgFile extends HgResource implements IHgFile {

	protected static final ByteArrayInputStream EMPTY_STREAM = new ByteArrayInputStream(new byte[0]);

	/**
	 * @param hgRoot
	 * @param changeset global changeset id
	 * @param path relative path to HgRoot
	 */
	public HgFile(HgRoot hgRoot, String changeset, IPath path) {
		super(hgRoot, changeset, path);
	}

	public HgFile(HgRoot hgRoot, ChangeSet changeset, IPath path) {
		super(hgRoot, changeset, path);
	}

	public HgFile(HgRoot hgRoot, IFile file) {
		super(hgRoot, file);
	}

	/**
	 * @see org.eclipse.core.resources.IEncodedStorage#getCharset()
	 */
	public String getCharset() throws CoreException {
		return this.getHgRoot().getEncoding();
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
		return this.getHgRoot().getIPath().append(path);
	}

	/**
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	@Override
	public Object getAdapter(Class adapter) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see org.eclipse.compare.BufferedContent#createStream()
	 */
	@Override
	protected InputStream createStream() throws CoreException {
		if (resource != null) {
			if (resource instanceof IStorage) {
				InputStream is= null;
				IStorage storage= (IStorage) resource;
				try {
					is= storage.getContents();
				} catch (CoreException e) {
					if (e.getStatus().getCode() == IResourceStatus.OUT_OF_SYNC_LOCAL) {
						resource.refreshLocal(IResource.DEPTH_INFINITE, null);
						is= storage.getContents();
					} else {
						throw e;
					}
				}
				if (is != null) {
					return is;
				}
			}
			return EMPTY_STREAM;
		}

		String result = "";
		// Setup and run command
		if (changeset.getDirection() == Direction.INCOMING && changeset.getBundleFile() != null) {
			// incoming: overlay repository with bundle and extract then via cat
			try {
				result = HgCatClient.getContentFromBundle(this,
						changeset.getRevision().getChangeset(),
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

		try {
			return new ByteArrayInputStream(result.getBytes(getCharset()));
		} catch (UnsupportedEncodingException e) {
			MercurialEclipsePlugin.logError(e);
		}
		return EMPTY_STREAM;
	}

}
