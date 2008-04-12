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

import java.text.SimpleDateFormat;
import java.util.Date;

import com.vectrace.MercurialEclipse.HgRevision;

public class ChangeSet implements Comparable<ChangeSet> {
	private int changesetIndex;
	private String changeset;
	private String tag;
	private String user;
	private String date;
	private String files;
	private String[] changedFiles;
	private String description;
	private String ageDate;
	private String nodeShort;
	private Date realDate;

	public ChangeSet(int changesetIndex, String changeSet, String tag,
			String user, String date, String files, String description) {
		this.changesetIndex = changesetIndex;
		this.changeset = changeSet;
		this.tag = tag;
		this.user = user;
		this.date = date;
		this.files = files;
		this.description = description;
		try {
			if (date != null) {
				this.realDate = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss Z")
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
			String description) {
		this(rev, node, tag, author, date, null, description);
		this.nodeShort = nodeShort;
		this.ageDate = ageDate;
		this.changedFiles = changedFiles;
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

	public String getFiles() {
		return files;
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
		int dateCompare = this.getRealDate().compareTo(o.getRealDate());
		if (dateCompare == 0) {
			return this.getChangesetIndex() - o.getChangesetIndex();
		}
		return dateCompare;
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
}
