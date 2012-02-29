/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others. All rights
 * reserved. This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Bastian Doetsch - implementation
 * Andrei Loskutov - bugfixes
 * Zsolt Koppany (Intland)
 * Ilya Ivanov (Intland)
 ******************************************************************************/

package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;

import com.aragost.javahg.Changeset;
import com.aragost.javahg.commands.LogCommand;
import com.aragost.javahg.commands.flags.HeadsCommandFlags;
import com.aragost.javahg.commands.flags.LogCommandFlags;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.model.JHgChangeSet;
import com.vectrace.MercurialEclipse.team.cache.CommandServerCache;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.MercurialRootCache;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class HgLogClient extends AbstractParseChangesetClient {

	public static final String NOLIMIT = "999999999999";

	public static Changeset[] getHeads(HgRoot hgRoot) {
		return toArray(HeadsCommandFlags.on(hgRoot.getRepository()).execute());
	}

	private static Changeset[] toArray(List<Changeset> list) {
		return list.toArray(new Changeset[list.size()]);
	}

	/**
	 * Helper method to transform JavaHg changesets into normal changesets.
	 */
	public static ChangeSet[] getChangeSets(HgRoot root, Changeset[] list) {
		ChangeSet[] ar = new ChangeSet[list.length];

		for (int i = 0, n = list.length; i < n; i++) {
			ar[i] = getChangeSet(root, list[i]);
		}

		return ar;
	}

	private static List<ChangeSet> getChangeSets(HgRoot root, List<Changeset> list) {
		List<ChangeSet> ar = new ArrayList<ChangeSet>(list.size());

		for (int i = 0, n = list.size(); i < n; i++) {
			ar.add(getChangeSet(root, list.get(i)));
		}

		return ar;
	}

	public static List<ChangeSet> getResourceLog(HgRoot root, IResource res, int limitNumber, int startRev) {
		boolean isFile = res.getType() == IResource.FILE;

		LogCommand command = addRange(LogCommandFlags.on(root.getRepository()), startRev, limitNumber, isFile);

		if (isFile) {
			command.follow().removed();
		}

		return getChangeSets(root, command.execute(ResourceUtils.getPath(res).toOSString()));
	}

	public static List<ChangeSet> getRootLog(HgRoot root, int limitNumber, int startRev) {
		return getChangeSets(root,
				addRange(LogCommandFlags.on(root.getRepository()), startRev, limitNumber, false)
						.execute());
	}

	/**
	 * TODO: reevaluate
	 */
	private static LogCommand addRange(LogCommand command, int startRev, int limitNumber, boolean isFile) {
		if (startRev >= 0 && startRev != Integer.MAX_VALUE) {
			// always advise to follow until 0 revision: the reason is that log limit
			// might be bigger then the difference of two consequent revisions on a specific resource
			command.rev(startRev + ":" + 0); //$NON-NLS-1$
		}
		if(isFile && startRev == Integer.MAX_VALUE) {
			// always start with the tip to get the latest version of a file too
			// seems that hg log misses some versions if the file working copy is not at the tip
			command.rev("tip:0"); //$NON-NLS-1$
		}
		command.limit((limitNumber > 0) ? limitNumber : Integer.MAX_VALUE);

		return command;
	}

	public static ChangeSet getChangeset(IResource res, String nodeId) {
		Assert.isNotNull(nodeId);
		HgRoot root = MercurialRootCache.getInstance().getHgRoot(res);

		return getChangeSet(root, nodeId);
	}

	/**
	 * Helper method to transform JavaHg changesets into normal changesets
	 */
	public static ChangeSet getChangeSet(HgRoot root, Changeset cs) {
		return LocalChangesetCache.getInstance().get(root, cs);
	}

	/**
	 * Tries to retrieve (possible not existing) changeset for the given id.
	 *
	 * @param hgRoot
	 *            non null
	 * @param nodeId
	 *            non null
	 * @return might return null if the changeset is not known/existing in the repo
	 */
	public static ChangeSet getChangeSet(HgRoot root, String nodeId) {
		return getChangeSet(root, nodeId, null, null, null);
	}

	public static ChangeSet getChangeSet(HgRoot root, String nodeId, IHgRepositoryLocation remote,
			Direction direction, File bundle) {
		LogCommand command = LogCommandFlags.on(CommandServerCache.getInstance().get(root, bundle)).rev(nodeId);

		Changeset cs = command.single();

		if (bundle != null) {
			// TODO: cache?
			return new JHgChangeSet(root, cs, remote, direction, bundle);
		}

		return getChangeSet(root, cs);
	}

	public static ChangeSet getChangeSet(HgRoot root, int rev) {
		return getChangeSet(root, rev + ":" + rev);
	}
}
