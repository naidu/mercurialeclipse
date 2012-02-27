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

import java.util.ArrayList;
import java.util.SortedSet;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * @author john
 *
 */
public class HgWorkspaceFolder extends HgWorkspaceResource implements IHgFolder {

	private final ArrayList<IHgResource> members = new ArrayList<IHgResource>();

	public HgWorkspaceFolder(HgRoot root, IContainer container, SortedSet<String> filter) {
		super(root, container);
		try {
			IResource wMembers[] = container.members();
			for (IResource res : wMembers) {
				if (res instanceof IContainer) {
					HgWorkspaceFolder hgFolder = new HgWorkspaceFolder(root, (IContainer) res,
							filter);
					if (hgFolder.members().length != 0) {
						members.add(hgFolder);
					}
				} else if (res instanceof IFile) {
					IPath relPath = ResourceUtils.getPath(res).makeRelativeTo(root.getIPath());
					if (filter == null || filter.contains(relPath.toOSString())) {
						members.add(new HgWorkspaceFile(root, (IFile) res));
					}
				}
			}
		} catch (CoreException e) {
			MercurialEclipsePlugin.logError(e);
		}
	}

	public HgWorkspaceFolder(HgRoot root, IContainer container) {
		super(root, container);
		try {
			IResource wMembers[] = container.members();
			for (IResource res : wMembers) {
				if (res instanceof IContainer) {
					members.add(new HgWorkspaceFolder(root, (IContainer) res));
				} else if (res instanceof IFile) {
					members.add(new HgWorkspaceFile(root, (IFile) res));
				}
			}
		} catch (CoreException e) {
			MercurialEclipsePlugin.logError(e);
		}
	}

	// operations

	/**
	 * @see com.vectrace.MercurialEclipse.model.IHgFolder#members()
	 */
	public IHgResource[] members() {
		return members.toArray(new IHgResource[members.size()]);
	}
}
