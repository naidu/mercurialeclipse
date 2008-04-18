/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Stefan C                  - Code cleanup
 *     Bastian Doetsch			 - additions for sync
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.SortedSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgCatClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;

/**
 * @author zingo
 * 
 * This is a IStorage subclass that handle file revision
 * 
 */
public class IStorageMercurialRevision implements IStorage {
	private String revision;
	private String global;
	private IResource resource;
	private ChangeSet changeSet;

	
	/**
	 * This method should be deprecated because a revision index is not unique.
	 * Therefore it tries to get the newest ChangeSet with the given revision
	 * index.
	 * 
	 * The recommended constructor to use is IStorageMercurialRevision(IResource
	 * res, String rev, String global, ChangeSet cs)
	 * 
	 */
	public IStorageMercurialRevision(IResource res, String rev) {
		super();
		resource = res;
		revision = rev;
		try {
			SortedSet<ChangeSet> changeSets = MercurialStatusCache
					.getInstance().getLocalChangeSets(res);
			if (changeSets != null) {
				ChangeSet[] changeSetArray = changeSets
						.toArray(new ChangeSet[changeSets.size()]);
				for (int i = changeSetArray.length-1; i >= 0; i--) {
					ChangeSet cs = changeSetArray[i];
					if (String.valueOf(cs.getRevision().getRevision()).equals(rev)) {
						this.changeSet = cs;
						break;
					}
				}
			}

		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
		} catch (NumberFormatException e) {
			MercurialEclipsePlugin.logError(e);
		}
	}

	public IStorageMercurialRevision(IResource res, String rev, String global,
			ChangeSet cs) {
		super();
		this.revision = rev;
		this.global = global;
		this.resource = res;
		this.changeSet = cs;
	}

	
	/**
	 * This constructor is not recommended, as the revision index is not unique.
	 * 
	 * @param res
	 * @param rev
	 */
	public IStorageMercurialRevision(IResource res, int rev) {
		this(res, String.valueOf(rev));
	}

	/**
	 * Constructs an {@link IStorageMercurialRevision} with the newest local
	 * changeset available.
	 * 
	 * @param res
	 *            the resource
	 */
	public IStorageMercurialRevision(IResource res) {
		super();

		ChangeSet cs = null;
		try {
			cs = MercurialStatusCache.getInstance()
					.getNewestLocalChangeSet(res);

			this.resource = res;
			this.revision = cs.getChangesetIndex() + ""; // should be fetched
			// from id
			this.global = cs.getChangeset();
			this.changeSet = cs;
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
		}
		//
		// // File workingDir = MercurialUtilities.getWorkingDir(res);
		// // IdentifyAction identifyAction = new IdentifyAction(null, res
		// // .getProject(), workingDir);
		// String ident = "unknown 0";
		// try {
		// ident = HgIdentClient.getCurrentRevision((IContainer) res);
		// // identifyAction.run();
		// // FIXME What happens if more than one changeset is found by
		// // identify? Currently just saving them and using the first.
		// String[] results = HgIdentClient.getChangeSets(ident);
		// changeSets = new ChangeSet[results.length];
		// for (int i = 0; i < results.length; i++) {
		// String[] parts = results[i].split(":");
		// changeSets[i] = new ChangeSet(Integer.parseInt(parts[0]),
		// parts[1], null, null);
		// this.revision = parts[0];
		// this.global = parts[1];
		// }
		//
		// } catch (Exception e) {
		// MercurialEclipsePlugin.logError("pull operation failed", e);
		// // System.out.println("pull operation failed");
		// // System.out.println(e.getMessage());
		//
		// IWorkbench workbench = PlatformUI.getWorkbench();
		// if (workbench.getActiveWorkbenchWindow() != null) {
		// Shell shell = workbench.getActiveWorkbenchWindow().getShell();
		// MessageDialog.openInformation(shell,
		// "Mercurial Eclipse couldn't identify hg revision of \n"
		// + res.getName().toString() + "\nusing tip",
		// ident);
		// revision = "tip";
		// }
		// }
	}

	@Deprecated
	public IStorageMercurialRevision(IResource res, int rev, int depth) {
		super();
		// project = proj;
		resource = res;
		revision = String.valueOf(rev);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		// System.out.println("IStorageMercurialRevision(" + resource.toString()
		// + "," + revision + ")::getAdapter()" );
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.resources.IStorage#getContents()
	 * 
	 * generate data content of the so called "file" in this case a revision,
	 * e.g. a hg cat --rev "rev" <file>
	 * 
	 */
	public InputStream getContents() throws CoreException {

		// Setup and run command
		String result = null;
		IFile file = resource.getProject().getFile(
				resource.getProjectRelativePath());
		if (changeSet != null) {

			String bundleFile = null;
			if (changeSet != null && changeSet.getBundleFile() != null) {
				try {
					bundleFile = changeSet.getBundleFile().getCanonicalFile()
							.getCanonicalPath();
				} catch (IOException e) {
					MercurialEclipsePlugin.logError(e);
					throw new CoreException(new Status(IStatus.ERROR,
							MercurialEclipsePlugin.ID, e.getMessage(), e));
				}
			}

			if (bundleFile != null) {
				result = HgCatClient.getContentFromBundle(file, changeSet
						.getChangesetIndex()
						+ "", bundleFile);
			} else {
				result = HgCatClient.getContent(file, changeSet
						.getChangesetIndex()
						+ "");
			}
		} else {
			result = HgCatClient.getContent(file, null);
		}
		ByteArrayInputStream is = new ByteArrayInputStream(result.getBytes());
		return is;
	}

	/*
	 * (non-Javadoc)setContents(
	 * 
	 * @see org.eclipse.core.resources.IStorage#getFullPath()
	 */
	public IPath getFullPath() {
		// System.out.println("IStorageMercurialRevision(" + resource.toString()
		// + "," + revision + ")::getFullPath()" );
		return resource.getFullPath().append(
				revision != null ? (" [" + revision + "]")
						: " [parent changeset]");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.resources.IStorage#getName()
	 */
	public String getName() {
		// System.out.print("IStorageMercurialRevision(" + resource.toString() +
		// "," + revision + ")::getName()" );
		String name;
		if (revision != null) {

			name = "[" + getRevision() + "] " + resource.getName();
		} else {
			name = resource.getName();
		}
		// System.out.println("=" + name );

		return name;
	}

	public String getRevision() {
		return revision;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.resources.IStorage#isReadOnly()
	 * 
	 * You can't write to other revisions then the current selected e.g.
	 * ReadOnly
	 * 
	 */
	public boolean isReadOnly() {
		// System.out.println("IStorageMercurialRevision(" + resource.toString()
		// + "," + revision + ")::isReadOnly()" );
		if (revision != null) {
			return true;
		}
		// if no revision resource is the current one e.g. editable :)
		ResourceAttributes attributes = resource.getResourceAttributes();
		if (attributes != null) {
			return attributes.isReadOnly();
		}
		return true; /* unknown state marked as read only for safety */
	}

	public IResource getResource() {
		return resource;
	}

	public String getGlobal() {
		return global;
	}

	public void setGlobal(String hash) {
		this.global = hash;
	}

	/**
	 * @return the changeSet
	 */
	public ChangeSet getChangeSet() {
		return changeSet;
	}

	/**
	 * @param changeSet
	 *            the changeSet to set
	 */
	public void setChangeSet(ChangeSet changeSet) {
		this.changeSet = changeSet;
	}
}
