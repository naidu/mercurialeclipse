package com.vectrace.MercurialEclipse.model;

import java.io.InputStream;

import org.eclipse.compare.BufferedContent;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

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

/**
 * @author Ge Zhong
 *
 */
public abstract class HgResource extends BufferedContent implements IHgResource {

	/**
	 * Not null
	 */
	protected final HgRoot root;

	/**
	 * Not null
	 */
	protected final IPath path;

	// constructors

	protected HgResource(HgRoot root, IPath path) {
		this.root = root;
		this.path = path;
	}

	// operations

	/**
	 * @see com.vectrace.MercurialEclipse.model.IHgResource#getHgRoot()
	 */
	public HgRoot getHgRoot() {
		return root;
	}

	public IPath getIPath() {
		return path;
	}

	/**
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		return null;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof IHgResource) {
			IHgResource res = (IHgResource)other;
			return root.equals(res.getHgRoot()) && path.equals(res.getIPath());
		}
		return false;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return (root.hashCode() << 16) & path.hashCode();
	}

	/**
	 * @see org.eclipse.compare.BufferedContent#createStream()
	 */
	@Override
	protected InputStream createStream() throws CoreException {
		return null;
	}

	/**
	 * @see org.eclipse.core.resources.IEncodedStorage#getCharset()
	 */
	public String getCharset() {
		return this.getHgRoot().getEncoding();
	}
}
