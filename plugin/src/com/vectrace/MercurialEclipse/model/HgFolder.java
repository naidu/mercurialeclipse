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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import org.eclipse.core.runtime.IPath;

import com.vectrace.MercurialEclipse.exception.HgException;

/**
 * @author Ge Zhong
 *
 */
public class HgFolder extends HgRevisionResource implements IHgFolder {

	private final ArrayList<IHgResource> members = new ArrayList<IHgResource>();
	private static final String separator = System.getProperty("file.separator");

	// constructors

	/**
	 * Create a new HgFolder and recursively instantiate sub-files and folders
	 *
	 * @param root The root to use
	 * @param changeset The changeset id
	 * @param path relative path from root
	 * @param listing List of files in the folder
	 * @param filter white list of files that can be included
	 */
	public HgFolder(HgRoot root, String changeset, IPath path, String[] listing,
			SortedSet<String> filter) throws HgException {
		super(root, changeset, path);
		parseListing(listing, filter);
	}

	public HgFolder(HgRoot root, JHgChangeSet changeset, IPath path, String[] listing,
			SortedSet<String> filter) {
		super(root, changeset, path);
		parseListing(listing, filter);
	}

	// operations

	/**
	 * Parse the list of files and then apply the filer
	 */
	private void parseListing(String[] listing, SortedSet<String> filter) {
		Map<String, ArrayList<String>> sublisting = new HashMap<String, ArrayList<String>>();

		String dir = path.addTrailingSeparator().toOSString();
		for (String line : listing) {
			String result = dir.length() > 1 ? line.substring(dir.length()) : line;
			int index = result.indexOf(separator);
			if (index == -1) {
				IPath filePath = path.append(result);
				if (filter == null || filter.contains(filePath.toOSString())) {
					IHgResource file = new HgFile(root, changeset, filePath);
					this.members.add(file);
				}
			} else {
				String folderName = result.substring(0, index);
				ArrayList<String> subfolder = sublisting.get(folderName);
				if (subfolder == null) {
					subfolder = new ArrayList<String>();
					subfolder.add(line);
					sublisting.put(folderName, subfolder);
				} else {
					subfolder.add(line);
				}
			}
		}

		if (sublisting.size() != 0) {
			Set<String> folderNames = sublisting.keySet();
			Iterator<String> it = folderNames.iterator();
			while (it.hasNext()) {
				String folderName = it.next();
				ArrayList<String> folder = sublisting.get(folderName);
				HgFolder hgFolder = new HgFolder(root, changeset, path.append(folderName),
						folder.toArray(new String[folder.size()]), filter);
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
