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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourceAttributes;

import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 *
 */
public abstract class HgWorkspaceResource extends HgResource implements IResourceHolder {

	/**
	 * If not null representing a local file in Hg working copy
	 */
	protected final IResource resource;

	/**
	 * Wraps a local resource as HgResource
	 * @param root the HgRoot, not null
	 * @param resource a local resource
	 */
	public HgWorkspaceResource(HgRoot root, IResource resource) {
		super(root, root.toRelative(ResourceUtils.getPath(resource)));

		this.resource = resource;
	}

	// operations

	/**
	 * @see com.vectrace.MercurialEclipse.model.IHgResource#isReadOnly()
	 */
	public final boolean isReadOnly() {
		if (resource != null) {
			ResourceAttributes attributes = resource.getResourceAttributes();
			if (attributes != null) {
				return attributes.isReadOnly();
			}
		}
		return true;
	}

	public IResource getResource() {
		return resource;
	}

	/**
	 * @see com.vectrace.MercurialEclipse.model.IHgResource#getName()
	 */
	public final String getName() {
		return resource.getName();
	}
}
