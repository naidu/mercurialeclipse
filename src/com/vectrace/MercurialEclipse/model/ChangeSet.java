/*******************************************************************************
 * Copyright (c) 2007-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Charles O'Farrell         - HgRevision
 *     Bastian Doetsch			 - some more info fields
 *******************************************************************************/

package com.vectrace.MercurialEclipse.model;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.vectrace.MercurialEclipse.HgRevision;

public class ChangeSet implements Comparable<ChangeSet> {
	private int changesetIndex;
	private String changeset;
	private String tag;
	private String user;
	private String date;
	private String[] changedFiles;
	private String description;
	private String ageDate;
	private String nodeShort;
	private String[] parents;
	private Date realDate;
	private File bundleFile;

	public ChangeSet(int changesetIndex, String changeSet, String tag,
			String user, String date, String description,
			String[] parents) {
		this.changesetIndex = changesetIndex;
		this.changeset = changeSet;
		this.tag = tag;
		this.user = user;
		this.date = date;
		this.description = description;
		this.parents = parents;
		try {
			if (date != null) {
				this.realDate = new SimpleDateFormat("yyyy-MM-dd hh:mm Z")
						.parse(date);
			}
		} catch (Exception e) {
			this.realDate = null;
		}
	}

	public ChangeSet(int changesetIndex, String changeSet, String user,
			String date) {
		this(changesetIndex, changeSet, null, user, date, null, null);
	}

	public ChangeSet(int rev, String nodeShort, String node, String tag,
			String author, String date, String ageDate, String[] changedFiles,
			String description, File bundle, String[] parents) {
		this(rev, node, tag, author, date, description, parents);
		this.nodeShort = nodeShort;
		this.ageDate = ageDate;
		this.changedFiles = changedFiles;
		this.bundleFile = bundle;
	}

	public int getChangesetIndex() {
		return changesetIndex;
	}

	public String getChangeset() {
		return changeset;
	}

	public String getTag() {
		return tag;
	}

	public String getUser() {
		return user;
	}

	public String getDate() {
		return date;
	}

	public String getDescription() {
		return description;
	}

	public HgRevision getRevision() {
		return new HgRevision(changeset, changesetIndex);
	}

	@Override
	public String toString() {
		if (nodeShort != null) {
			return this.changesetIndex + ":" + this.nodeShort;
		}
		return this.changesetIndex + ":" + this.changeset;

	}

	/**
	 * @return the changedFiles
	 */
	public String[] getChangedFiles() {
		return changedFiles;
	}

	/**
	 * @param changedFiles
	 *            the changedFiles to set
	 */
	public void setChangedFiles(String[] changedFiles) {
		this.changedFiles = changedFiles;
	}

	/**
	 * @return the ageDate
	 */
	public String getAgeDate() {
		return ageDate;
	}

	/**
	 * @param ageDate
	 *            the ageDate to set
	 */
	public void setAgeDate(String ageDate) {
		this.ageDate = ageDate;
	}

	/**
	 * @return the nodeShort
	 */
	public String getNodeShort() {
		return nodeShort;
	}

	/**
	 * @param nodeShort
	 *            the nodeShort to set
	 */
	public void setNodeShort(String nodeShort) {
		this.nodeShort = nodeShort;
	}

	public int compareTo(ChangeSet o) {
		if (o.getChangeset().equals(this.getChangeset())) {
			return 0;
		}

		if (this.getChangesetIndex() == o.getChangesetIndex()) {
			return this.date.compareTo(o.date);
		}
		return this.getChangesetIndex() - o.getChangesetIndex();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj != null && obj instanceof ChangeSet) {
			return this.compareTo((ChangeSet) obj) == 0;
		}
		return false;
	}

	public Date getRealDate() {
		return this.realDate;
	}

	/**
	 * @return the bundleFile
	 */
	public File getBundleFile() {
		return bundleFile;
	}

	/**
	 * @param bundleFile
	 *            the bundleFile to set
	 */
	public void setBundleFile(File bundleFile) {
		this.bundleFile = bundleFile;
	}

	public String[] getParents() {
		return parents;
	}

	public void setParents(String[] parents) {
		this.parents = parents;
	}
}
