/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jérôme Nègre              - implementation
 *     Stefan C                  - Code cleanup
 *     Bastian Doetsch			 - extracted from RevisionChooserDialog for Sync.
 *******************************************************************************/
package com.vectrace.MercurialEclipse.storage;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.commands.HgLogClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;

public class FileDataLoader extends DataLoader {

	private IFile file;

	public FileDataLoader(IFile file) {
		this.file = file;
	}

	@Override
	public IProject getProject() {
		return file.getProject();
	}

	@Override
	public ChangeSet[] getRevisions() throws HgException {
		if (file.getType() == IResource.FILE) {
			super.changeSets = HgLogClient.getRevisions(file);
			return changeSets;
		}
		return null;
	}
}