/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * ge.zhong	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.IOException;
import java.util.SortedSet;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.model.HgFile;
import com.vectrace.MercurialEclipse.model.HgFolder;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgFile;
import com.vectrace.MercurialEclipse.model.IHgResource;

/**
 * @author Ge Zhong
 *
 */
public class HgLocateClient extends AbstractClient {

	public static IHgResource getHgResources(IResource resource, ChangeSet cs, SortedSet<String> filter) throws HgException {

		HgRoot hgRoot = getHgRoot(resource);
		if (cs == null) {
			// local resource
			if (resource instanceof IFile) {
				return new HgFile(hgRoot, (IFile)resource);
			}
			if (resource instanceof IContainer) {
				return new HgFolder(hgRoot, (IContainer)resource, filter);
			}
			return null;
		}

		String revision = cs.getChangeset();
		HgCommand command = new HgCommand("locate", "Retrieving repository contents", hgRoot, true);

		if (cs.getDirection() == Direction.INCOMING && cs.getBundleFile() != null) {
			try {
				command.setBundleOverlay(cs.getBundleFile());
			} catch (IOException e) {
				throw new HgException("Unable to determine canonical path for " + cs.getBundleFile(), e);
			}
		}

		if (revision != null && revision.length() != 0) {
			command.addOptions("-r", revision); //$NON-NLS-1$
		}

		IPath relpath = resource.getLocation().makeRelativeTo(hgRoot.getIPath());

		command.addOptions(getHgResourceSearchPattern(resource));

		String[] lines = null;
		try {
			lines = command.executeToString().split("\n"); //$NON-NLS-1$
		} catch (HgException e) {
				throw new HgException(e);
		}

        if (lines.length == 0) {
        	return null;
        }

		if (resource instanceof IStorage) {
			for (String line : lines) {
					return new HgFile(hgRoot, cs, new Path(line));
			}
			return null;
		}

		return new HgFolder(hgRoot, revision, relpath, lines, filter);
	}

	public static IHgResource getHgResources(IHgResource hgResource, String revision, SortedSet<String> filter) throws HgException {
		HgRoot hgRoot = hgResource.getHgRoot();
		AbstractShellCommand command = new HgCommand("locate", "Retrieving repository contents", hgRoot, true);

		if (revision != null && revision.length() != 0) {
			command.addOptions("-r", revision); //$NON-NLS-1$
		}

		if (hgResource instanceof IHgFile) {
			command.addOptions('"' + "glob:" + hgResource.getHgRootRelativePath() + '"');
		} else {
			command.addOptions('"' + "glob:" + hgResource.getHgRootRelativePath() + System.getProperty("file.separator") + "**" + '"');
		}

		String[] lines = null;
		try {
			lines = command.executeToString().split("\n"); //$NON-NLS-1$
		} catch (HgException e) {
				throw new HgException(e);
		}

        if (lines.length == 0) {
        	return null;
        }

		if (hgResource instanceof IStorage) {
			for (String line : lines) {
				if (line.indexOf(System.getProperty("file.separator")) == -1) {
					return new HgFile(hgRoot, revision, new Path(line));
				}
			}
			return null;
		}

		return new HgFolder(hgRoot, revision, hgResource.getIPath(), lines, filter);


	}
}
