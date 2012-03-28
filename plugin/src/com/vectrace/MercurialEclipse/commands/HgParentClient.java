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

import java.io.File;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;

import com.aragost.javahg.Changeset;
import com.aragost.javahg.Repository;
import com.aragost.javahg.commands.flags.LogCommandFlags;
import com.aragost.javahg.commands.flags.ParentsCommandFlags;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.cache.CommandServerCache;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class HgParentClient extends AbstractClient {

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

	private static String findCommonAncestor(Repository repo, String node1, String node2) {
		List<Changeset> list = LogCommandFlags.on(repo)
				.rev("ancestor(" + node1 + ", " + node2 + ")").execute();

		assert list.size() <= 1;

		return list.isEmpty() ? null : list.get(0).getNode();
	}

	/**
	 * @param hgRoot The root that these nodes are in
	 * @param node1 The first changeset id
	 * @param node2 The second changeset id
	 * @return The common ancestor node id, or null.
	 * @throws HgException
	 */
	public static String findCommonAncestor(HgRoot hgRoot, String node1, String node2) {
		return findCommonAncestor(hgRoot.getRepository(), node1, node2);
	}

	/**
	 * This methods finds the common ancestor of two changesets, supporting overlays for using
	 * incoming changesets.
	 *
	 * @param hgRoot
	 *            hg root
	 * @param cs1
	 *            first changeset
	 * @param cs2
	 *            second changeset
	 * @return The common ancestor node id, or null.
	 */
	public static String findCommonAncestor(HgRoot hgRoot, ChangeSet cs1, ChangeSet cs2) {
		File bundleFile = null;

		if (cs1.getBundleFile() != null || cs2.getBundleFile() != null) {
			if (cs1.getBundleFile() != null) {
				bundleFile = cs1.getBundleFile();
			} else {
				bundleFile = cs2.getBundleFile();
			}
		}

		return findCommonAncestor(CommandServerCache.getInstance().get(hgRoot, bundleFile),
				cs1.getNode(), cs2.getNode());
	}
}
