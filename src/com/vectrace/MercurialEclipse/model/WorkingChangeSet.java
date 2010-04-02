/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov (Intland) - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;

/**
 * A temporary changeset which holds not commited resources. This changeset cannot be used
 * as a usual changeset, as many of it's functionality is not supported or limited.
 *
 * @author Andrei
 */
public class WorkingChangeSet extends ChangeSet {


	public WorkingChangeSet(String name) {
		super(-1, name, null, null, "", null, "", null, null);
		Assert.isNotNull(name);
		setName(name);
		direction = Direction.OUTGOING;
		files = new HashSet<IFile>();

	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof WorkingChangeSet)){
			return false;
		}
		WorkingChangeSet set2 = (WorkingChangeSet) obj;
		return getName().equals(set2.getName());
	}

	@Override
	public int hashCode() {
		return System.identityHashCode(this);
	}

	@Override
	public String toString() {
		String changeset = getChangeset();
		int size = getFiles().size();
		if(size == 0){
			return changeset + " (empty)";
		}
		return changeset + " (" + size + ")";
	}

	public void clear(){
		synchronized (files){
			files.clear();
		}
	}

	public void dispose() {
		clear();
	}



	@Override
	public void setComment(String description) {
		super.setComment(description);
	}

	@SuppressWarnings("restriction")
	@Override
	public void setName(String name) {
		super.setName(name);
	}

	@Override
	public FileFromChangeSet[] getChangesetFiles() {
		Set<IFile> files2 = getFiles();
		int diffKind = Differencer.CHANGE | Differencer.RIGHT;

		List<FileFromChangeSet> fcs = new ArrayList<FileFromChangeSet>(files2.size());
		for (IFile file : files2) {
			fcs.add(new FileFromChangeSet(this, file, diffKind));
		}
		return fcs.toArray(new FileFromChangeSet[0]);
	}

	/**
	 * @param file non null
	 */
	public void addFile(IFile file) {
		files.add(file);
	}


}