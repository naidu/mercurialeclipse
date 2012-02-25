/*******************************************************************************
 * Copyright (c) 2007-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Charles O'Farrell         - HgRevision
 *     Bastian Doetsch			 - some more info fields
 *     Andrei Loskutov           - bug fixes
 *     Zsolt Koppany (Intland)   - bug fixes
 *     Adam Berkes (Intland)     - bug fixes
 *     Philip Graf               - bug fix
 *******************************************************************************/
package com.vectrace.MercurialEclipse.model;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;

import com.vectrace.MercurialEclipse.HgRevision;
import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgParentClient;
import com.vectrace.MercurialEclipse.commands.HgTagClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.utils.ChangeSetUtils;
import com.vectrace.MercurialEclipse.utils.StringUtils;

/**
 * @deprecated
 */
@Deprecated
public class DumbChangeSet extends ChangeSet {

	private static final Tag[] EMPTY_TAGS = new Tag[0];
	private static final SimpleDateFormat INPUT_DATE_FORMAT = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm Z");

	private final HgRevision revision;
	private final int changesetIndex;
	private final String changeset;
	private final String branch;
	private final String user;
	private final String date;
	private String tagsStr;
	private String comment;
	private String nodeShort;
	private String[] parents;
	private Date realDate;
	File bundleFile;
	private IHgRepositoryLocation repository;
	Direction direction;
	private final HgRoot hgRoot;
	Set<IFile> files;
	private Tag[] tags;

	/**
	 * This class is getting too tangled up with everything else, has a a large amount of fields
	 * (17) and worse is that it is not immutable, which makes the entanglement even more dangerous.
	 *
	 * My plan is to make it immutable by using the builder pattern and remove all setters.
	 * FileStatus fetching may(or may not) be feasable to put elsewhere or fetched "on-demand" by
	 * this class itself. Currently, it has no operations and it purely a data class which isn't
	 * very OO efficent.
	 *
	 * Secondly, remove getDirection by tester methods (isIncoming, isOutgoing, isLocal)
	 * @deprecated
	 */
	@Deprecated
	public static class Builder {
		private DumbChangeSet cs;

		public Builder(int revision, String changeSet, String branch, String date, String user,
				HgRoot root) {
			this.cs = new DumbChangeSet(revision, changeSet, user, date, branch == null ? "" : branch,
					root);
		}

		public Builder tags(String tags) {
			this.cs.tagsStr = tags;
			return this;
		}

		public Builder description(String description) {
			cs.setComment(description);
			return this;
		}

		public Builder parents(String[] parents) {
			this.cs.setParents(parents);
			return this;
		}

		public Builder direction(Direction direction) {
			this.cs.direction = direction;
			return this;
		}

		public Builder changedFiles(FileStatus[] changedFiles) {
			this.cs.changedFiles = changedFiles == null ? EMPTY_STATUS : Collections
					.unmodifiableList(Arrays.asList(changedFiles));
			return this;
		}

		public Builder bundleFile(File bundleFile) {
			this.cs.bundleFile = bundleFile;
			return this;
		}

		public Builder repository(IHgRepositoryLocation repository) {
			this.cs.repository = repository;
			return this;
		}

		// nodeShort should be first X of changeset, this is superfluous
		public Builder nodeShort(String nodeShort) {
			this.cs.nodeShort = nodeShort;
			return this;
		}

		public ChangeSet build() {
			ChangeSet result = this.cs;
			this.cs = null;
			return result;
		}
	}

	DumbChangeSet(int changesetIndex, String changeSet, String tags, String branch, String user,
			String date, String description, String[] parents, HgRoot root) {
		this.changesetIndex = changesetIndex;
		this.changeset = changeSet;
		this.revision = new HgRevision(changeset, changesetIndex);
		this.tagsStr = tags;
		this.branch = branch;
		this.user = user;
		this.date = date;
		this.hgRoot = root;
		setComment(description);
		setParents(parents);
		// remember index:fullchangesetid
		setName(getIndexAndName());
	}

	private DumbChangeSet(int changesetIndex, String changeSet, String user, String date,
			String branch, HgRoot root) {
		this(changesetIndex, changeSet, null, branch, user, date, "", null, root); //$NON-NLS-1$
	}

	@Override
	public int getChangesetIndex() {
		return changesetIndex;
	}

	@Override
	public String getChangeset() {
		return changeset;
	}

	/**
	 * @return tags array (all tags associated with current changeset). May return empty array, but
	 *         never null
	 * @see ChangeSetUtils#getPrintableTagsString(ChangeSet)
	 */
	@Override
	public Tag[] getTags() {
		if (tags == null) {
			if (!StringUtils.isEmpty(tagsStr)) {
				tags = HgTagClient.getTags(hgRoot, tagsStr.split("_,_"));
			}
			if (tags == null) {
				tags = EMPTY_TAGS;
			}
		}
		return tags;
	}

	/**
	 * @return the tagsStr
	 */
	@Override
	public String getTagsStr() {
		return tagsStr;
	}

	@Override
	public String getBranch() {
		return branch;
	}

	@Override
	public String getComment() {
		return comment;
	}

	@Override
	public HgRevision getRevision() {
		return revision;
	}

	@Override
	public String toString() {
		return getIndexAndName();
	}

	protected String getIndexAndName() {
		if (nodeShort != null) {
			return changesetIndex + ":" + nodeShort; //$NON-NLS-1$
		}
		return changesetIndex + ":" + changeset; //$NON-NLS-1$
	}

	/**
	 * @return the nodeShort
	 */
	@Override
	public String getNodeShort() {
		return nodeShort;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj instanceof ChangeSet) {
			ChangeSet other = (ChangeSet) obj;
			if (getChangeset().equals(other.getChangeset())
					&& getChangesetIndex() == other.getChangesetIndex()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		return 31 + ((changeset == null) ? 0 : changeset.hashCode()) + changesetIndex;
	}

	/**
	 * @return never returns null. Returns {@link ChangeSet#UNKNOWN_DATE} if the date can't be
	 *         parsed
	 */
	@Override
	public Date getRealDate() {
		try {
			if (realDate == null) {
				if (date != null) {
					// needed because static date format instances are not thread safe
					synchronized (INPUT_DATE_FORMAT) {
						realDate = INPUT_DATE_FORMAT.parse(date);
					}
				} else {
					realDate = UNKNOWN_DATE;
				}
			}
		} catch (ParseException e) {
			realDate = UNKNOWN_DATE;
		}
		return realDate;
	}

	/**
	 * @return the bundleFile, may be null. The file can contain additional changeset information,
	 *         if this is a changeset used by "incoming" or "pull" operation
	 */
	@Override
	public File getBundleFile() {
		return bundleFile;
	}

	@Override
	public String[] getParents() {
		getParentRevision(0);
		return parents;
	}

	/**
	 * @see com.vectrace.MercurialEclipse.model.ChangeSet#getParentRevision(int)
	 */
	@Override
	public HgRevision getParentRevision(int ordinal) {
		if (getChangesetIndex() != 0 && (parents == null || parents.length == 0)) {
			try {
				parents = HgParentClient.getParentNodeIds(this, "{rev}:{node}");
			} catch (HgException e) {
				MercurialEclipsePlugin.logError(e);
			}
		}

		if (parents != null && 0 <= ordinal && ordinal < parents.length) {
			return HgRevision.parse(parents[ordinal]);
		}

		return null;
	}

	public void setParents(String[] parents) {
		// filter null parents (hg uses -1 to signify a null parent)
		if (parents != null) {
			List<String> temp = new ArrayList<String>(parents.length);
			for (int i = 0; i < parents.length; i++) {
				String parent = parents[i];
				if (parent.charAt(0) != '-') {
					temp.add(parent);
				}
			}
			this.parents = temp.toArray(new String[temp.size()]);
		}
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
	public IHgRepositoryLocation getRepository() {
		return repository;
	}

	/**
	 * @return the direction
	 */
	@Override
	public Direction getDirection() {
		return direction;
	}

	/**
	 * @return the hgRoot file (always as <b>canonical path</b>)
	 * @see File#getCanonicalPath()
	 */
	@Override
	public HgRoot getHgRoot() {
		return hgRoot;
	}



	@Override
	public String getAuthor() {
		return user;
	}

	@Override
	public Date getDate() {
		return getRealDate();
	}
}
