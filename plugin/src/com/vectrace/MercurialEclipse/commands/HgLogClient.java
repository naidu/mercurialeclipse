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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.aragost.javahg.Changeset;
import com.aragost.javahg.commands.LogCommand;
import com.aragost.javahg.commands.flags.HeadsCommandFlags;
import com.aragost.javahg.commands.flags.LogCommandFlags;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.model.JHgChangeSet;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.cache.MercurialRootCache;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class HgLogClient extends AbstractParseChangesetClient {

	private static final Map<IPath, Set<ChangeSet>> EMPTY_MAP =
		Collections.unmodifiableMap(new HashMap<IPath, Set<ChangeSet>>());

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

	/**
	 * Helper method to transform JavaHg changesets into normal changesets
	 */
	public static ChangeSet getChangeSet(HgRoot root, Changeset cs, IHgRepositoryLocation remote,
			Direction direction, File bundle) {
		return new JHgChangeSet(root, cs, remote, direction, bundle);
	}

	/**
	 * Helper method to transform JavaHg changesets into normal changesets
	 */
	public static ChangeSet getChangeSet(HgRoot root, Changeset cs) {
		return new JHgChangeSet(root, cs);
	}

	/**
	 * @return map where the key is an absolute file path, never null
	 */
	public static Map<IPath, Set<ChangeSet>> getCompleteProjectLog(
			IResource res, boolean withFiles) throws HgException {
		return getProjectLog(res, -1, Integer.MAX_VALUE, withFiles);
	}

	/**
	 * @return map where the key is an absolute file path, never null
	 */
	public static Map<IPath, Set<ChangeSet>> getCompleteRootLog(
			HgRoot hgRoot, boolean withFiles) throws HgException {
		return getRootLog(hgRoot, -1, Integer.MAX_VALUE, withFiles);
	}

	/**
	 * @return map where the key is an absolute file path, never null
	 */
	public static Map<IPath, Set<ChangeSet>> getProjectLogBatch(
			IResource res, int batchSize, int startRev, boolean withFiles)
			throws HgException {
		return getProjectLog(res, batchSize, startRev, withFiles);
	}

	/**
	 * @return map where the key is an absolute file path, never null
	 */
	public static Map<IPath, Set<ChangeSet>> getRecentProjectLog(
			IResource res, int limitNumber, boolean withFiles) throws HgException {
		return getProjectLogBatch(res, limitNumber, -1, withFiles);
	}

	public static List<ChangeSet> getResourceLog(HgRoot root, IResource res, int limitNumber,
			int startRev) {
		boolean isFile = res.getType() == IResource.FILE;

		LogCommand command = addRange(LogCommandFlags.on(root.getRepository()), startRev, limitNumber, isFile);

		if (isFile) {
			command.follow();
		}

		return getChangeSets(root, command.execute(ResourceUtils.getPath(res).toOSString()));
	}

	/**
	 * @return map where the key is an absolute file path, never null
	 */
	public static Map<IPath, Set<ChangeSet>> getProjectLog(IResource res,
			int limitNumber, int startRev, boolean withFiles)
			throws HgException {
		HgCommand command = new HgCommand("log", "Retrieving history", //$NON-NLS-1$
				res, false);

		command.setUsePreferenceTimeout(MercurialPreferenceConstants.LOG_TIMEOUT);
		command.addStyleFile(withFiles ? AbstractParseChangesetClient.STYLE_WITH_FILES
				: AbstractParseChangesetClient.STYLE_DEFAULT);

		boolean isFile = res.getType() == IResource.FILE;
		addRange(command, startRev, limitNumber, isFile);

		if (isFile) {
			command.addOptions("-f"); //$NON-NLS-1$
		}

		if (res.getType() != IResource.PROJECT) {
			IPath path = ResourceUtils.getPath(res);
			if(path.isEmpty()) {
				return EMPTY_MAP;
			}
			command.addOptions(path.toOSString());
		} else {
			HgRoot hgRoot = command.getHgRoot();
			File fileHandle = ResourceUtils.getFileHandle(res);
			if(!hgRoot.equals(fileHandle)){
				// for multiple projects under same hg root we should return only current project history
				command.addOptions(fileHandle.getAbsolutePath());
			}
		}

		String result = command.executeToString();
		if (result.length() == 0) {
			return EMPTY_MAP;
		}
		Map<IPath, Set<ChangeSet>> revisions = createLocalRevisions(
				res, result, Direction.LOCAL, null, null, null);
		return revisions;
	}

	public static List<ChangeSet> getRootLog(HgRoot root, int limitNumber, int startRev) {
		return getChangeSets(root,
				addRange(LogCommandFlags.on(root.getRepository()), startRev, limitNumber, false)
						.execute());
	}

	/**
	 * @return map where the key is an absolute file path, never null
	 * @deprecated
	 */
	@Deprecated
	public static Map<IPath, Set<ChangeSet>> getRootLog(HgRoot hgRoot,
			int limitNumber, int startRev, boolean withFiles)
			throws HgException {
		HgCommand command = new HgCommand("log", "Retrieving history", hgRoot, false);
		command.setUsePreferenceTimeout(MercurialPreferenceConstants.LOG_TIMEOUT);
		command.addStyleFile(withFiles ? AbstractParseChangesetClient.STYLE_WITH_FILES
				: AbstractParseChangesetClient.STYLE_DEFAULT);
		addRange(command, startRev, limitNumber, false);

		String result = command.executeToString();
		if (result.length() == 0) {
			return EMPTY_MAP;
		}
		Path path = new Path(hgRoot.getAbsolutePath());
		Map<IPath, Set<ChangeSet>> revisions = createLocalRevisions(path, result, Direction.LOCAL, null, null, null, hgRoot);
		return revisions;
	}

	/**
	 * @return never null
	 */
	public static Map<IPath, Set<ChangeSet>> getPathLog(boolean isFile, File path,
			HgRoot hgRoot, int limitNumber, int startRev, boolean withFiles)
			throws HgException {
		if(hgRoot == null) {
			return EMPTY_MAP;
		}

		HgCommand command = new HgCommand("log", "Retrieving history", hgRoot, false);

		command.setUsePreferenceTimeout(MercurialPreferenceConstants.LOG_TIMEOUT);
		command.addStyleFile(withFiles ? AbstractParseChangesetClient.STYLE_WITH_FILES
				: AbstractParseChangesetClient.STYLE_DEFAULT);

		addRange(command, startRev, limitNumber, isFile);

		if (isFile) {
			command.addOptions("-f"); //$NON-NLS-1$
		}

		command.addOptions(hgRoot.toRelative(path));

		String result = command.executeToString();
		if (result.length() == 0) {
			return EMPTY_MAP;
		}
		Map<IPath, Set<ChangeSet>> revisions = createLocalRevisions(
				new Path(path.getAbsolutePath()),
				result, Direction.LOCAL, null, null, null, hgRoot);
		return revisions;
	}

	private static void addRange(AbstractShellCommand command, int startRev, int limitNumber, boolean isFile) {
		if (startRev >= 0 && startRev != Integer.MAX_VALUE) {
			// always advise to follow until 0 revision: the reason is that log limit
			// might be bigger then the difference of two consequent revisions on a specific resource
			command.addOptions("-r"); //$NON-NLS-1$
			command.addOptions(startRev + ":" + 0); //$NON-NLS-1$
		}
		if(isFile && startRev == Integer.MAX_VALUE) {
			// always start with the tip to get the latest version of a file too
			// seems that hg log misses some versions if the file working copy is not at the tip
			command.addOptions("-r"); //$NON-NLS-1$
			command.addOptions("tip:0"); //$NON-NLS-1$
		}
		command.addOptions("--limit", (limitNumber > 0) ? limitNumber + "" : NOLIMIT); //$NON-NLS-1$ //$NON-NLS-2$
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

		return getChangeset(root, nodeId);
	}

	/**
	 * Tries to retrieve (possible not existing) changeset for the given id.
	 * @param hgRoot non null
	 * @param nodeId non null
	 * @return might return null if the changeset is not known/existing in the repo
	 */
	public static ChangeSet getChangeset(HgRoot root, String nodeId) {
		return new JHgChangeSet(root, LogCommandFlags.on(root.getRepository()).rev(nodeId)
				.execute().get(0));
	}
}
