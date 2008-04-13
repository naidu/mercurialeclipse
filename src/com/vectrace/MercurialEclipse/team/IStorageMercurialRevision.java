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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
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

	@Deprecated
	public IStorageMercurialRevision(IResource res, String rev) {
		super();
		resource = res;
		revision = rev;
		try {
			changeSet = MercurialStatusCache.getInstance().getLocalChangeSets(res)
					.get(new Integer(rev));
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
		} catch (NumberFormatException e) {
			MercurialEclipsePlugin.logError(e);
		}
	}

	public IStorageMercurialRevision(IResource res, String rev, String global, ChangeSet cs) {
		super();
		this.revision = rev;
		this.global = global;
		this.resource = res;
		this.changeSet = cs;
	}

	public IStorageMercurialRevision(IResource res, int rev) {
		this(res, String.valueOf(rev));
	}

	public IStorageMercurialRevision(IResource res) {
		super();

		ChangeSet cs = null;
		try {
			cs = MercurialStatusCache.getInstance().getNewestLocalChangeSet(res);

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
		String[] cmd;		
		if (changeSet != null) {					
			
			List<String>launchCmd =new ArrayList<String>();
			launchCmd.add(MercurialUtilities.getHGExecutable());
			
			if (changeSet!=null && changeSet.getBundleFile() != null){
				launchCmd.add("-R");
				try {
					launchCmd.add(changeSet.getBundleFile().getCanonicalFile().getCanonicalPath());
				} catch (IOException e) {					
					MercurialEclipsePlugin.logError(e);
					throw new CoreException(new Status(IStatus.ERROR,MercurialEclipsePlugin.ID,e.getMessage(),e));
				}
			}
			
			launchCmd.add("cat");
			launchCmd.add("-r");
			launchCmd.add(changeSet.getChangesetIndex()+"");
			launchCmd.add(MercurialUtilities.getResourceName(resource));
			
			cmd = launchCmd.toArray(new String[launchCmd.size()]);			
			
		} else {
			cmd = new String[] { MercurialUtilities.getHGExecutable(),
					"cat", "--", MercurialUtilities.getResourceName(resource) };
		}
		File workingDir = MercurialUtilities.getWorkingDir(resource);

		/*
		 * TODO using MercurialUtilities.ExecuteCommandToInputStream looks buggy
		 * as hell and fail to diff files that are not really small (deadlock?)
		 * (see the javadoc of java.lang.Process for a possible explanation)
		 */
		ByteArrayOutputStream resultStream = MercurialUtilities
				.ExecuteCommandToByteArrayOutputStream(cmd, workingDir,
						true);
		return new ByteArrayInputStream(resultStream.toByteArray());
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
