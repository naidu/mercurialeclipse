package com.vectrace.MercurialEclipse.model;

import java.io.InputStream;

import org.eclipse.compare.BufferedContent;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;

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
	protected HgRoot root;

	/**
	 * Not null
	 */
	protected IPath path;

	/**
	 * If not null representing a local file in Hg working copy
	 */
	protected IResource resource;

	/**
	 * If a local resource then then null
	 */
	protected ChangeSet changeset;

	/**
	 * @param root the HgRoot, not null
	 * @param changeset is null if and only if resource != null, or this is a NullHgFile
	 * @param path relative path to HgRoot, not null
	 */
	public HgResource(HgRoot root, String changeset, IPath path) {
		try {
			if(changeset != null && changeset.length() != 0) {
				LocalChangesetCache cache = LocalChangesetCache.getInstance();
				this.changeset = cache.getOrFetchChangeSetById(root, changeset);
				if (this.changeset == null) {
					// refetch cache and try again
					cache.fetchRevisions(root, false, 0, 0, false);
					this.changeset = cache.getOrFetchChangeSetById(root, changeset);
				}
			}
			this.root = root;
			this.path = path.removeTrailingSeparator();
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
		}
	}

	/**
	 * @param root the HgRoot, not null
	 * @param changeset =null if and only if wResource != null or this is a NullHgFile
	 * @param path relative path to HgRoot, not null
	 */
	public HgResource(HgRoot root, ChangeSet changeset, IPath path) {
		if(changeset != null){
			this.changeset = changeset;
			this.root = root;
			this.path = path.removeTrailingSeparator();
		}
	}

	/**
	 * Wraps a local resource as HgResource
	 * @param root the HgRoot, not null
	 * @param resource a local resource
	 */
	public HgResource(HgRoot root, IResource resource) {
		this.root = root;
		this.resource = resource;
		path = root.toRelative(resource.getLocation());
	}

	/**
	 * @see com.vectrace.MercurialEclipse.model.IHgResource#getHgRoot()
	 */
	public HgRoot getHgRoot() {
		return root;
	}

	public String getHgRootRelativePath() {
		return path.toOSString();
	}

	public String getName() {
		String name = path.lastSegment();
		if(name == null) {
			return root.getIPath().lastSegment();
		}
		return name;
	}

	public String getFileExtension() {
		return path.getFileExtension();
	}

	public ChangeSet getChangeSet() {
		return changeset;
	}

	public IPath getIPath() {
		return path;
	}

	public Object getAdapter(Class adapter) {
		return null;
	}

	public boolean isReadOnly() {
		if (resource != null) {
			ResourceAttributes attributes = resource.getResourceAttributes();
			if (attributes != null) {
				return attributes.isReadOnly();
			}
		}
		return true;
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

	public IResource getResource() {
		return resource;
	}

	/**
	 * @see org.eclipse.compare.BufferedContent#createStream()
	 */
	@Override
	protected InputStream createStream() throws CoreException {
		return null;
	}

}
