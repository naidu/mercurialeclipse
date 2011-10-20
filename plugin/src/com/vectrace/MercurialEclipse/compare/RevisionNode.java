/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.compare;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.CompareUI;
import org.eclipse.compare.IEditableContent;
import org.eclipse.compare.IEditableContentExtension;
import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.compare.IModificationDate;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.HgResource;
import com.vectrace.MercurialEclipse.model.IHgFile;
import com.vectrace.MercurialEclipse.model.IHgFolder;
import com.vectrace.MercurialEclipse.model.IHgResource;

public class RevisionNode implements IEncodedStreamContentAccessor, IStructureComparator, ITypedElement,
IEditableContent, IModificationDate, IEditableContentExtension {

	private final IHgResource resource;
	private List<IStructureComparator> children;

	public RevisionNode(IHgResource resource) {
		this.resource = resource;
	}

	public IHgResource getHgResource() {
		return resource;
	}

	/**
	 * @see org.eclipse.compare.ITypedElement#getName()
	 */
	public String getName() {
		if (resource != null) {
			return resource.getName();
		}
		return null;
	}

	public String getLabel()
	{
		String name = resource.getName();
		if (resource.getChangeSet() != null) {
			name = name + " [" + resource.getChangeSet().toString() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return name;
	}

	public InputStream getContents() throws CoreException {
		if (resource instanceof IStorage) {
			return ((IStorage) resource).getContents();
		}
		return null;
	}

	public byte[] getContent() {
		if (resource instanceof HgResource) {
			return ((HgResource) resource).getContent();
		}
		return null;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof RevisionNode) {
			return this.resource.equals(((RevisionNode) other).getHgResource());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return resource.hashCode();
	}

	public boolean isWorkingCopy() {
		return resource.getResource() != null;
	}

	/**
	 * @see org.eclipse.compare.IEditableContentExtension#isReadOnly()
	 */
	public boolean isReadOnly() {
		return resource.isReadOnly();
	}

	/**
	 * @see org.eclipse.compare.IEditableContentExtension#validateEdit(org.eclipse.swt.widgets.Shell)
	 */
	public IStatus validateEdit(Shell shell) {
		IResource res = resource.getResource();

		if (res instanceof IFile) {
			// See org.eclipse.compare.ResourceNode.validateEdit(Shell)
			if (isReadOnly()) {
				return ResourcesPlugin.getWorkspace().validateEdit(new IFile[] { (IFile)res}, shell);
			}
			return Status.OK_STATUS;
		}

		// Not in workspace
		return Status.CANCEL_STATUS;
	}

	/**
	 * @see org.eclipse.compare.IEditableContent#isEditable()
	 */
	public boolean isEditable() {
		return !isReadOnly();
	}

	/**
	 * @see org.eclipse.compare.IEditableContent#replace(org.eclipse.compare.ITypedElement, org.eclipse.compare.ITypedElement)
	 */
	public ITypedElement replace(ITypedElement dest, ITypedElement src) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see org.eclipse.compare.ITypedElement#getImage()
	 */
	public Image getImage() {
		return CompareUI.getImage(getType());
	}

	/**
	 * @see org.eclipse.compare.ITypedElement#getType()
	 */
	public String getType() {
		if (resource instanceof IHgFolder) {
			return ITypedElement.FOLDER_TYPE;
		}
		if (resource instanceof IHgFile) {
			String s = ((IHgFile)resource).getFileExtension();
			if (s != null) {
				return s;
			}
		}
		return ITypedElement.UNKNOWN_TYPE;
	}

	/**
	 * @see org.eclipse.compare.structuremergeviewer.IStructureComparator#getChildren()
	 */
	public Object[] getChildren() {
		if (children == null) {
			children = new ArrayList<IStructureComparator>();
			if (resource instanceof IHgFolder) {
				IHgResource[] members = ((IHgFolder) resource).members();
				for (int i = 0; i < members.length; i++) {
					IStructureComparator child = new RevisionNode(members[i]);
					children.add(child);
				}
			}
		}
		return children.toArray();
	}

	/**
	 * @see org.eclipse.compare.IEncodedStreamContentAccessor#getCharset()
	 */
	public String getCharset() throws CoreException {
		return resource.getHgRoot().getEncoding();
	}

	/**
	 * @see org.eclipse.compare.IModificationDate#getModificationDate()
	 */
	public long getModificationDate() {
		IResource res = resource.getResource();

		if (res != null) {
			return res.getLocalTimeStamp();
		}

		// Future: get timestamp from commit time?
		return 0;
	}

	/**
	 * @see org.eclipse.compare.IEditableContent#setContent(byte[])
	 */
	public void setContent(byte[] newContent) {
		if (resource.getResource() instanceof IFile) {
			InputStream is = new ByteArrayInputStream(newContent);
			try {
				// update cache
				((HgResource) resource).setContent(newContent);
				// update local file
				((IFile) resource.getResource()).setContents(is, true, true, null);
			} catch (CoreException e) {
				MercurialEclipsePlugin.logError(e);
			}
		}
	}
}