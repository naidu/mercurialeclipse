/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov         - implementation
 *     John Peberdy            - refactoring
 *******************************************************************************/
package com.vectrace.MercurialEclipse.model;

import org.eclipse.core.resources.IFile;

import com.vectrace.MercurialEclipse.synchronize.cs.UncommittedChangesetGroup;

/**
 * @author Andrei
 */
public class GroupedUncommittedChangeSet extends WorkingChangeSet {

	private boolean isDefault;

	private final UncommittedChangesetGroup group;

	public GroupedUncommittedChangeSet(String name, UncommittedChangesetGroup group) {
		super(name);

		this.group = group;
		group.add(this);
	}

	public void removeFile(IFile file) {
		// TODO check group files
//		boolean contains = group.contains(file);
//		boolean added = contains;
		synchronized (files){
			files.remove(file);
		}
	}

	public void setDefault(boolean isDefault) {
		this.isDefault = isDefault;
	}

	/**
	 * @return true if all changes should go to this changeset first (if there are more then one
	 *         uncommitted changeset available)
	 */
	public boolean isDefault() {
		return isDefault;
	}

	public boolean add(IFile file){
		boolean contains = group.contains(file);
		boolean added = contains;
		if(!contains) {
			added = group.add(file, this);
		}
		if(added) {
			synchronized (files){
				added = files.add(file);
			}
		}
		return added;
	}

	/**
	 * @return the group, never null
	 */
	public UncommittedChangesetGroup getGroup() {
		return group;
	}
}
