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

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.model.HgFile;
import com.vectrace.MercurialEclipse.model.HgFolder;
import com.vectrace.MercurialEclipse.model.HgRevisionResource;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgFile;
import com.vectrace.MercurialEclipse.model.IHgResource;
import com.vectrace.MercurialEclipse.model.JHgChangeSet;
import com.vectrace.MercurialEclipse.model.NullHgFile;

/**
 * @author Ge Zhong
 *
 */
public class HgLocateClient extends AbstractClient {

	public static HgFile getHgFile(HgRoot hgRoot, IPath relpath, JHgChangeSet cs) throws HgException {
		return (HgFile) getHgResources(hgRoot, relpath, true, cs, null);
	}

	/**
	 * Get the {@link HgRevisionResource} for the given resource at the given changeset
	 *
	 * @param resource The resource to use
	 * @param cs The changeset to use
	 * @param filter Optional filter
	 * @return The revision resource or a NullHgFile if it couldn't be located
	 * @throws HgException
	 */
	public static HgRevisionResource getHgResources(HgRoot hgRoot, IPath relpath, boolean file, JHgChangeSet cs, SortedSet<String> filter) throws HgException {

		String revision = cs.getNode();
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

		command.addOptions(getHgResourceSearchPattern(hgRoot, relpath, file));

		String[] lines = null;
		try {
			lines = command.executeToString().split("\n"); //$NON-NLS-1$
		} catch (HgException e) {
			// it is normal that the resource does not exist.
		}

		if (file) {
			if (lines == null || lines.length == 0) {
				return new NullHgFile(hgRoot, cs, relpath);
			}
			for (String line : lines) {
				return new HgFile(hgRoot, cs, new Path(line));
			}
		}

		return new HgFolder(hgRoot, cs, relpath, lines, filter);
	}

	public static IHgResource getHgResources(IHgResource hgResource, String revision, SortedSet<String> filter) throws HgException {
		HgRoot hgRoot = hgResource.getHgRoot();
		AbstractShellCommand command = new HgCommand("locate", "Retrieving repository contents", hgRoot, true);

		if (revision != null && revision.length() != 0) {
			command.addOptions("-r", revision); //$NON-NLS-1$
		}

		if (hgResource instanceof IHgFile) {
			command.addOptions("glob:" + hgResource.getIPath().toOSString());
		} else {
			command.addOptions("glob:" + hgResource.getIPath().toOSString() + System.getProperty("file.separator") + "**");
		}

		String[] lines = null;
		try {
			lines = command.executeToString().split("\n"); //$NON-NLS-1$
		} catch (HgException e) {
			// it is normal that the resource does not exist.
			MercurialEclipsePlugin.logWarning(e.getMessage(), e);
		}

		if (hgResource instanceof IStorage) {
			if (lines == null || lines.length == 0) {
				return new NullHgFile(hgRoot, revision, hgResource.getIPath());
			}
			for (String line : lines) {
				return new HgFile(hgRoot, revision, new Path(line));
			}
		}

		try {
			return new HgFolder(hgRoot, revision, hgResource.getIPath(), lines, filter);
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
			return null;
		}
	}
}
