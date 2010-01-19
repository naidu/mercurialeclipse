/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;

import org.eclipse.compare.ResourceNode;
import org.eclipse.core.resources.IFile;
import org.eclipse.team.core.TeamException;

import com.vectrace.MercurialEclipse.commands.HgParentClient;
import com.vectrace.MercurialEclipse.compare.RevisionNode;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.utils.CompareUtils;

/**
 * @author zingo, Jerome Negre <jerome+hg@jnegre.org>
 */
public class CompareAction extends SingleFileAction {

	/**
	 * Empty constructor must be here, otherwise Eclipse wouldn't be able to create the object via reflection
	 */
	public CompareAction() {
		super();
	}

	public CompareAction(IFile file) {
		this();
		this.selection = file;
	}

	@Override
	protected void run(IFile file) throws TeamException {

		boolean clean = MercurialStatusCache.getInstance().isClean(file);
		if(!clean) {
			compareToLocal(file);
			return;
		}
		// get the predecessor version and compare current version with it
		String[] parents = HgParentClient.getParentNodeIds(file);
		ChangeSet cs = LocalChangesetCache.getInstance().getOrFetchChangeSetById(file, parents[0]);
		if(cs != null && cs.getChangesetIndex() != 0) {
			parents = cs.getParents();
			if (parents == null || parents.length == 0) {
				parents = HgParentClient.getParentNodeIds(file, cs);
			}
			if (parents != null && parents.length > 0) {
				ChangeSet cs2 = LocalChangesetCache.getInstance().getOrFetchChangeSetById(file, parents[0]);
				if(cs2 != null) {
					CompareUtils.openEditor(file, cs2, true);
					return;
				}
			}
		}
		// something went wrong. So compare to local
		compareToLocal(file);
	}

	private void compareToLocal(IFile file) {
		// local workspace version
		ResourceNode leftNode = new ResourceNode(file);
		// mercurial version
		RevisionNode rightNode = new RevisionNode(new MercurialRevisionStorage(file));
		CompareUtils.openEditor(leftNode, rightNode, false, true);
	}

}
