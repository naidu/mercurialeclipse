/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
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
 *     John Peberdy              - refactoring
 *******************************************************************************/
package com.vectrace.MercurialEclipse.model;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.team.core.synchronize.SyncInfoTree;
import org.eclipse.team.internal.core.subscribers.CheckedInChangeSet;

import com.aragost.javahg.commands.StatusResult;
import com.aragost.javahg.commands.flags.StatusCommandFlags;
import com.vectrace.MercurialEclipse.HgRevision;
import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.FileStatus.Action;
import com.vectrace.MercurialEclipse.properties.DoNotDisplayMe;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;
import com.vectrace.MercurialEclipse.utils.StringUtils;

/**
 * TODO: why extend CheckedInChangeSet?
 */
@SuppressWarnings("restriction")
public abstract class ChangeSet extends CheckedInChangeSet implements Comparable<ChangeSet> {

	protected static final List<FileStatus> EMPTY_STATUS = Collections
			.unmodifiableList(new ArrayList<FileStatus>());
	private final IFile[] EMPTY_FILES = new IFile[0];

	private static final SimpleDateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm");

	protected static final Date UNKNOWN_DATE = new Date(0);

	public static enum Direction {
		INCOMING, OUTGOING, LOCAL;
	}

	// .. cached data

	protected List<FileStatus> changedFiles;

	private Set<IFile> files;

	// constructor

	public ChangeSet() {
	}

	// operations

	public int compareTo(ChangeSet o) {
		if (o.getChangeset().equals(this.getChangeset())) {
			return 0;
		}
		int result = this.getChangesetIndex() - o.getChangesetIndex();
		if (result != 0) {
			return result;
		}
		if (getRealDate() != UNKNOWN_DATE && o.getRealDate() != UNKNOWN_DATE) {
			return getRealDate().compareTo(o.getRealDate());
		}
		return 0;
	}

	public abstract Date getRealDate();

	public abstract int getChangesetIndex();

	public abstract String getChangeset();

	/**
	 * Returns index:nodeId
	 *
	 * @see org.eclipse.team.internal.core.subscribers.ChangeSet#getName()
	 */
	@Override
	public final String getName() {
		return super.getName();
	}

	/**
	 * @see org.eclipse.team.internal.core.subscribers.ChangeSet#setName(java.lang.String)
	 */
	@Override
	protected void setName(String name) {
		super.setName(name);
	}

	/**
	 * @see org.eclipse.team.internal.core.subscribers.ChangeSet#getComment()
	 */
	@Override
	public abstract String getComment();

	/**
	 * @see org.eclipse.team.internal.core.subscribers.CheckedInChangeSet#getAuthor()
	 */
	@Override
	public abstract String getAuthor();

	/**
	 * @see org.eclipse.team.internal.core.subscribers.CheckedInChangeSet#getDate()
	 */
	@Override
	public abstract Date getDate();

	/**
	 * @return not modifiable set of files changed/added/removed in this changeset, never null. The
	 *         returned file references might not exist (yet/anymore) on the disk or in the Eclipse
	 *         workspace.
	 */
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

	/**
	 * @param resource
	 *            non null
	 * @return true if the given resource was removed in this changeset
	 */
	public final boolean isRemoved(IResource resource) {
		return contains(resource, FileStatus.Action.REMOVED);
	}

	/**
	 * @param resource
	 *            non null
	 * @return true if the given resource was moved in this changeset
	 */
	public final boolean isMoved(IResource resource) {
		return contains(resource, FileStatus.Action.MOVED);
	}

	/**
	 * @param resource
	 *            non null
	 * @return file status object if this changeset contains given resource, null otherwise
	 */
	public final FileStatus getStatus(IResource resource) {
		if (getChangedFiles().isEmpty()) {
			return null;
		}
		IPath path = ResourceUtils.getPath(resource);
		if (path.isEmpty()) {
			return null;
		}
		for (FileStatus fileStatus : getChangedFiles()) {
			if (path.equals(fileStatus.getAbsolutePath())) {
				return fileStatus;
			}
		}
		return null;
	}

	/**
	 * @param resource
	 *            non null
	 * @param action
	 *            non null
	 * @return true if this changeset contains a resource with given action state
	 */
	private boolean contains(IResource resource, Action action) {
		if (getChangedFiles().isEmpty()) {
			return false;
		}
		boolean match = false;
		IPath path = null;
		for (FileStatus fileStatus : getChangedFiles()) {
			if (fileStatus.getAction() == action) {
				if (path == null) {
					path = ResourceUtils.getPath(resource);
				}
				if (path.equals(fileStatus.getAbsolutePath())) {
					match = true;
					break;
				}
			}
		}
		return match;
	}

	public final String getSummary() {
		return StringUtils.removeLineBreaks(getComment());
	}

	public abstract Tag[] getTags();

	/**
	 * @return the ageDate
	 */
	public final String getAgeDate() {
		double delta = (System.currentTimeMillis() - getRealDate().getTime());

		delta /= 1000 * 60; // units is minutes

		if (delta <= 1) {
			return "less than a minute ago";
		}

		if (delta <= 60) {
			return makeAgeString(delta, "minute");
		}

		delta /= 60;
		if (delta <= 24) {
			return makeAgeString(delta, "hour");
		}

		// 1 day to 31 days
		delta /= 24; // units is days
		if (delta <= 31) {
			return makeAgeString(delta, "day");
		}

		// 4 weeks - 3 months
		if (delta / 7 <= 12) {
			return makeAgeString(delta / 7, "week");
		}

		// 3 months - 1 year
		if (delta / 30 <= 12) {
			return makeAgeString(delta / 30, "month");
		}

		return makeAgeString(delta / 365, "year");
	}

	private static String makeAgeString(double d, String unit) {
		int i = (int) Math.max(1, Math.round(d));

		return i + " " + unit + ((i == 1) ? "" : "s") + " ago";
	}

	public abstract String getBranch();

	public abstract HgRoot getHgRoot();

	/**
	 * Roughly corresponds to Mercurial's template filter named "person" except if
	 * an email is provided includes everything left of the '@'.
	 *
	 * See also: http://www.javaforge.com/issue/13809
	 * <pre>
	 * def person(author):
	 * '''get name of author, or else username.'''
	 * if not '@' in author:
	 *     return author
	 * f = author.find('<')
	 * if f == -1:
	 *     return util.shortuser(author)
	 * return author[:f].rstrip()
	 * </pre>
	 */
	@DoNotDisplayMe
	public final String getPerson() {
		String sUser = getAuthor();

		if (sUser != null) {
			int a = sUser.indexOf('@');

			if (a < 0) {
				return sUser;
			}

			int b = sUser.indexOf('<');

			if (b >= 0) {
				return sUser.substring(0, b).trim();
			}
			return sUser.substring(0, a).trim();
		}

		return null;
	}

	public abstract String getNodeShort();

	public abstract HgRevision getRevision();

	/**
	 * @return Whether the repository is currently on this revision
	 */
	public final boolean isCurrentOutgoing() {
		return getDirection() == Direction.OUTGOING && isCurrent();
	}

	public abstract Direction getDirection();

	public final String getDateString() {
		Date d = getRealDate();
		if (d != null) {
			return formatDate(d);
		}
		return null;
	}

	public static String formatDate(Date d) {
		// needed because static date format instances are not thread safe
		synchronized (DISPLAY_DATE_FORMAT) {
			return DISPLAY_DATE_FORMAT.format(d);
		}
	}

	public abstract File getBundleFile();

	public abstract String[] getParents();

	/**
	 * @return the changedFiles, never null. The returned list is non modifiable so any attempt to
	 *         modify it will lead to an exception.
	 */
	public final List<FileStatus> getChangedFiles() {
		HgRoot hgRoot = getHgRoot();

		if (changedFiles == null) {
			List<FileStatus> l = new ArrayList<FileStatus>();

			StatusResult res = StatusCommandFlags.on(hgRoot.getRepository())
					.rev(getParentRevision(0).getChangeset(), getRevision().getChangeset()).added()
					.modified().deleted().removed().copies().execute();

			for (Iterator<String> it = res.getModified().iterator(); it.hasNext();) {
				l.add(new FileStatus(FileStatus.Action.MODIFIED, it.next(), hgRoot));
			}

			for (Iterator<String> it = res.getAdded().iterator(); it.hasNext();) {
				l.add(new FileStatus(FileStatus.Action.ADDED, it.next(), hgRoot));
			}

			for (Iterator<String> it = res.getRemoved().iterator(); it.hasNext();) {
				l.add(new FileStatus(FileStatus.Action.REMOVED, it.next(), hgRoot));
			}

			// TODO moves
			for (Iterator<String> it = res.getCopied().keySet().iterator(); it.hasNext();) {
				String s = it.next();
				String source = res.getCopied().get(s);
				l.add(new FileStatus(
						res.getRemoved().contains(source) ? FileStatus.Action.MOVED
								: FileStatus.Action.COPIED, s, source, hgRoot));
			}

			if (l.isEmpty()) {
				changedFiles = EMPTY_STATUS;
			} else {
				changedFiles = Collections.unmodifiableList(l);
			}
		}

		return changedFiles;
	}

	public abstract IHgRepositoryLocation getRepository();

	/**
	 * @return True if this is a merge changeset.
	 */
	public final boolean isMerge() {
		String[] parents = getParents();

		return parents != null && 1 < parents.length && !StringUtils.isEmpty(parents[0])
				&& !StringUtils.isEmpty(parents[1]);
	}

	public abstract HgRevision getParentRevision(int i);

	public abstract String getTagsStr();

	/**
	 * @return Whether the repository is currently on this revision
	 */
	public final boolean isCurrent() {
		HgRoot hgRoot = getHgRoot();

		if (hgRoot != null) {
			try {
				return equals(LocalChangesetCache.getInstance().getChangesetForRoot(hgRoot));
			} catch (HgException e) {
				MercurialEclipsePlugin.logError(e);
			}
		}

		return false;
	}

	public String getUser() {
		return getAuthor();
	}

	@Override
	public boolean contains(IResource local) {
		return getFiles().contains(local);
	}

	public boolean contains(IPath local) {
		for (IFile resource : getFiles()) {
			if (local.equals(ResourceUtils.getPath(resource))) {
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
	 * This method should NOT be used directly by clients of Mercurial plugin except those from
	 * "synchronize" packages. It exists only to fulfill contract with Team "synchronize" API and is
	 * NOT performant, as it may create dynamic proxy objects. {@inheritDoc}
	 */
	@Override
	@DoNotDisplayMe
	public IFile[] getResources() {
		return getFiles().toArray(EMPTY_FILES);
	}

	@DoNotDisplayMe
	public FileFromChangeSet[] getChangesetFiles() {
		List<FileFromChangeSet> fcs = new ArrayList<FileFromChangeSet>();

		for (FileStatus fileStatus : getChangedFiles()) {
			int action = 0;
			int dir = 0;
			switch (fileStatus.getAction()) {
			case ADDED:
			case MOVED:
			case COPIED:
				action = Differencer.ADDITION;
				break;
			case MODIFIED:
				action = Differencer.CHANGE;
				break;
			case REMOVED:
				action = Differencer.DELETION;
				break;
			}
			switch (getDirection()) {
			case INCOMING:
				dir |= Differencer.LEFT;
				break;
			case OUTGOING:
				dir |= Differencer.RIGHT;
				break;
			case LOCAL:
				dir |= Differencer.RIGHT;
				break;
			}
			fcs.add(new FileFromChangeSet(this, fileStatus, action | dir));

			if (fileStatus.getAction() == FileStatus.Action.MOVED) {
				// for moved files, include an extra FileFromChangeset for the deleted file
				FileStatus fs = new FileStatus(Action.REMOVED, fileStatus
						.getRootRelativeCopySourcePath().toString(), getHgRoot());
				fcs.add(new FileFromChangeSet(this, fs, dir | Differencer.DELETION));
			}
		}
		return fcs.toArray(new FileFromChangeSet[0]);
	}

	@Override
	@DoNotDisplayMe
	public SyncInfoTree getSyncInfoSet() {
		return super.getSyncInfoSet();
	}

	@Override
	public boolean isEmpty() {
		return getChangedFiles().isEmpty();
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
