/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov         - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.properties.DoNotDisplayMe;
import com.vectrace.MercurialEclipse.utils.ChangeSetUtils;

/**
 * A temporary changeset which holds not commited resources. This changeset cannot be used
 * as a usual changeset, as many of it's functionality is not supported or limited.
 * @author Andrei
 */
public abstract class WorkingChangeSet extends ChangeSet {

	private static final Tag[] EMPTY_TAGS = new Tag[0];

	private final String name;
	private String comment;
	Set<IFile> files;

	public WorkingChangeSet(String name) {

		this.name = name;

		setComment("");
		files = new LinkedHashSet<IFile>();
		setName(name);
	}

	@Override
	@DoNotDisplayMe
	public int getIndex() {
		return -1;
	}

	@Override
	public String getNode() {
		return name;
	}

	/**
	 * @return tags array (all tags associated with current changeset). May return empty array, but
	 *         never null
	 * @see ChangeSetUtils#getPrintableTagsString(ChangeSet)
	 */
	@Override
	@DoNotDisplayMe
	public Tag[] getTags() {
		return EMPTY_TAGS;
	}

	/**
	 * @return the tagsStr
	 */
	@Override
	@DoNotDisplayMe
	public String getTagsStr() {
		return null;
	}

	@Override
	public String getBranch() {
		return null;
	}

	@Override
	public String getComment() {
		return comment;
	}

	protected String getIndexAndName() {
		return -1 + ":" + name; //$NON-NLS-1$
	}

	/**
	 * @return the nodeShort
	 */
	@DoNotDisplayMe
	@Override
	public String getNodeShort() {
		throw new UnsupportedOperationException();
	}

	/**
	 * @see com.vectrace.MercurialEclipse.model.ChangeSet#getBundleFile()
	 */
	@Override
	@DoNotDisplayMe
	public File getBundleFile() {
		return null;
	}

	/**
	 * @see com.vectrace.MercurialEclipse.model.ChangeSet#getParents()
	 */
	@Override
	@DoNotDisplayMe
	public String[] getParents() {
		return null;
	}

	public void setComment(String comment) {
		if (comment != null) {
			this.comment = comment;
		} else {
			this.comment = "";
		}
	}

	/**
	 * @return the repository
	 */
	@Override
	@DoNotDisplayMe
	public IHgRepositoryLocation getRepository() {
		return null;
	}

	/**
	 * @return the direction
	 */
	@Override
	public Direction getDirection() {
		return Direction.OUTGOING;
	}

	/**
	 * @see com.vectrace.MercurialEclipse.model.ChangeSet#getHgRoot()
	 */
	@Override
	public HgRoot getHgRoot() {
		return null;
	}

	@Override
	@DoNotDisplayMe
	public String getAuthor() {
		return "";
	}

	@Override
	@DoNotDisplayMe
	public Date getDate() {
		return UNKNOWN_DATE;
	}

	@Override
	public Set<IFile> getFiles() {
		return Collections.unmodifiableSet(files);
	}

	/**
	 * @see com.vectrace.MercurialEclipse.model.ChangeSet#getChangedFiles()
	 */
	@Override
	@DoNotDisplayMe
	public List<FileStatus> getChangedFiles() {
		assert false;
		return Collections.EMPTY_LIST;
	}

	@Override
	public void remove(IResource file){
		// simply not supported, as it may be called not only from our code
	}

	/**
	 * @see com.vectrace.MercurialEclipse.model.ChangeSet#isCurrent()
	 */
	@Override
	@DoNotDisplayMe
	public boolean isCurrent() {
		return false;
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj;
	}

	@Override
	public int hashCode() {
		return System.identityHashCode(this);
	}

	@Override
	public String toString() {
		int size = getFiles().size();
		if(size == 0){
			return name + " (empty)";
		}
		return name + " (" + size + ")";
	}

	public void clear(){
		synchronized (files){
			files.clear();
		}
	}

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
			fcs.add(new FileFromChangeSet(this, file, null, diffKind));
		}
		return fcs.toArray(new FileFromChangeSet[0]);
	}
}