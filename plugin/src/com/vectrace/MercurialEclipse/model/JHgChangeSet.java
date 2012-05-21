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
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;

import com.aragost.javahg.Changeset;
import com.aragost.javahg.Phase;
import com.aragost.javahg.commands.flags.PhaseCommandFlags;
import com.vectrace.MercurialEclipse.HgFeatures;
import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.properties.DoNotDisplayMe;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * Changeset backed by a JavaHg changeset
 */
public class JHgChangeSet extends ChangeSet {

	public final static String NULL_ID = Changeset.NULL_ID;

	/**
	 * Not null
	 */
	private final Changeset changeset;

	/**
	 * Not null
	 */
	private final HgRoot hgRoot;

	/**
	 * Not null
	 */
	private final Direction direction;

	/**
	 * May be null
	 */
	private final IHgRepositoryLocation remote;

	/**
	 * May be null
	 */
	private final File bundle;

	// .. cached data

	private Set<IFile> files;

	protected List<FileStatus> changedFiles;

	private Tag[] tags;

	// constructors

	public JHgChangeSet(HgRoot hgRoot, Changeset changeset, IHgRepositoryLocation remote,
			Direction direction, File bundle) {
		this.changeset = changeset;
		this.hgRoot = hgRoot;
		this.remote = remote;
		this.direction = direction;
		this.bundle = bundle;

		setName(getIndex() + ":" + getNodeShort());

		Assert.isLegal(hgRoot != null && changeset != null && direction != null);
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
	 * @see com.vectrace.MercurialEclipse.model.ChangeSet#getIndex()
	 */
	@Override
	public int getIndex() {
		return changeset.getRevision();
	}

	/**
	 * @see com.vectrace.MercurialEclipse.model.ChangeSet#getNode()
	 */
	@Override
	public String getNode() {
		return changeset.getNode();
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
		if (changeset.getParent1() == null) {
			return new String[0];
		}

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

	public String getParentNode(int i) {
		Changeset cs;
		if (i == 0) {
			cs = changeset.getParent1();
		} else if (i == 1) {
			cs = changeset.getParent2();
		} else {
			return null;
		}

		if (cs == null) {
			return null;
		}

		return cs.getNode();
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

	@Override
	public final Tag[] getTags() {
		if (tags == null) {
			List<String> tagNames = changeset.tags();

			tags = new Tag[tagNames.size()];

			for (int i = 0; i < tags.length; i++) {
				tags[i] = Tag.makeLight(tagNames.get(i), changeset);
			}
		}
		return tags;
	}

	/**
	 * @see com.vectrace.MercurialEclipse.model.ChangeSet#getAuthor()
	 */
	@Override
	public String getAuthor() {
		return changeset.getUser();
	}

	/**
	 * @see com.vectrace.MercurialEclipse.model.ChangeSet#getDate()
	 */
	@Override
	public Date getDate() {
		return changeset.getTimestamp().getDate();
	}

	/**
	 * @see com.vectrace.MercurialEclipse.model.ChangeSet#getComment()
	 */
	@Override
	public String getComment() {
		return changeset.getMessage();
	}

	/**
	 * @return the changedFiles, never null. The returned list is non modifiable so any attempt to
	 *         modify it will lead to an exception.
	 */
	@Override
	public final List<FileStatus> getChangedFiles() {
		return getChangedFiles(0);
	}

	public final List<FileStatus> getChangedFiles(int index) {
		if (index == 0 && changedFiles != null) {
			return changedFiles;
		}

		List<FileStatus> status = HgStatusClient.getStatus(this, index);

		// Cache the first parent only
		if (index == 0) {
			changedFiles = status;
		}

		return status;
	}

	/**
	 * @return not modifiable set of files changed/added/removed in this changeset, never null. The
	 *         returned file references might not exist (yet/anymore) on the disk or in the Eclipse
	 *         workspace.
	 */
	@Override
	@DoNotDisplayMe
	public Set<IFile> getFiles() {
		if (files == null) {
			Set<IFile> files1 = new LinkedHashSet<IFile>();
			List<FileStatus> changed = getChangedFiles();
			if (changed != null) {
				for (FileStatus fileStatus : changed) {
					IFile fileHandle = ResourceUtils.getFileHandle(fileStatus.getAbsolutePath());
					if (fileHandle != null) {
						files1.add(fileHandle);
					}
				}
			}
			files = Collections.unmodifiableSet(files1);
		}
		return files;
	}

	@DoNotDisplayMe
	public Changeset getData() {
		return changeset;
	}

	/**
	 * @see com.vectrace.MercurialEclipse.model.ChangeSet#isCurrent()
	 */
	@Override
	public boolean isCurrent() {
		try {
			return getData().equals(
					LocalChangesetCache.getInstance().getCurrentChangeSet(hgRoot).getData());
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
		}

		return false;
	}

	/**
	 * Note callers must check {@link HgFeatures#PHASES} enablement.
	 *
	 * @return The current phase
	 */
	public Phase getPhase() {
		return getData().phase();
	}

	public void setDraft() {
		PhaseCommandFlags.on(hgRoot.getRepository()).draft().rev(getNode()).force().execute();
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((bundle == null) ? 0 : bundle.hashCode());
		result = prime * result + ((changeset == null) ? 0 : changeset.hashCode());
		result = prime * result + ((direction == null) ? 0 : direction.hashCode());
		result = prime * result + ((hgRoot == null) ? 0 : hgRoot.hashCode());
		result = prime * result + ((remote == null) ? 0 : remote.hashCode());
		return result;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		JHgChangeSet other = (JHgChangeSet) obj;
		if (bundle == null) {
			if (other.bundle != null) {
				return false;
			}
		} else if (!bundle.equals(other.bundle)) {
			return false;
		}
		if (changeset == null) {
			if (other.changeset != null) {
				return false;
			}
		} else if (!changeset.equals(other.changeset)) {
			return false;
		}
		if (direction != other.direction) {
			return false;
		}
		if (hgRoot == null) {
			if (other.hgRoot != null) {
				return false;
			}
		} else if (!hgRoot.equals(other.hgRoot)) {
			return false;
		}
		if (remote == null) {
			if (other.remote != null) {
				return false;
			}
		} else if (!remote.equals(other.remote)) {
			return false;
		}
		return true;
	}
}
