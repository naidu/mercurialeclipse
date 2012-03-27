/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * john	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.model;

import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.team.cache.MercurialRootCache;

/**
 *
 */
public class HgWorkspaceFile extends HgWorkspaceResource implements IHgFile  {

	public HgWorkspaceFile(HgRoot root, IFile res) {
		super(root, res);
	}

	// operations

	/**
	 * @see org.eclipse.core.resources.IStorage#getFullPath()
	 */
	public IPath getFullPath() {
		return getHgRoot().getIPath().append(path);
	}

	/**
	 * @see com.vectrace.MercurialEclipse.model.IHgFile#getFileExtension()
	 */
	public String getFileExtension() {
		return resource.getFileExtension();
	}

	/**
	 * @see com.vectrace.MercurialEclipse.model.HgResource#createStream()
	 */
	@Override
	protected InputStream createStream() throws CoreException {
		if (resource.exists()) {
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
						// if the file is deleted or inaccessible
						// log the error and return empty stream
						MercurialEclipsePlugin.logError(e);
					}
				}
				if (is != null) {
					return is;
				}
			}
		}
		return HgFile.EMPTY_STREAM;
	}

	/**
	 * @see com.vectrace.MercurialEclipse.model.HgResource#getAdapter(java.lang.Class)
	 */
	@Override
	public Object getAdapter(Class adapter) {
		if(adapter == IResource.class) {
			return getResource();
		}
		return null;
	}

	public static HgWorkspaceFile make(IFile file) {
		return new HgWorkspaceFile(MercurialRootCache.getInstance().getHgRoot(file), file);
	}
}
