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

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.aragost.javahg.Repository;
import com.aragost.javahg.commands.ExecutionException;
import com.aragost.javahg.commands.LocateCommand;
import com.aragost.javahg.commands.flags.LocateCommandFlags;
import com.google.common.collect.Lists;
import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgFile;
import com.vectrace.MercurialEclipse.model.HgFolder;
import com.vectrace.MercurialEclipse.model.HgRevisionResource;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgFile;
import com.vectrace.MercurialEclipse.model.IHgResource;
import com.vectrace.MercurialEclipse.model.JHgChangeSet;
import com.vectrace.MercurialEclipse.model.NullHgFile;
import com.vectrace.MercurialEclipse.team.cache.CommandServerCache;

/**
 * @author Ge Zhong
 */
public class HgLocateClient extends AbstractClient {

	public static HgFile getHgFile(HgRoot hgRoot, IPath relpath, JHgChangeSet cs) throws HgException {
		return (HgFile) getHgResources(hgRoot, relpath, true, cs, null);
	}

	/**
	 * Get the {@link HgRevisionResource} for the given resource at the given changeset
	 *
	 * @param resource
	 *            The resource to use
	 * @param cs
	 *            The changeset to use
	 * @param filter
	 *            Optional filter
	 * @return The revision resource or a NullHgFile if it couldn't be located
	 * @throws HgException
	 */
	public static HgRevisionResource getHgResources(HgRoot hgRoot, IPath relpath, boolean file,
			JHgChangeSet cs, SortedSet<String> filter) throws HgException {
		Repository repo = CommandServerCache.getInstance().get(hgRoot, cs.getBundleFile());
		LocateCommand command = LocateCommandFlags.on(repo).rev(cs.getNode());

		try {
			List<IPath> paths = toPaths(hgRoot,
					command.execute(getHgResourceSearchPattern(hgRoot, relpath, file)));

			if (file) {
				if (paths.isEmpty()) {
					return new NullHgFile(hgRoot, cs, relpath);
				}
				for (IPath line : paths) {
					return new HgFile(hgRoot, cs, line);
				}
			}

			return new HgFolder(hgRoot, cs, relpath, paths, filter);
		} catch (ExecutionException e) {
			return new NullHgFile(hgRoot, cs, relpath);
		}
	}

	/**
	 * @param hgResource
	 *            Resource to query
	 * @param revision
	 *            Revision at which to query. Not null
	 * @param filter
	 *            Optional filter
	 * @return New IHgResource for the given revision
	 * @throws HgException
	 */
	public static HgRevisionResource getHgResources(IHgResource hgResource, String revision,
			SortedSet<String> filter) throws HgException {
		Assert.isNotNull(revision);

		HgRoot hgRoot = hgResource.getHgRoot();
		LocateCommand command = LocateCommandFlags.on(hgRoot.getRepository()).rev(revision);
		List<IPath> paths;
		String pattern;

		if (hgResource instanceof IHgFile) {
			pattern = "glob:" + hgResource.getIPath().toOSString();
		} else {
			pattern = "glob:" + hgResource.getIPath().toOSString()
					+ System.getProperty("file.separator") + "**";
		}

		try {
			paths = toPaths(hgRoot, command.execute(pattern));
		} catch (ExecutionException e) {
			// it is normal that the resource does not exist.
			MercurialEclipsePlugin.logWarning(e.getMessage(), e);
			paths = Collections.emptyList();
		}

		if (hgResource instanceof IStorage) {
			if (paths.isEmpty()) {
				return new NullHgFile(hgRoot, revision, hgResource.getIPath());
			}
			for (IPath line : paths) {
				return new HgFile(hgRoot, revision, line);
			}
		}

		try {
			return new HgFolder(hgRoot, revision, hgResource.getIPath(), paths, filter);
		} catch (HgException e) {
			// ??
			MercurialEclipsePlugin.logError(e);
			return null;
		}
	}

	private static List<IPath> toPaths(HgRoot hgRoot, List<File> files) {
		List<IPath> paths= Lists.newArrayList();

		for (File f : files) {
			paths.add(hgRoot.toRelative(new Path(f.getAbsolutePath())));
		}

		return paths;
	}
}
