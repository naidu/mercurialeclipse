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

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;

/**
 *
 */
public abstract class HgRevisionResource extends HgResource implements IChangeSetHolder {

	/**
	 * If a local resource then then null
	 */
	protected final ChangeSet changeset;

	// constructors

	/**
	 * @param root the HgRoot, not null
	 * @param changeset is null if and only if resource != null, or this is a NullHgFile
	 * @param path relative path to HgRoot, not null
	 */
	public HgRevisionResource(HgRoot root, String changeset, IPath path) throws HgException {
		super(root, path.removeTrailingSeparator());

		LocalChangesetCache cache = LocalChangesetCache.getInstance();

		this.changeset = cache.get(root, changeset);

		Assert.isNotNull(this.changeset);
	}

	/**
	 * @param root the HgRoot, not null
	 * @param changeset =null if and only if wResource != null or this is a NullHgFile
	 * @param path relative path to HgRoot, not null
	 */
	public HgRevisionResource(HgRoot root, ChangeSet changeset, IPath path) {
		super(root, path.removeTrailingSeparator());

		this.changeset = changeset;
	}

	// operations

	public final ChangeSet getChangeSet() {
		return changeset;
	}

	/**
	 * @see com.vectrace.MercurialEclipse.model.IHgResource#getName()
	 */
	public String getName() {
		String name = path.lastSegment();
		if(name == null) {
			return root.getIPath().lastSegment();
		}
		return name;
	}

	/**
	 * @see com.vectrace.MercurialEclipse.model.IHgResource#isReadOnly()
	 */
	public final boolean isReadOnly() {
		return true;
	}
}
