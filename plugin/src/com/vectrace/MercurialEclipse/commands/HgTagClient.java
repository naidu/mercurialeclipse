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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.aragost.javahg.commands.ExecutionException;
import com.aragost.javahg.commands.TagCommand;
import com.aragost.javahg.commands.flags.TagCommandFlags;
import com.aragost.javahg.commands.flags.TagsCommandFlags;
import com.vectrace.MercurialEclipse.compare.TagComparator;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.Tag;
import com.vectrace.MercurialEclipse.storage.HgCommitMessageManager;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

public class HgTagClient extends AbstractClient {

	/**
	 * Fetches all tags for given root.
	 * @param hgRoot non null
	 * @return never null, might be empty array
	 */
	private static com.aragost.javahg.commands.Tag[] getJavaHgTags(HgRoot hgRoot) {
		List<com.aragost.javahg.commands.Tag> tags = TagsCommandFlags.on(hgRoot.getRepository())
				.includeTip().execute();

		return tags.toArray(new com.aragost.javahg.commands.Tag[tags.size()]);
	}

	/**
	 * Fetches all tags for given root.
	 * @param hgRoot non null
	 * @return never null, might be empty array
	 */
	public static Tag[] getTags(HgRoot hgRoot) {
		com.aragost.javahg.commands.Tag[] tags = getJavaHgTags(hgRoot);

		Tag[] itags = new Tag[tags.length];

		for (int i = 0; i < tags.length; i++) {
			itags[i] = new Tag(tags[i]);
		}

		return itags;
	}

	public static Tag[] getTags(HgRoot hgRoot, Collection<String> tagNames) {
		return getTags(hgRoot, tagNames.toArray(new String[tagNames.size()]));
	}

	public static Tag[] getTags(HgRoot hgRoot, String[] tagNames) {
		Tag[] tags = getTags(hgRoot);
		List<Tag> l = new ArrayList<Tag>(tagNames.length);

		for (int i = 0; i < tagNames.length; i++) {
			for (int j = 0; j < tags.length; j++) {
				if (tags[j].getName().equals(tagNames[i])) {
					l.add(tags[j]);
				}
			}
		}

		Collections.sort(l, new TagComparator());
		return l.toArray(new Tag[l.size()]);
	}

	/**
	 * @param user
	 *            if null, uses the default user
	 * @throws HgException
	 */
	public static void addTag(HgRoot hgRoot, String name, String rev, String user, boolean local,
			boolean force) throws HgException {
		TagCommand command = TagCommandFlags.on(hgRoot.getRepository()).user(user);

		if (local) {
			command = command.local();
		}
		if (force) {
			command = command.force();
		}
		if (rev != null) {
			command = command.rev(rev);
		}

		user = MercurialUtilities.getDefaultUserName(user);

		if (user != null) {
			command.user(user);
		}

		try {
			command.execute(name);
		} catch (ExecutionException ee) {
			throw new HgException(command.getErrorString(), ee);
		}

		HgCommitMessageManager.updateDefaultCommitName(hgRoot, user);
	}

	public static void removeTag(HgRoot hgRoot, Tag tag, String user) throws HgException {
		TagCommand command = TagCommandFlags.on(hgRoot.getRepository()).user(user);

		user = MercurialUtilities.getDefaultUserName(user);

		if (user != null) {
			command.user(user);
		}

		command = command.remove();

		try {
			command.execute(tag.getName());
		} catch (ExecutionException ee) {
			throw new HgException(command.getErrorString(), ee);
		}

		HgCommitMessageManager.updateDefaultCommitName(hgRoot, user);
	}
}
