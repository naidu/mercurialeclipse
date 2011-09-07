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
package com.vectrace.MercurialEclipse.storage;

import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

/**
 * @author Ge Zhong
 *
 */
public class ResourceDataLoader extends DataLoader {

	private final IResource resource;

	public ResourceDataLoader(IResource resource) {
		this.resource = resource;
	}
	/**
	 * @see com.vectrace.MercurialEclipse.storage.DataLoader#getHgRoot()
	 */
	@Override
	public HgRoot getHgRoot() {
		return MercurialTeamProvider.getHgRoot(resource);
	}

	/**
	 * @see com.vectrace.MercurialEclipse.storage.DataLoader#getResource()
	 */
	@Override
	public IResource getResource() {
		return resource;
	}

}
