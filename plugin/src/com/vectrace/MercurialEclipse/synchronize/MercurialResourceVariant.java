/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian Doetsch	implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.IResourceVariant;

import com.vectrace.MercurialEclipse.compare.RevisionNode;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.IChangeSetHolder;
import com.vectrace.MercurialEclipse.model.IHgFolder;
import com.vectrace.MercurialEclipse.model.IHgResource;

public class MercurialResourceVariant implements IResourceVariant {
	private final RevisionNode rev;

	public MercurialResourceVariant(RevisionNode rev) {
		this.rev = rev;
	}

	public byte[] asBytes() {
		return getContentIdentifier().getBytes();
	}

	public String getContentIdentifier() {
		ChangeSet cs = ((IChangeSetHolder) rev.getHgResource()).getChangeSet();
		return cs.getChangesetIndex() + ":" + cs.getChangeset();
	}

	public String getName() {
		return rev.getHgResource().getName();
	}

	public IStorage getStorage(IProgressMonitor monitor) throws TeamException {
		IHgResource hgResource = rev.getHgResource();
		if (hgResource instanceof IStorage) {
			return (IStorage) hgResource;
		}
		return null;
	}

	public boolean isContainer() {
		return rev.getHgResource() instanceof IHgFolder;
	}

	public RevisionNode getRev() {
		return rev;
	}

}
