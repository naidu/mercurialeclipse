/*******************************************************************************
 * Copyright (c) 2007-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Stefan Groschupf          - logError
 *     Stefan C                  - Code cleanup
 *     Andrei Loskutov           - bug fixes
 *     John Peberdy              - optimization
 *******************************************************************************/
package com.vectrace.MercurialEclipse.history;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.ITag;
import org.eclipse.team.core.history.provider.FileRevision;

import com.vectrace.MercurialEclipse.commands.HgBisectClient.Status;
import com.vectrace.MercurialEclipse.history.GraphLayout.GraphRow;
import com.vectrace.MercurialEclipse.model.HgFile;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IChangeSetHolder;
import com.vectrace.MercurialEclipse.model.IHgFile;
import com.vectrace.MercurialEclipse.model.IHgResource;
import com.vectrace.MercurialEclipse.model.IResourceHolder;
import com.vectrace.MercurialEclipse.model.JHgChangeSet;
import com.vectrace.MercurialEclipse.model.Signature;
import com.vectrace.MercurialEclipse.model.Tag;
import com.vectrace.MercurialEclipse.properties.DoNotDisplayMe;

/**
 * @author zingo
 */
public class MercurialRevision extends FileRevision implements IHgResource, IChangeSetHolder, IResourceHolder {

	private final IResource resource;
	private final JHgChangeSet changeSet;
	private final int revision;
	private final Signature signature;

	// .. cached data

	private IHgFile storage;

	/**
	 * Tags sorted by revision
	 * @see #pendingTags
	 */
	private Tag [] tags;

	/**
	 *  List of unsorted tags not yet added to {@link #tags}. May be null.
	 *  @see #tags
	 */
	private List<Tag> pendingTags;

	private Status bisectStatus;

	private GraphRow graphRow;

	/**
	 * @param changeSet must be non null
	 * @param gChangeSet may be null
	 * @param resource must be non null
	 * @param sig may be null
	 */
	public MercurialRevision(JHgChangeSet changeSet, IResource resource,
			Signature sig, Status bisectStatus) {
		super();
		Assert.isNotNull(changeSet);
		Assert.isNotNull(resource);
		this.changeSet = changeSet;
		this.revision = changeSet.getIndex();
		this.resource = resource;
		this.signature = sig;
		this.bisectStatus = bisectStatus;
	}

	public Signature getSignature() {
		return signature;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result	+  changeSet.hashCode();
		result = prime * result	+ resource.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof MercurialRevision)) {
			return false;
		}
		MercurialRevision other = (MercurialRevision) obj;
		if (!changeSet.equals(other.changeSet)) {
			return false;
		}
		if (!resource.equals(other.resource)) {
			return false;
		}
		return true;
	}

	/**
	 * @return never null
	 */
	public JHgChangeSet getChangeSet() {
		return changeSet;
	}

	@DoNotDisplayMe
	public String getName() {
		return resource.getName();
	}

	@Override
	@DoNotDisplayMe
	public boolean exists() {
		return true;
	}

	@Override
	@DoNotDisplayMe
	public String getContentIdentifier() {
		return changeSet.getNode();
	}

	@Override
	public String getAuthor() {
		return changeSet.getAuthor();
	}

	@Override
	public String getComment() {
		return changeSet.getComment();
	}

	@Override
	public ITag[] getBranches()
	{
		final String name = changeSet.getBranch();

		ITag branch = new ITag() {
			public String getName() {
				return name;
			}
		};

		ITag[] branches = {branch};

		return branches;
	}

	@Override
	@DoNotDisplayMe
	public long getTimestamp() {
		return changeSet.getDate().getTime();
	}

	@Override
	public URI getURI() {
		return resource.getLocationURI();
	}

	@Override
	public Tag [] getTags() {
		if (pendingTags != null) {
			processPendingTags();
		}
		if(tags == null) {
			return changeSet.getTags();
		}
		return tags;
	}

	/**
	 * @return never returns null
	 */
	public String getTagsString(){
		StringBuilder sb = new StringBuilder();
		Tag[] allTags = getTags();
		for (int i = 0; i < allTags.length; i++) {
			sb.append(allTags[i].getName());
			if(i < allTags.length - 1) {
				sb.append(", "); //$NON-NLS-1$
			}
		}
		return sb.toString();
	}

	/**
	 * Allows to add extra tags, not contained in the underlying changeset, to this revision.
	 * The point is: we want to be able to show tag information on revisions of particular
	 * files, which was NOT directly tagged, but we want to know which existing tags are
	 * covered by the version.
	 *
	 * @param newTag must be non null
	 */
	public void addTag(Tag newTag) {
		if(newTag == null) {
			return;
		}
		if (pendingTags == null) {
			pendingTags = new ArrayList<Tag>(4);
		}
		pendingTags.add(newTag);
	}

	private void processPendingTags() {
		if (pendingTags == null) {
			return;
		}

		SortedSet<Tag> all = new TreeSet<Tag>();

		all.addAll(pendingTags);
		pendingTags = null;

		if(tags != null) {
			for (Tag tag : tags) {
				if(tag != null) {
					all.add(tag);
				}
			}
		}
		for (Tag tag : changeSet.getTags()) {
			if(tag != null) {
				all.add(tag);
			}
		}
		this.tags = all.toArray(new Tag[all.size()]);
		Arrays.sort(tags);
	}

	/**
	 * Cleans up all extra tags we probably have on this revision
	 */
	public void cleanupExtraTags(){
		tags = null;
		pendingTags = null;
	}

	public IStorage getStorage(IProgressMonitor monitor) throws CoreException {
		if (storage == null) {
			if(resource instanceof IFile) {
				storage = new HgFile(changeSet.getHgRoot(), changeSet, changeSet
						.getHgRoot().getRelativePath(resource));
			}
		}
		return storage;
	}

	@DoNotDisplayMe
	public boolean isPropertyMissing() {
		return false;
	}

	public IFileRevision withAllProperties(IProgressMonitor monitor)
			throws CoreException {
		return this;
	}

	/**
	 * @return the revision
	 */
	public int getRevision() {
		return revision;
	}

	/**
	 * @return never null
	 */
	@DoNotDisplayMe
	public IResource getResource() {
		return resource;
	}

	/**
	 * @return true, if the resource represented by this revision is file
	 */
	@DoNotDisplayMe
	public boolean isFile(){
		return resource instanceof IFile;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("hg rev ["); //$NON-NLS-1$
		builder.append("revision="); //$NON-NLS-1$
		builder.append(revision);
		builder.append(", "); //$NON-NLS-1$
		builder.append("changeSet="); //$NON-NLS-1$
		builder.append(changeSet);
		builder.append(", "); //$NON-NLS-1$
		builder.append("resource="); //$NON-NLS-1$
		builder.append(resource);
		builder.append(", "); //$NON-NLS-1$
		if (signature != null) {
			builder.append("signature="); //$NON-NLS-1$
			builder.append(signature);
			builder.append(", "); //$NON-NLS-1$
		}
		if (tags != null) {
			builder.append("tags="); //$NON-NLS-1$
			builder.append(Arrays.asList(tags));
		}
		if (pendingTags != null) {
			builder.append("pendingTags="); //$NON-NLS-1$
			builder.append(pendingTags);
		}
		builder.append("]"); //$NON-NLS-1$
		return builder.toString();
	}

	/**
	 * @return the bisectStatus
	 */
	public Status getBisectStatus() {
		return bisectStatus;
	}

	/**
	 * @param bisectStatus the bisectStatus to set
	 */
	public void setBisectStatus(Status bisectStatus) {
		this.bisectStatus = bisectStatus;
	}

	public Object getAdapter(Class adapter) {
		if(adapter == IResource.class) {
			return resource;
		}
		return null;
	}

	public HgRoot getHgRoot() {
		return changeSet.getHgRoot();
	}

	public IPath getIPath() {
		return getHgRoot().toRelative(resource.getLocation());
	}

	public boolean isReadOnly() {
		return true;
	}

	/**
	 * @return The graph row for the changeset table. Only non-null if this is in the history view.
	 */
	public GraphRow getGraphRow() {
		return graphRow;
	}

	public void setGraphRow(GraphRow graphRow) {
		this.graphRow = graphRow;
	}
}
