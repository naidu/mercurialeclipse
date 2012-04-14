/*******************************************************************************
 * Copyright (c) 2008 Bastian Doetsch and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		bastian	implementation
 * 		Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.util.List;

import com.aragost.javahg.Bookmark;
import com.aragost.javahg.commands.flags.BookmarksCommandFlags;
import com.vectrace.MercurialEclipse.model.HgRoot;

/**
 * @author bastian
 */
public class HgBookmarkClient extends AbstractClient {

	/**
	 * @return a List of bookmarks
	 */
	public static List<Bookmark> getBookmarks(HgRoot hgRoot) {
		return BookmarksCommandFlags.on(hgRoot.getRepository()).list();
	}

	public static void create(HgRoot hgRoot, String name, String targetChangeset) {
		BookmarksCommandFlags.on(hgRoot.getRepository()).rev(targetChangeset).create(name);
	}

	public static void rename(HgRoot hgRoot, String name, String newName) {
		BookmarksCommandFlags.on(hgRoot.getRepository()).rename(name, newName);
	}

	public static void delete(HgRoot hgRoot, String name) {
		BookmarksCommandFlags.on(hgRoot.getRepository()).delete(name);
	}

}
