/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - initial
 *     Bastian Doetsch           - changes
 *     Brian Wallis              - getMergeStatus
 *     Andrei Loskutov           - bug fixes
 *     Zsolt Koppany (Intland)	 - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.aragost.javahg.commands.StatusCommand;
import com.aragost.javahg.commands.StatusLine;
import com.aragost.javahg.commands.flags.StatusCommandFlags;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * Get file status
 */
public class HgStatusClient extends AbstractClient {

	public static List<StatusLine> getStatusWithoutIgnored(HgRoot root, IResource res) {
		StatusCommand command = StatusCommandFlags.on(root.getRepository()).modified().added()
				.removed().deleted().unknown().clean();

		if (res.getType() == IResource.FILE) {
			return command.lines(ResourceUtils.getFileHandle(res));
		}

		return command.lines();
	}

	public static List<StatusLine> getStatusWithoutIgnored(HgRoot root) {
		return StatusCommandFlags.on(root.getRepository()).modified().added().removed()
				.deleted().unknown().clean().lines();
	}

	public static List<StatusLine> getStatusWithoutIgnored(HgRoot root, List<IResource> files) {
		return StatusCommandFlags.on(root.getRepository()).modified().added().removed()
				.deleted().unknown().clean().lines(toFileArray(files));
	}

	public static List<StatusLine> getStatusMARDU(HgRoot root, String revision, String inPattern) {
		StatusCommand command = StatusCommandFlags.on(root.getRepository()).modified().added()
				.removed().deleted().unknown();

		if (revision != null && revision != "") {
			command.rev(revision);
		}

		if (inPattern != null && inPattern != "") {
			command.include(inPattern);
		}

		return command.lines();
	}

	/**
	 * @param hgRoot non null
	 * @return non null, but probably empty array with root relative file paths with all
	 * files under the given root, which are untracked by hg
	 */
	public static SortedSet<String> getUntrackedFiles(HgRoot hgRoot) {
		return getFiles(StatusCommandFlags.on(hgRoot.getRepository()).unknown().lines());
	}

	public static void assertClean(HgRoot root) throws HgException {
		if (isDirty(root)) {
			throw new HgException("Unexpected condition: " + root.getName() + " is dirty");
		}
	}

	public static boolean isDirty(HgRoot root) {
		return !getDirtyFiles(root).isEmpty();
	}

	/**
	 * @return root relative paths of changed files, never null
	 */
	private static Set<String> getDirtyFiles(HgRoot root) {
		return getFiles(StatusCommandFlags.on(root.getRepository()).modified().added().removed()
				.deleted().lines());
	}

	public static SortedSet<String> getFiles(List<StatusLine> lines) {
		SortedSet<String> dirtySet = new TreeSet<String>();
		for (StatusLine line : lines) {
			dirtySet.add(line.getFileName());
		}
		return dirtySet;
	}

	/**
	 * Get absolute paths for dirty files
	 *
	 * @param root The root to query
	 * @return A set of absolute paths to dirty resource
	 */
	public static Set<IPath> getDirtyFilePaths(HgRoot root) {
		Set<String> dirtyFiles = getDirtyFiles(root);
		IPath rootPath = new Path(root.getAbsolutePath());
		Set<IPath> resources = new HashSet<IPath>();
		for (String rootRelativePath : dirtyFiles) {
			// determine absolute path
			IPath path = rootPath.append(rootRelativePath);
			resources.add(path);
		}
		return resources;
	}
}
