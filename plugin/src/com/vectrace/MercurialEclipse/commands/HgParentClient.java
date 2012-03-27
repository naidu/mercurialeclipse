/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre - implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;

import com.aragost.javahg.Changeset;
import com.aragost.javahg.commands.flags.ParentsCommandFlags;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class HgParentClient extends AbstractClient {

	/**
	 * @deprecated
	 */
	@Deprecated
	private static final Pattern ANCESTOR_PATTERN = Pattern
			.compile("^(-?[0-9]+):([0-9a-f]+)$"); //$NON-NLS-1$

	public static int[] getParentIndexes(HgRoot hgRoot) {
		Changeset[] parents = getParents(hgRoot);
		int[] idxs = new int[parents.length];

		for (int i = 0; i < idxs.length; i++) {
			idxs[i] = parents[i].getRevision();
		}

		return idxs;
	}

	public static Changeset[] getParents(HgRoot hgRoot) {
		List<Changeset> cs = ParentsCommandFlags.on(hgRoot.getRepository()).execute();

		return cs.toArray(new Changeset[cs.size()]);
	}

	public static Changeset[] getParents(HgRoot hgRoot, IResource file) {
		List<Changeset> cs = ParentsCommandFlags.on(hgRoot.getRepository()).execute(
				ResourceUtils.getPath(file).toOSString());

		return cs.toArray(new Changeset[cs.size()]);
	}

	public static Changeset[] getParents(HgRoot hgRoot, Changeset cs, IPath path) {
		List<Changeset> parents = ParentsCommandFlags.on(hgRoot.getRepository()).rev(cs.getNode())
				.execute(path.toOSString());

		return parents.toArray(new Changeset[parents.size()]);
	}

	/**
	 * TODO: use JavaHg
	 *
	 * @param hgRoot The root that these nodes are in
	 * @param node1 The first changeset id
	 * @param node2 The second changeset id
	 * @return The common ancestor node id, or null.
	 * @throws HgException
	 */
	public static String findCommonAncestor(HgRoot hgRoot, String node1, String node2)
			throws HgException {
		AbstractShellCommand command = new HgCommand("debugancestor", //$NON-NLS-1$
				"Finding common ancestor", hgRoot, false);
		command.addOptions(node1, node2);
		String result = command.executeToString().trim();
		Matcher m = ANCESTOR_PATTERN.matcher(result);
		if (m.matches()) {
			return m.group(2);
		}
		throw new HgException("Parse exception: '" + result + "'");
	}

	/**
	 * TODO: use JavaHg
	 *
	 * This methods finds the common ancestor of two changesets, supporting
	 * overlays for using incoming changesets. Only one changeset may be
	 * incoming.
	 *
	 * @param hgRoot
	 *            hg root
	 * @param cs1
	 *            first changeset
	 * @param cs2
	 *            second changeset
	 * @return The common ancestor node id, or null.
	 * @throws HgException
	 */
	public static String findCommonAncestor(HgRoot hgRoot, ChangeSet cs1, ChangeSet cs2)
			throws HgException {
		String result;
		try {
			HgCommand command = new HgCommand("debugancestor", "Finding common ancestor", hgRoot,
					false);

			if (cs1.getBundleFile() != null || cs2.getBundleFile() != null) {
				if (cs1.getBundleFile() != null) {
					command.setBundleOverlay(cs1.getBundleFile());
				} else {
					command.setBundleOverlay(cs2.getBundleFile());
				}
			}

			command.addOptions(cs1.getNode(), cs2.getNode());

			result = command.executeToString().trim();
			Matcher m = ANCESTOR_PATTERN.matcher(result);
			if (m.matches()) {
				return m.group(2);
			}
			throw new HgException("Parse exception: '" + result + "'");
		} catch (NumberFormatException e) {
			throw new HgException(e.getLocalizedMessage(), e);
		} catch (IOException e) {
			throw new HgException(e.getLocalizedMessage(), e);
		}
	}
}
