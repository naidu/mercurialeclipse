/*******************************************************************************
 * Copyright (c) 2010 Andrei Loskutov (Intland).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Andrei Loskutov (Intland) - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.model;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.IWorkbenchAdapter;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * A virtual (but always canonical) path object which can be used to show plain files and
 * directories together with those known by Eclipse workspace.
 *
 * @author Andrei
 */
public class HgPath extends File implements IWorkbenchAdapter, IAdaptable {
	private final Path path;

	public HgPath(String pathname) throws IOException {
		this(new File(pathname));
	}

	public HgPath(File file) throws IOException {
		super(file.getCanonicalPath());
		path = new Path(getAbsolutePath());
	}

	public Object[] getChildren(Object o) {
		if (isFile()) {
			return new Object[0];
		}
		File[] files = listFiles();
		if (files == null) {
			return new Object[0];
		}
		List<Object> children = new ArrayList<Object>();
		for (File file : files) {
			IResource workspaceHandle;
			try {
				workspaceHandle = ResourceUtils.convert(file);
				if (workspaceHandle != null && workspaceHandle.exists()) {
					children.add(workspaceHandle);
				} else {
					if (isHgRoot(file)) {
						children.add(new HgRoot(file));
					} else {
						if (!file.getName().equals(".hg")) {
							children.add(new HgPath(file));
						}
					}
				}
			} catch (Exception e) {
				MercurialEclipsePlugin.logError(e);
				continue;
			}
		}
		return children.toArray();
	}

	public ImageDescriptor getImageDescriptor(Object object) {
		if (isFile()) {
			return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
					ISharedImages.IMG_OBJ_FILE);
		}
		return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
				ISharedImages.IMG_OBJ_FOLDER);
	}

	public String getLabel(Object o) {
		return getAbsolutePath();
	}

	public Object getParent(Object o) {
		try {
			return new HgPath(getParentFile());
		} catch (IOException e) {
			MercurialEclipsePlugin.logError(e);
			return null;
		}
	}

	@SuppressWarnings("rawtypes")
	public Object getAdapter(Class adapter) {
		if (adapter == IWorkbenchAdapter.class) {
			return this;
		}
		if (adapter == IResource.class || adapter == IFile.class) {
			try {
				IResource resource = ResourceUtils.convert(this);
				if (adapter == IFile.class && !(resource instanceof IFile)) {
					// do NOT return folder objects for iFile!
					return null;
				}
				return resource;
			} catch (HgException e) {
				MercurialEclipsePlugin.logError(e);
			}
		}
		return null;
	}

	/**
	 * @return the {@link IPath} object corresponding to this root, never null
	 */
	public IPath getIPath() {
		return path;
	}

	public IPath toAbsolute(IPath relative) {
		return path.append(relative);
	}

	/**
	 * Converts given path to the relative
	 *
	 * @param child
	 *            a possible child path
	 * @return a hg root relative path of a given file, if the given file is located under this
	 *         root, otherwise the path of a given file
	 */
	public String toRelative(File child) {
		// first try with the unresolved path. In most cases it's enough
		String fullPath = child.getAbsolutePath();
		if (!fullPath.startsWith(getPath())) {
			try {
				// ok, now try to resolve all the links etc. this takes A LOT of time...
				fullPath = child.getCanonicalPath();
				if (!fullPath.startsWith(getPath())) {
					return child.getPath();
				}
			} catch (IOException e) {
				MercurialEclipsePlugin.logError(e);
				return child.getPath();
			}
		}
		// +1 is to remove the file separator / at the start of the relative path
		return fullPath.substring(getPath().length() + 1);
	}

	public static boolean isHgRoot(File path) {
		if (path == null || !path.isDirectory()) {
			return false;
		}
		FileFilter hg = new FileFilter() {
			public boolean accept(File path1) {
				return path1.getName().equalsIgnoreCase(".hg") && path1.isDirectory(); //$NON-NLS-1$
			}
		};
		File[] rootContent = path.listFiles(hg);
		return rootContent != null && rootContent.length == 1;
	}

	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

}
