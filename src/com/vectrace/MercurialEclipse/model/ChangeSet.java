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
 *     Andrei Loskutov (Intland) - bug fixes
 *     Zsolt Koppany (Intland)   - bug fixes
 *     Adam Berkes (Intland)     - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.model;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.team.internal.core.subscribers.CheckedInChangeSet;

import com.vectrace.MercurialEclipse.HgRevision;
import com.vectrace.MercurialEclipse.model.FileStatus.Action;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;
import com.vectrace.MercurialEclipse.utils.StringUtils;

@SuppressWarnings("restriction")
public class ChangeSet extends CheckedInChangeSet implements Comparable<ChangeSet> {

	private static final IFile[] EMPTY_FILES = new IFile[0];
	private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm Z");
	public static final Date UNKNOWN_DATE = new Date(0);
	public static enum Direction {
		INCOMING, OUTGOING, LOCAL;
	}

	private final HgRevision revision;
	private final int changesetIndex;
	private final String changeset;
	private final String branch;
	private final String user;
	private final String date;
	private String tag;
	private FileStatus[] changedFiles;
	private String description;
	private String ageDate;
	private String nodeShort;
	private String[] parents;
	private Date realDate;
	File bundleFile;
	private HgRepositoryLocation repository;
	Direction direction;
	private final HgRoot hgRoot;
	Set<IFile> files;

	/**
	 *  A more or less dummy changeset containing only index and global id. Such
	 *  changeset is useful and can be constructed from the other changesets "parent" ids
	 */
	public static class ParentChangeSet extends ChangeSet {

		/**
		 * @param indexAndId a semicolon separated index:id pair
		 * @param child this changeset's child from which we are constructing the parent
		 */
		public ParentChangeSet(String indexAndId, ChangeSet child) {
			super(getIndex(indexAndId), getChangeset(indexAndId), null, null, null, null, "", null, child.getHgRoot()); //$NON-NLS-1$
			this.bundleFile = child.getBundleFile();
			this.direction = child.direction;
		}

		static int getIndex(String parentId){
			if(parentId == null || parentId.length() < 3){
				return 0;
			}
			String[] parts = parentId.split(":");
			if(parts.length != 2){
				return 0;
			}
			try {
				return Integer.valueOf(parts[0]).intValue();
			} catch (NumberFormatException e) {
				return 0;
			}
		}

		static String getChangeset(String parentId){
			if(parentId == null || parentId.length() < 3){
				return null;
			}
			String[] parts = parentId.split(":");
			if(parts.length != 2){
				return null;
			}
			try {
				return parts[1];
			} catch (NumberFormatException e) {
				return null;
			}
		}
	}

	/**
	 * This class is getting too tangled up with everything else, has a a large
	 * amount of fields (17) and worse is that it is not immutable, which makes
	 * the entanglement even more dangerous.
	 *
	 * My plan is to make it immutable by using the builder pattern and remove
	 * all setters. FileStatus fetching may(or may not) be feasable to put
	 * elsewhere or fetched "on-demand" by this class itself. Currently, it has
	 * no operations and it purely a data class which isn't very OO efficent.
	 *
	 * Secondly, remove getDirection by tester methods (isIncoming, isOutgoing,
	 * isLocal)
	 *
	 */
	public static class Builder {
		private ChangeSet cs;

		public Builder(int revision, String changeSet, String branch, String date, String user, HgRoot root) {
			this.cs = new ChangeSet(revision, changeSet, user, date, branch == null? "" : branch, root);
		}

		public Builder tag(String tag) {
			this.cs.tag = tag;
			return this;
		}

		public Builder description(String description) {
			cs.setDescription(description);
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
			this.cs.changedFiles = changedFiles;
			return this;
		}

		public Builder bundleFile(File bundleFile) {
			this.cs.bundleFile = bundleFile;
			return this;
		}

		public Builder repository(HgRepositoryLocation repository) {
			this.cs.repository = repository;
			return this;
		}

		// what is ageDate? Can it be derived from date and now()
		public Builder ageDate(String ageDate) {
			this.cs.ageDate = ageDate;
			return this;
		}

		// nodeShort should be first X of changeset, this is superflous
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

	ChangeSet(int changesetIndex, String changeSet, String tag,
			String branch, String user, String date, String description,
			String[] parents, HgRoot root) {
		this.changesetIndex = changesetIndex;
		this.changeset = changeSet;
		this.revision = new HgRevision(changeset, changesetIndex);
		this.tag = tag;
		this.branch = branch;
		this.user = user;
		this.date = date;
		this.hgRoot = root;
		setDescription(description);
		setParents(parents);
		// remember index:fullchangesetid
		setName(toString());
	}


	private ChangeSet(int changesetIndex, String changeSet, String user, String date, String branch, HgRoot root) {
		this(changesetIndex, changeSet, null, branch, user, date, "", null, root); //$NON-NLS-1$
	}

	public int getChangesetIndex() {
		return changesetIndex;
	}

	public String getChangeset() {
		return changeset;
	}

	public String getTag() {
		if (HgRevision.TIP.getChangeset().equals(tag) && bundleFile != null) {
			StringBuilder builder = new StringBuilder(tag).append(" [ ").append(repository.toString()).append(" ]"); //$NON-NLS-1$ //$NON-NLS-2$
			tag = builder.toString();
		}
		return tag;
	}

	public String getBranch() {
		return branch;
	}

	public String getUser() {
		return user;
	}

	public String getDateString() {
//      return date;
		String dt = date;
		if (dt != null) {
			// Return date without extra time-zone.
			int off = dt.lastIndexOf(' ');
			if (off != -1 && off < dt.length() - 1) {
				switch (dt.charAt(off + 1)) {
				case '+':
				case '-':
					dt = dt.substring(0, off);
					break;
				}
			}
		}
		return dt;
	}

	@Override
	public String getComment() {
		return description;
	}

	public HgRevision getRevision() {
		return revision;
	}

	@Override
	public String toString() {
		if (nodeShort != null) {
			return this.changesetIndex + ":" + this.nodeShort; //$NON-NLS-1$
		}
		return this.changesetIndex + ":" + this.changeset; //$NON-NLS-1$

	}

	/**
	 * @return the changedFiles
	 */
	public FileStatus[] getChangedFiles() {
		if (changedFiles != null) {
			// Don't let clients manipulate the array in-place
			return changedFiles.clone();
		}
		return new FileStatus[0];
	}

	/**
	 * @param resource  non null
	 * @return true if the given resource was removed in this changeset
	 */
	public boolean isRemoved(IResource resource) {
		return contains(resource, FileStatus.Action.REMOVED);
	}

	/**
	 * @param resource  non null
	 * @return true if the given resource was added in this changeset
	 */
	public boolean isAdded(IResource resource) {
		return contains(resource, FileStatus.Action.ADDED);
	}

	/**
	 * @param resource  non null
	 * @return true if the given resource was modified in this changeset
	 */
	public boolean isModified(IResource resource) {
		return contains(resource, FileStatus.Action.MODIFIED);
	}

	/**
	 * @param resource non null
	 * @param action non null
	 * @return true if this changeset contains a resource with given action state
	 */
	private boolean contains(IResource resource, Action action) {
		if(changedFiles.length == 0){
			return false;
		}
		boolean match = false;
		IPath path = null;
		for (FileStatus fileStatus : changedFiles) {
			if(fileStatus.getAction() == action){
				if(path == null){
					path = ResourceUtils.getPath(resource);
				}
				if(path.equals(fileStatus.getAbsolutePath())){
					match = true;
					break;
				}
			}
		}
		return match;
	}

	/**
	 * @return the ageDate
	 */
	public String getAgeDate() {
		return ageDate;
	}

	/**
	 * @return the nodeShort
	 */
	public String getNodeShort() {
		return nodeShort;
	}

	public int compareTo(ChangeSet o) {
		if (o.getChangeset().equals(this.getChangeset())) {
			return 0;
		}
		int result = this.getChangesetIndex() - o.getChangesetIndex();
		if(result != 0){
			return result;
		}
		if (getRealDate() != UNKNOWN_DATE && o.getRealDate() != UNKNOWN_DATE) {
			return getRealDate().compareTo(o.getRealDate());
		}
		return 0;
	}

	@Override
	public boolean equals(Object obj) {
		if(this == obj){
			return true;
		}
		if (obj instanceof ChangeSet) {
			ChangeSet other = (ChangeSet) obj;
			if(getChangeset().equals(other.getChangeset())){
				return true;
			}
			if (date != null && date.equals(other.getDateString())) {
				return true;
			}

			// changeset indices are not equal in different repos, e.g. incoming
			// so we can't do a check solely based on indexes.
			return getChangesetIndex() == other.getChangesetIndex();
		}
		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((changeset == null) ? 0 : changeset.hashCode());
		return result;
	}

	/**
	 * @return never returns null. Returns {@link ChangeSet#UNKNOWN_DATE} if the date
	 * can't be parsed
	 */
	public Date getRealDate() {
		try {
			if (realDate == null) {
				if (date != null) {
					realDate = SIMPLE_DATE_FORMAT.parse(date);
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
	 * @return the bundleFile, may be null. The file can contain additional changeset
	 * information, if this is a changeset used by "incoming" or "pull" operation
	 */
	public File getBundleFile() {
		return bundleFile;
	}

	public String[] getParents() {
		return parents;
	}

	private void setParents(String[] parents) {
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

	private void setDescription(String description) {
		if (description != null) {
			this.description = description;
		} else {
			this.description = "";
		}
	}

	public String getSummary() {
		return StringUtils.removeLineBreaks(getComment());
	}

	/**
	 * @return the repository
	 */
	public HgRepositoryLocation getRepository() {
		return repository;
	}

	/**
	 * @return the direction
	 */
	public Direction getDirection() {
		return direction;
	}

	/**
	 * @return the hgRoot file (always as <b>canonical path</b>)
	 * @see File#getCanonicalPath()
	 */
	public HgRoot getHgRoot() {
		return hgRoot;
	}

	@Override
	public boolean contains(IResource local) {
		return getFiles().contains(local);
	}

	public boolean contains(IPath local) {
		for (IFile resource : getFiles()) {
			if(local.equals(resource.getLocation())){
				return true;
			}
		}
		return false;
	}


	@Override
	public boolean containsChildren(final IResource local, int depth) {
		return contains(local);
	}

	/**
	 * This method should NOT be used directly by clients of Mercurial plugin except
	 * those from "synchronize" packages. It exists only to fulfill contract with Team
	 * "synchronize" API and is NOT performant, as it may create dynamic proxy objects.
	 * {@inheritDoc}
	 */
	@Override
	public IFile[] getResources() {
		return getFiles().toArray(EMPTY_FILES);
	}

	public FileFromChangeSet[] getChangesetFiles(){
		List<FileFromChangeSet> fcs = new ArrayList<FileFromChangeSet>();
		if(changedFiles == null) {
			return fcs.toArray(new FileFromChangeSet[0]);
		}

		for (FileStatus fileStatus : changedFiles) {
			int kind = 0;
			switch (fileStatus.getAction()) {
			case ADDED:
				kind = Differencer.ADDITION;
				break;
			case MODIFIED:
				kind = Differencer.CHANGE;
				break;
			case REMOVED:
				kind = Differencer.DELETION;
				break;
			}
			switch(getDirection()){
				case INCOMING:
					kind |= Differencer.LEFT;
					break;
				case OUTGOING:
					kind |= Differencer.RIGHT;
					break;
				case LOCAL:
					kind |= Differencer.RIGHT;
					break;
			}
			fcs.add(new FileFromChangeSet(this, fileStatus, kind));
		}
		return fcs.toArray(new FileFromChangeSet[0]);
	}

	/**
	 * @return not modifiable set of files changed/added/removed in this changeset, never null.
	 * The returned file references might not exist (yet/anymore) on the disk or in the
	 * Eclipse workspace.
	 */
	public Set<IFile> getFiles(){
		if(files != null){
			return files;
		}
		Set<IFile> files1 = new HashSet<IFile>();
		if(changedFiles != null) {
			for (FileStatus fileStatus : changedFiles) {
				IFile fileHandle = ResourceUtils.getFileHandle(fileStatus.getAbsolutePath());
				if(fileHandle != null) {
					files1.add(fileHandle);
				}
			}
		}
		files = Collections.unmodifiableSet(files1);
		return files;
	}

	@Override
	public boolean isEmpty() {
		return changedFiles == null || changedFiles.length == 0;
	}

	@Override
	public String getAuthor() {
		return getUser();
	}

	@Override
	public Date getDate() {
		return getRealDate();
	}

	/**
	 * Returns index:fullchangesetid pair
	 */
	@Override
	public String getName() {
		return super.getName();
	}

	@Override
	public void remove(IResource resource) {
		// not supported
	}


	@Override
	public void rootRemoved(IResource resource, int depth) {
		// not supported
	}

}
