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
package com.vectrace.MercurialEclipse.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import org.eclipse.core.runtime.IPath;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.vectrace.MercurialEclipse.exception.HgException;

/**
 * @author Ge Zhong
 *
 */
public class HgFolder extends HgRevisionResource implements IHgFolder {

	private final List<IHgResource> members = new ArrayList<IHgResource>();

	// constructors

	/**
	 * Create a new HgFolder and recursively instantiate sub-files and folders
	 *
	 * @param root The root to use
	 * @param changeset The changeset id
	 * @param path relative path from root
	 * @param listing List of root relative paths in the folder
	 * @param filter List of files that can be included, or null for all
	 */
	public HgFolder(HgRoot root, String changeset, IPath path, List<IPath> listing,
			SortedSet<String> filter) throws HgException {
		super(root, changeset, path);
		parseListing(listing, filter);
	}

	public HgFolder(HgRoot root, JHgChangeSet changeset, IPath path, List<IPath> listing,
			SortedSet<String> filter) {
		super(root, changeset, path);
		parseListing(listing, filter);
	}

	// operations

	/**
	 * Parse the list of files and then apply the filer
	 */
	private void parseListing(List<IPath> listing, SortedSet<String> filter) {
		Multimap<String, IPath> sublisting = HashMultimap.create();

		for (IPath line : listing) {
			assert line.isPrefixOf(path);

			String segment = line.segment(path.segmentCount());

			if (line.segmentCount() == path.segmentCount() + 1) {
				if (filter == null || filter.contains(line.toOSString())) {
					IHgResource file = new HgFile(root, changeset, line);
					this.members.add(file);
				}
			} else {
				sublisting.put(segment, line);
			}
		}

		if (sublisting.size() != 0) {
			Set<String> folderNames = sublisting.keySet();
			for (Iterator<String> it = folderNames.iterator(); it.hasNext();) {
				String folderName = it.next();
				Collection<IPath> folder = sublisting.get(folderName);

				HgFolder hgFolder = new HgFolder(root, changeset, path.append(folderName),
						new ArrayList<IPath>(folder), filter);

				if (hgFolder.members().length != 0) {
					this.members.add(hgFolder);
				}
			}
		}

	}

	/**
	 * @see com.vectrace.MercurialEclipse.model.IHgFolder#members()
	 */
	public IHgResource[] members() {
		return members.toArray(new IHgResource[members.size()]);
	}
}
