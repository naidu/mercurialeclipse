/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Bastian Doetsch 			- implementation of remove
 *     	Andrei Loskutov (Intland) 	- bug fixes
 *     	Zsolt Koppany (Intland)		- bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vectrace.MercurialEclipse.compare.TagComparator;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.Tag;

public class HgTagClient extends AbstractClient {
	private static final Pattern GET_TAGS_PATTERN = Pattern.compile("^(.+[^ ]) +([0-9]+):([a-f0-9]+)( local)?$"); //$NON-NLS-1$

	public static Tag[] getTags(HgRoot hgRoot) throws HgException {
		AbstractShellCommand command = new HgCommand("tags", hgRoot, false); //$NON-NLS-1$
		command.addOptions("-v"); //$NON-NLS-1$
		String[] lines = command.executeToString().split("\n"); //$NON-NLS-1$

		Collection<Tag> tags = getTags(lines);
		Tag[] sortedTags = tags.toArray(new Tag[] {});
		return sortedTags;
	}

	protected static Collection<Tag> getTags(String[] lines) throws HgException {
		List<Tag> tags = new ArrayList<Tag>();

		for (String line : lines) {
			Matcher m = GET_TAGS_PATTERN.matcher(line);
			if (m.matches()) {
				Tag tag = new Tag(m.group(1), Integer.parseInt(m.group(2)), m.group(3), m.group(4) != null);
				tags.add(tag);
			} else {
				throw new HgException(Messages.getString("HgTagClient.parseException") + line + "'"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		Collections.sort(tags, new TagComparator());
		return tags;
	}

	/**
	 * @param user
	 *            if null, uses the default user
	 * @throws HgException
	 */
	public static void addTag(HgRoot hgRoot, String name, String rev, String user, boolean local, boolean force) throws HgException {
		HgCommand command = new HgCommand("tag", hgRoot, false); //$NON-NLS-1$
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
		HgCommand command = new HgCommand("tag", getWorkingDirectory(hgRoot), false); //$NON-NLS-1$
		command.addUserName(user);
		command.addOptions("--remove");
		command.addOptions(tag.getName());
		String result = command.executeToString();
		command.rememberUserName();
		return result;
	}
}
