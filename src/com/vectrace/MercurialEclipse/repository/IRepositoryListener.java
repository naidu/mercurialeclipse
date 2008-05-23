/*******************************************************************************
 * Copyright (c) 2003, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 *     Bastian Doetsch              - adaptation
 ******************************************************************************/
package com.vectrace.MercurialEclipse.repository;

import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;


/**
 * Listener for repositories. events fired when repository added, removed or changed 
 */
public interface IRepositoryListener {
	public void repositoryAdded(HgRepositoryLocation root);
    public void repositoryModified(HgRepositoryLocation root);
	public void repositoryRemoved(HgRepositoryLocation root);
	public void repositoriesChanged(HgRepositoryLocation[] roots);
}

