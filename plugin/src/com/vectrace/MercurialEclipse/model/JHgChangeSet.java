/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * john	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.model;

import java.io.File;
import java.util.Date;

import com.aragost.javahg.Changeset;
import com.vectrace.MercurialEclipse.HgRevision;
import com.vectrace.MercurialEclipse.commands.HgTagClient;

/**
 * Changeset backed by a JavaHg changeset
 */
public class JHgChangeSet extends ChangeSet {

	private final Changeset changeset;
	private final HgRoot hgRoot;
	private final IHgRepositoryLocation remote;
	private final Direction direction;
	private final File bundle;

	private Tag[] tags;

	// constructors

	public JHgChangeSet(HgRoot hgRoot, Changeset changeset, IHgRepositoryLocation remote,
			Direction direction, File bundle) {
		this.changeset = changeset;
		this.hgRoot = hgRoot;
		this.remote = remote;
		this.direction = direction;
		this.bundle = bundle;

		setName(getChangesetIndex() + ":" + getNodeShort());
	}

	public JHgChangeSet(HgRoot hgRoot, Changeset changeset) {
		this(hgRoot, changeset, null, Direction.LOCAL, null);
	}

	// operations

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getName();
	}

	/**
	 * @see com.vectrace.MercurialEclipse.model.ChangeSet#getRealDate()
	 */
	@Override
	public Date getRealDate() {
		return changeset.getTimestamp().getDate();
	}

	/**
	 * @see com.vectrace.MercurialEclipse.model.ChangeSet#getChangesetIndex()
	 */
	@Override
	public int getChangesetIndex() {
		return changeset.getRevision();
	}

	/**
	 * @see com.vectrace.MercurialEclipse.model.ChangeSet#getChangeset()
	 */
	@Override
	public String getChangeset() {
		return changeset.getNode();
	}

	/**
	 * @see com.vectrace.MercurialEclipse.model.ChangeSet#getTags()
	 */
	@Override
	public Tag[] getTags() {
		if (tags == null) {
			tags = HgTagClient.getTags(hgRoot, changeset.tags());
		}
		return tags;
	}

	/**
	 * @see com.vectrace.MercurialEclipse.model.ChangeSet#getBranch()
	 */
	@Override
	public String getBranch() {
		return changeset.getBranch();
	}

	/**
	 * @see com.vectrace.MercurialEclipse.model.ChangeSet#getHgRoot()
	 */
	@Override
	public HgRoot getHgRoot() {
		return hgRoot;
	}

	/**
	 * @see com.vectrace.MercurialEclipse.model.ChangeSet#getNodeShort()
	 */
	@Override
	public String getNodeShort() {
		return changeset.getNode().substring(0, 12);
	}

	/**
	 * @see com.vectrace.MercurialEclipse.model.ChangeSet#getRevision()
	 */
	@Override
	public HgRevision getRevision() {
		return new HgRevision(changeset);
	}

	/**
	 * @see com.vectrace.MercurialEclipse.model.ChangeSet#getDirection()
	 */
	@Override
	public Direction getDirection() {
		return direction;
	}

	/**
	 * @see com.vectrace.MercurialEclipse.model.ChangeSet#getBundleFile()
	 */
	@Override
	public File getBundleFile() {
		return bundle;
	}

	/**
	 * @see com.vectrace.MercurialEclipse.model.ChangeSet#getParents()
	 */
	@Override
	public String[] getParents() {
		if (changeset.getParent2() == null) {
			return new String[] { changeset.getParent1().getNode() };
		}

		return new String[] { changeset.getParent1().getNode(), changeset.getParent2().getNode() };
	}

	/**
	 * @see com.vectrace.MercurialEclipse.model.ChangeSet#getRepository()
	 */
	@Override
	public IHgRepositoryLocation getRepository() {
		return remote;
	}

	/**
	 * @see com.vectrace.MercurialEclipse.model.ChangeSet#getParentRevision(int)
	 */
	@Override
	public HgRevision getParentRevision(int i) {
		switch (i) {
		case 0:
			return new HgRevision(changeset.getParent1());
		case 1:
			return new HgRevision(changeset.getParent2());
		}
		return null;
	}

	/**
	 * @see com.vectrace.MercurialEclipse.model.ChangeSet#getParentRevision(int, boolean)
	 */
	@Override
	public HgRevision getParentRevision(int i, boolean b) {
		return getParentRevision(i);
	}

	/**
	 * @see com.vectrace.MercurialEclipse.model.ChangeSet#getTagsStr()
	 */
	@Override
	public String getTagsStr() {
		StringBuilder b = new StringBuilder();
		for (String s : changeset.tags()) {
			b.append(s);
			b.append(',');
		}
		return b.length() == 0 ? "" : b.substring(0, b.length() - 1);
	}

	/**
	 * @see org.eclipse.team.internal.core.subscribers.CheckedInChangeSet#getAuthor()
	 */
	@Override
	public String getAuthor() {
		return changeset.getUser();
	}

	/**
	 * @see org.eclipse.team.internal.core.subscribers.CheckedInChangeSet#getDate()
	 */
	@Override
	public Date getDate() {
		return changeset.getTimestamp().getDate();
	}

	/**
	 * @see org.eclipse.team.internal.core.subscribers.ChangeSet#getComment()
	 */
	@Override
	public String getComment() {
		return changeset.getMessage();
	}

}
