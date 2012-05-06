/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch           - implementation
 *     Andrei Loskutov           - bug fixes
 *     Adam Berkes (Intland)     - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import com.aragost.javahg.commands.ResolveStatusLine;
import com.aragost.javahg.commands.flags.ResolveCommandFlags;
import com.aragost.javahg.merge.ConflictResolvingContext;
import com.aragost.javahg.merge.FlagConflict;
import com.aragost.javahg.merge.KeepDeleteConflict;
import com.google.common.collect.Lists;
import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.ResolveStatus;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class HgResolveClient extends AbstractClient {

	/**
	 * List merge state of files after merge
	 */
	public static List<ResolveStatus> list(IResource res) throws HgException {
		HgRoot hgRoot = getHgRoot(res);
		IProject project = res.getProject();

		List<ResolveStatusLine> list = ResolveCommandFlags.on(hgRoot.getRepository()).list();
		List<ResolveStatus> result = Lists.newArrayList();

		for (ResolveStatusLine line : list) {
			// Status line is always hg root relative. For those projects
			// which has different project root hg root relative path must
			// be converted to project relative
			IResource iFile = ResourceUtils.convertRepoRelPath(hgRoot, project, line.getFileName());
			if (iFile != null) {
				result.add(new ResolveStatus(iFile, line.getType()));
			}
		}
		return result;
	}

	/**
	 * List merge state of files after merge
	 */
	public static List<ResolveStatus> list(HgRoot hgRoot) {
		List<ResolveStatusLine> list = ResolveCommandFlags.on(hgRoot.getRepository()).list();
		List<ResolveStatus> result = Lists.newArrayList();

		for (ResolveStatusLine line : list) {
			// Status line is always hg root relative. For those projects
			// which has different project root (always deeper than hg root)
			// hg root relative path must be converted
			IResource iFile = ResourceUtils.getFileHandle(hgRoot.toAbsolute(new Path(line
					.getFileName())));
			if (iFile != null) {
				result.add(new ResolveStatus(iFile, line.getType()));
			}
		}

		return result;
	}

	/**
	 * Mark a resource as resolved ("R")
	 */
	public static void markResolved(HgRoot hgRoot, IFile ifile) throws HgException {
		File file = ResourceUtils.getFileHandle(ifile);
		try {
			ResolveCommandFlags.on(hgRoot.getRepository()).mark(file.getCanonicalPath());
			// cleanup .orig files left after merge
			File origFile = new File(file.getAbsolutePath() + ".orig");
			if (origFile.isFile()) {
				IResource fileToDelete = ResourceUtils.convert(origFile);
				boolean deleted = origFile.delete();
				if (!deleted) {
					MercurialEclipsePlugin.logInfo("Failed to cleanup " + origFile
							+ " file after merge", null);
				} else {
					try {
						fileToDelete.refreshLocal(IResource.DEPTH_ZERO, null);
					} catch (CoreException e) {
						MercurialEclipsePlugin.logError(e);
					}
				}
			}
			refreshStatus(ifile);
		} catch (IOException e) {
			throw new HgException(e.getLocalizedMessage(), e);
		}
	}

	/**
	 * Mark a resource as unresolved ("U")
	 */
	public static void markUnresolved(HgRoot hgRoot, IFile ifile) throws HgException {
		File file = ResourceUtils.getFileHandle(ifile);

		try {
			ResolveCommandFlags.on(hgRoot.getRepository()).unmark(file.getCanonicalPath());
			refreshStatus(ifile);
		} catch (IOException e) {
			throw new HgException(e.getLocalizedMessage(), e);
		}
	}

	private static void refreshStatus(IResource res) throws HgException {
		MercurialStatusCache.getInstance().refreshStatus(res, null);
		ResourceUtils.touch(res);
	}

	/**
	 * Executes resolve command to find change sets necessary for merging
	 * <p>
	 * WARNING: This method potentially reverts changes!
	 * <p>
	 * Future: We should write some python to interface with the Mercurial API directly to get this
	 * info so we don't have to do operations with side effects or rely on --debug output.
	 *
	 * @param file
	 *            The file to consider
	 * @return An array of length 3 of changeset ids: result[0] - 'my' result[1] - 'other' result[2]
	 *         - 'base'
	 * @throws HgException
	 * @deprecated TODO: find a better way to do this
	 */
	@Deprecated
	public static String[] restartMergeAndGetChangeSetsForCompare(IFile file) throws HgException {
		String[] results = new String[3];
		HgCommand command = new HgCommand("resolve", "Invoking resolve to find parent information",
				file, false);

		command.addOptions("--config", "ui.merge=internal:mustfail", "--debug");
		command.addFiles(file);

		String stringResult = "";
		try {
			command.executeToString();
		} catch (HgException e) {
			// exception is expected here
			stringResult = e.getMessage();
		}

		String filename = file.getName();
		String patternString = "my .*" + filename + "@?([0-9a-fA-F]*)\\+?[\\s]" + "other .*"
				+ filename + "@?([0-9a-fA-F]*)\\+?[\\s]" + "ancestor .*" + filename
				+ "@?([0-9a-fA-F]*)\\+?[\\s]";

		Matcher matcher = Pattern.compile(patternString).matcher(stringResult);

		if (matcher.find() && matcher.groupCount() == 3) {
			results[0] = matcher.group(1); // my
			results[1] = matcher.group(2); // other
			results[2] = matcher.group(3); // ancestor
		}

		return results;
	}

	/**
	 * Resolve all conflicts using pre-merge, or external merge tool if configured
	 * @param hgRoot TODO
	 * @param hgRoot The root to resolve for
	 *
	 * @return True if all conflicts are resolved
	 */
	public static boolean autoResolve(HgRoot hgRoot, ConflictResolvingContext ctx) {
		// TODO: handle keep-delete and flag conflicts
		// TODO: should this instead be done in the merge view asyncronously?
		for (KeepDeleteConflict conflict : ctx.getKeepDeleteConflicts()) {

		}

		for (FlagConflict conflict : ctx.getFlagConflicts()) {

		}

		return autoResolve(hgRoot);
	}

	/**
	 * Attempt to externally resolve all conflicts: If external merge tool preference is set invokes
	 * simple resolve. Otherwise resolve is invoked with an invalid merge tool so Mercurial's
	 * premerge algorithm is invoked and the file is left unresolved if pre-merge fails.
	 *
	 * @param hgRoot The root to resolve for
	 * @return True if all conflicts are resolved
	 */
	public static boolean autoResolve(HgRoot hgRoot) {

		if (isUseExternalMergeTool()) {
			ResolveCommandFlags.on(hgRoot.getRepository()).execute();
		} else {
			// Do resolve one by one because we're using an invalid merge tool so only pre-merge is done.
			for(ResolveStatusLine line : ResolveCommandFlags.on(hgRoot.getRepository()).list()) {
				if (line.getType() == ResolveStatusLine.Type.UNRESOLVED) {
					ResolveCommandFlags.on(hgRoot.getRepository()).tool("simplemerge").execute(line.getFileName());
				}
			}
		}

		for(ResolveStatusLine line : ResolveCommandFlags.on(hgRoot.getRepository()).list()) {
			if (line.getType() == ResolveStatusLine.Type.UNRESOLVED) {
				return false;
			}
		}

		return true;
	}
}
