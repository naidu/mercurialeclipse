/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Bastian Doetsch 			- implementation of remove
 *     	Andrei Loskutov         	- bug fixes
 *     	Zsolt Koppany (Intland)		- bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.aragost.javahg.commands.flags.TagsCommandFlags;
import com.vectrace.MercurialEclipse.compare.TagComparator;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.Tag;

public class HgTagClient extends AbstractClient {

	/**
	 * Fetches all tags for given root.
	 * @param hgRoot non null
	 * @return never null, might be empty array
	 */
	private static com.aragost.javahg.commands.Tag[] getJavaHgTags(HgRoot hgRoot) {
		List<com.aragost.javahg.commands.Tag> tags = TagsCommandFlags.on(hgRoot.getRepository()).execute();

		return tags.toArray(new com.aragost.javahg.commands.Tag[tags.size()]);
	}

	/**
	 * Fetches all tags for given root.
	 * @param hgRoot non null
	 * @return never null, might be empty array
	 */
	public static com.vectrace.MercurialEclipse.model.Tag[] getTags(HgRoot hgRoot) {
		com.aragost.javahg.commands.Tag[] tags = getJavaHgTags(hgRoot);

		com.vectrace.MercurialEclipse.model.Tag[] itags = new com.vectrace.MercurialEclipse.model.Tag[tags.length];

		for (int i = 0; i < tags.length; i++) {
			itags[i] = new com.vectrace.MercurialEclipse.model.Tag(tags[i]);
		}

		return itags;
	}

	/**
	 * Fetches all tags for given root.
	 * @param hgRoot non null
	 * @return never null, might be empty array
	 */
	public static List<com.vectrace.MercurialEclipse.model.Tag> getTags(HgRoot hgRoot, String[] tagNames) {
		com.vectrace.MercurialEclipse.model.Tag[] tags = getTags(hgRoot);
		List<com.vectrace.MercurialEclipse.model.Tag> l = new ArrayList<com.vectrace.MercurialEclipse.model.Tag>(
				tagNames.length);

		for (int i = 0; i < tagNames.length; i++) {
			for (int j = 0; j < tags.length; j++) {
				if (tags[j].getName().equals(tagNames[i])) {
					l.add(tags[j]);
				}
			}
		}

		Collections.sort(l, new TagComparator());
		return l;
	}

	/**
	 * @param user
	 *            if null, uses the default user
	 * @throws HgException
	 */
	public static void addTag(HgRoot hgRoot, String name, String rev, String user, boolean local, boolean force) throws HgException {
		HgCommand command = new HgCommand(
				"tag", "Tagging revision " + ((rev == null) ? "" : rev + " ") + "as " + name, hgRoot, false); //$NON-NLS-1$
		if (local) {
			command.addOptions("-l");
		}
		if (force) {
			command.addOptions("-f");
		}
		if (rev != null) {
			command.addOptions("-r", rev);
		}
		command.addUserName(user);
		command.addOptions(name);
		command.executeToBytes();
		command.rememberUserName();
	}

	public static String removeTag(HgRoot hgRoot, Tag tag, String user) throws HgException {
		HgCommand command = new HgCommand("tag", "Removing tag " + tag, hgRoot, false); //$NON-NLS-1$
		command.addUserName(user);
		command.addOptions("--remove");
		command.addOptions(tag.getName());
		String result = command.executeToString();
		command.rememberUserName();
		return result;
	}
}
