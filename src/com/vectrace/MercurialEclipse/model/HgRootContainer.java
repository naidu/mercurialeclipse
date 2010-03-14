/*******************************************************************************
 * Copyright (c) 2010 Andrei Loskutov (Intland)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.model;

import java.net.URI;

import org.eclipse.core.internal.resources.Container;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;


/**
 * @author Andrei
 */
@SuppressWarnings("restriction")
public final class HgRootContainer extends Container {
	private final HgRoot hgRoot;

	public HgRootContainer(HgRoot hgRoot) {
		super(hgRoot.getIPath(), (Workspace) ResourcesPlugin.getWorkspace());
		this.hgRoot = hgRoot;
	}

	public String getDefaultCharset(boolean checkImplicit) throws CoreException {
		return hgRoot.getEncoding().toString();
	}

	@Override
	public int getType() {
		return IResource.FOLDER;
	}

	/**
	 * @return the hgRoot, never null
	 */
	public HgRoot getHgRoot() {
		return hgRoot;
	}

	@Override
	public String getName() {
		return hgRoot.getName();
	}

	@Override
	public IPath getLocation() {
		return hgRoot.getIPath();
	}

	@Override
	public URI getLocationURI() {
		return hgRoot.toURI();
	}

	@Override
	public long getLocalTimeStamp() {
		return hgRoot.lastModified();
	}

	@Override
	public long getModificationStamp() {
		return getLocalTimeStamp();
	}

	@Override
	public IContainer getParent() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}

	@Override
	public IProject getProject() {
		return null;
	}

	@Override
	public IPath getFullPath() {
		return new Path("");
	}

}