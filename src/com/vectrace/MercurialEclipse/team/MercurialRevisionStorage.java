/*******************************************************************************
 * Copyright (c) 2006-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Stefan C                  - Code cleanup
 *     Bastian Doetsch           - additions for sync
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgCatClient;
import com.vectrace.MercurialEclipse.commands.HgLogClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.utils.PatchUtils;

/**
 * @author zingo
 *
 *         This is a IStorage subclass that can handle file revision
 *
 */
public class MercurialRevisionStorage implements IStorage {
	private static final ByteArrayInputStream EMPTY_STREAM = new ByteArrayInputStream(new byte[0]);
	private int revision;
	private String global;
	private final IFile resource;
	protected ChangeSet changeSet;
	protected ContentHolder content;
	private File parent;

	protected class ContentHolder {
		private final byte[] bytes;
		private final String string;
		private Throwable error;

		private ContentHolder(byte [] b, String str, Throwable t) {
			bytes = b;
			string = str;
			error = t;
		}

		public ContentHolder(byte [] bytes) {
			this(bytes, null, null);
		}

		public ContentHolder(String string) {
			this(null, string, null);
		}

		public ContentHolder(Throwable t) {
			this(null, null, t);
		}

		private InputStream createStreamContent(String result) throws HgException {
			try {
				HgRoot root = MercurialTeamProvider.getHgRoot(resource);
				return new ByteArrayInputStream(result.getBytes(root.getEncoding().name()));
			} catch (UnsupportedEncodingException e) {
				error = e;
				// core API ignores exceptions from this method, so we need to log them here
				MercurialEclipsePlugin.logWarning("Failed to get revision content for " +
						MercurialRevisionStorage.this.toString(), e);
				return EMPTY_STREAM;
			}
		}

		public InputStream createStream() throws HgException {
			if (bytes != null) {
				return new ByteArrayInputStream(bytes);
			} else if(string != null && error == null){
				return createStreamContent(string);
			} else {
				return EMPTY_STREAM;
			}
		}

	}

	/**
	 * The recommended constructor to use is MercurialRevisionStorage(IResource res, String rev, String global,
	 * ChangeSet cs)
	 *
	 */
	public MercurialRevisionStorage(IFile res, String changeset) {
		super();
		resource = res;
		try {
			if(changeset != null) {
				this.changeSet = LocalChangesetCache.getInstance().getOrFetchChangeSetById(res, changeset);
			}
			if(changeSet != null){
				this.revision = changeSet.getChangesetIndex();
				this.global = changeSet.getChangeset();
			}
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
		}
	}

	/**
	 * Constructs a new MercurialRevisionStorage with the given params.
	 *
	 * @param res
	 *            the resource for which we want an IStorage revision
	 * @param rev
	 *            the changeset index as string
	 * @param global
	 *            the global hash identifier
	 * @param cs
	 *            the changeset object
	 */
	public MercurialRevisionStorage(IFile res, int rev, String global, ChangeSet cs) {
		super();
		this.revision = rev;
		this.global = global;
		this.resource = res;
		this.changeSet = cs;
	}

	/**
	 * Constructs an {@link MercurialRevisionStorage} with the newest local changeset available.
	 *
	 * @param res
	 *            the resource
	 */
	public MercurialRevisionStorage(IFile res) {
		super();
		this.resource = res;
		ChangeSet cs = null;
		try {
			cs = LocalChangesetCache.getInstance().getChangesetByRootId(res);
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
		}
		if(cs != null){
			this.revision = cs.getChangesetIndex(); // should be fetched
			// from id
			this.global = cs.getChangeset();
		}
		this.changeSet = cs;
	}

	/**
	 * @param parent the parent (ancestor file name before rename/copy), might be null
	 */
	public void setParent(File parent) {
		this.parent = parent;
	}

	@SuppressWarnings("unchecked")
	public Object getAdapter(Class adapter) {
		if (adapter.equals(IResource.class)) {
			return resource;
		}
		return null;
	}

	/**
	 * Generate data content of the so called "file" in this case a revision, e.g. a hg cat --rev "rev" %file%
	 * <p>
	 * {@inheritDoc}
	 */
	public InputStream getContents() throws CoreException {
		if(content != null){
			return content.createStream();
		}
		try {
			IFile file = resource.getProject().getFile(resource.getProjectRelativePath());
			content = fetchContent(file);
		} catch (CoreException e) {

			if(parent != null){
				IFile file = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(
						new Path(parent.getAbsolutePath()));
				try {
					content = fetchContent(file);
					return content.createStream();
				} catch (CoreException e2) {
					e = e2;
				}
			}
			content = new ContentHolder(e);
			// core API ignores exceptions from this method, so we need to log them here
			MercurialEclipsePlugin.logWarning("Failed to get revision content for " + toString(), e);
			throw e;
		}

		return content.createStream();
	}

	private ContentHolder fetchContent(IFile file) throws CoreException {
		if (changeSet == null) {
			// no changeset known
			return new ContentHolder(HgCatClient.getContent(file, null));
		}
		String result;
		// Setup and run command
		if (changeSet.getDirection() == Direction.INCOMING && changeSet.getBundleFile() != null) {
			// incoming: overlay repository with bundle and extract then via cat
			try {
				result = HgCatClient.getContentFromBundle(file,
						changeSet.getRevision().getChangeset(),
						changeSet.getBundleFile().getCanonicalPath());
			} catch (IOException e) {
				throw new HgException("Unable to determine canonical path for " + changeSet.getBundleFile(), e);
			}
		} else if (changeSet.getDirection() == Direction.OUTGOING) {
			return new ContentHolder(PatchUtils.getPatchedContentsAsBytes(file, changeSet.getPatches(), true));
		} else {
			// local: get the contents via cat
			if(file.exists() && MercurialStatusCache.getInstance().isUnknown(file)){
				// for existing but unknown files, simply return dummy content
				return new ContentHolder((byte[])null);
			}
			result = HgCatClient.getContent(file, Integer.valueOf(changeSet.getChangesetIndex()).toString());
		}
		return new ContentHolder(result);
	}

	public IPath getFullPath() {
		return resource.getFullPath().append(revision != 0 ? (" [" + revision + "]") //$NON-NLS-1$ //$NON-NLS-2$
				: Messages.getString("MercurialRevisionStorage.parentChangeset")); //$NON-NLS-1$
	}

	public String getName() {
		// the getContents() call below is a workaround for the fact that the core API
		// seems to ignore the failures in getContents() and do not update the storage name
		// in this case. So we just call getContents (which result is buffered in any case)
		// here, and remember the possible error.
		try {
			getContents();
		} catch (CoreException e) {
			content = new ContentHolder(e);
		}
		String name;
		if (changeSet != null) {
			name = resource.getName() + " [" + changeSet.toString() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			name = resource.getName();
		}
		if(content.error != null){
			String message = content.error.getMessage();
			if (message.indexOf('\n') > 0) {
				// name = message + ", " + name;
				name = message.substring(0, message.indexOf('\n'));
			} else {
				name = message;
			}
		}
		return name;
	}

	public int getRevision() {
		return revision;
	}

	/**
	 * You can't write to other revisions then the current selected e.g. ReadOnly
	 */
	public boolean isReadOnly() {
		if (revision != 0) {
			return true;
		}
		// if no revision resource is the current one e.g. editable :)
		ResourceAttributes attributes = resource.getResourceAttributes();
		if (attributes != null) {
			return attributes.isReadOnly();
		}
		return true; /* unknown state marked as read only for safety */
	}

	public IFile getResource() {
		return resource;
	}

	public String getGlobal() {
		return global;
	}

	/**
	 * @return the changeSet
	 */
	public ChangeSet getChangeSet() {
		return changeSet;
	}

	/**
	 * This constructor is not recommended, as the revision index is not unique when working with other than the local
	 * repository.
	 *
	 * @param res
	 * @param rev
	 */
	public MercurialRevisionStorage(IFile res, int rev) {
		super();
		resource = res;
		if(rev < 0){
			return;
		}
		try {
			ChangeSet tip = HgLogClient.getTip(res.getProject());
			boolean localKnown = tip.getChangesetIndex() >= rev;
			if(!localKnown){
				return;
			}
			this.changeSet = LocalChangesetCache.getInstance().getOrFetchChangeSetById(res, String.valueOf(rev));
			if(changeSet != null){
				this.revision = changeSet.getChangesetIndex();
				this.global = changeSet.getChangeset();
			}
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Hg revision [");
		if (changeSet != null) {
			builder.append("changeSet=");
			builder.append(changeSet);
			builder.append(", ");
		}
		if (revision != 0) {
			builder.append("revision=");
			builder.append(revision);
			builder.append(", ");
		}
		if (global != null) {
			builder.append("global=");
			builder.append(global);
			builder.append(", ");
		}
		if (resource != null) {
			builder.append("resource=");
			builder.append(resource);
			builder.append(", ");
		}
		if (parent != null) {
			builder.append("parent=");
			builder.append(parent);
			builder.append(", ");
		}
		builder.append("]");
		return builder.toString();
	}
}
