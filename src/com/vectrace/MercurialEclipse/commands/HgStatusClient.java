/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - initial
 *     Bastian Doetsch           - changes
 *     Brian Wallis              - getMergeStatus
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.HgRevision;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;

/**
 * @author <a href="mailto:zsolt.koppany@intland.com">Zsolt Koppany</a>
 * @version $Id$
 */
public class HgStatusClient extends AbstractClient {
	public static String getStatus(IContainer root) throws HgException {
		return getStatus(root.getLocation().toFile());
	}

	public static String getStatus(File root) throws HgException {
		AbstractShellCommand command = new HgCommand("status", root, true); //$NON-NLS-1$
		// modified, added, removed, deleted, unknown, ignored, clean
		command.addOptions("-marduic"); //$NON-NLS-1$
		command.setUsePreferenceTimeout(MercurialPreferenceConstants.STATUS_TIMEOUT);
		return command.executeToString();
	}

	public static String getStatusWithoutIgnored(HgRoot root, IResource res) throws HgException {
		AbstractShellCommand command = new HgCommand("status", root, true); //$NON-NLS-1$

		// modified, added, removed, deleted, unknown, ignored, clean
		command.addOptions("-marduc"); //$NON-NLS-1$
		command.setUsePreferenceTimeout(MercurialPreferenceConstants.STATUS_TIMEOUT);
		if (res.getType() == IResource.FILE) {
			command.addOptions(res.getLocation().toFile().getAbsolutePath());
		}
		return command.executeToString();
	}

	public static String[] getUntrackedFiles(IContainer root) throws HgException {
		AbstractShellCommand command = new HgCommand("status", root, true); //$NON-NLS-1$
		command.setUsePreferenceTimeout(MercurialPreferenceConstants.STATUS_TIMEOUT);
		command.addOptions("-u", "-n"); //$NON-NLS-1$ //$NON-NLS-2$
		return command.executeToString().split("\n"); //$NON-NLS-1$
	}

	public static boolean isDirty(List<? extends IResource> resources) throws HgException {
		AbstractShellCommand command = new HgCommand("status", true); //$NON-NLS-1$
		command.setUsePreferenceTimeout(MercurialPreferenceConstants.STATUS_TIMEOUT);
		command.addOptions("-mard");// modified, added, removed, deleted //$NON-NLS-1$
		command.addFiles(resources);
		return command.executeToBytes().length != 0;
	}

	public static boolean isDirty(IProject project) throws HgException {
		AbstractShellCommand command = new HgCommand("status", project, true); //$NON-NLS-1$
		command.setUsePreferenceTimeout(MercurialPreferenceConstants.STATUS_TIMEOUT);
		command.addOptions("-mard");// modified, added, removed, deleted //$NON-NLS-1$
		return command.executeToBytes().length != 0;
	}

	public static String getMergeStatus(IResource res) throws HgException {
		AbstractShellCommand command = new HgCommand("identify", getWorkingDirectory(res), true); //$NON-NLS-1$
		// Full global IDs
		command.addOptions("-i","--debug"); //$NON-NLS-1$ //$NON-NLS-2$
		command.setUsePreferenceTimeout(MercurialPreferenceConstants.STATUS_TIMEOUT);
		String versionIds = command.executeToString().trim();

		Pattern p = Pattern.compile("^[0-9a-z]+\\+([0-9a-z]+)\\+$", Pattern.MULTILINE); //$NON-NLS-1$
		Matcher m = p.matcher(versionIds);
		if(m.matches()) {
			return m.group(1);
		}
		return null;
	}

	public static String getStatusWithoutIgnored(HgRoot root, List<IResource> files) throws HgException {
		AbstractShellCommand command = new HgCommand("status", root, true); //$NON-NLS-1$

		command.setUsePreferenceTimeout(MercurialPreferenceConstants.STATUS_TIMEOUT);
		// modified, added, removed, deleted, unknown, ignored, clean
		command.addOptions("-marduc"); //$NON-NLS-1$
		command.addFiles(files);
		return command.executeToString();
	}

	/**
	 * @return root relative paths of changed files, never null
	 */
	public static String[] getDirtyFiles(HgRoot root, File file) throws HgException {
		AbstractShellCommand command = new HgCommand("status", root, true); //$NON-NLS-1$

		command.setUsePreferenceTimeout(MercurialPreferenceConstants.STATUS_TIMEOUT);
		command.addOptions("-mard"); //$NON-NLS-1$
		String result = command.executeToString();
		if (result == null || result.length() == 0) {
			return new String[0];
		}
		// now we will have status AND path info
		String[] status = result.split("\n");
		for (int i = 0; i < status.length; i++) {
			// remove status info from status AND path line
			status[i] = status[i].substring(2);
		}
		return status;
	}

	/**
	 * Returns <b>possible</b> ancestor of the given file, if given file is a result of
	 * a copy or rename operation.
	 * <p>
	 * <b>Note</b>: this is a very inefficient algorithm, which may need a lot of time to
	 * complete.
	 *
	 * @param file
	 *            successor path (as full absolute file path)
	 * @param root
	 *            hg root
	 * @param firstKnownRevision the version at which the information about possible
	 *  parent file should be retrieved
	 *
	 * @return full absolute file path which was the source of the given file one changeset
	 *         before given version, or null if the given file was not copied or renamed
	 *         at given version.
	 * @throws HgException
	 */
	public static File getPossibleSourcePath(HgRoot root, File file, int firstKnownRevision) throws HgException{
		return getPossibleSourcePath(root, file, (firstKnownRevision - 1), "" + firstKnownRevision);
	}

	/**
	 * <b>Guesses</b> a <b>possible</b> ancestor of the given file, if given file is a result of
	 * a copy or rename operation.
	 * <p>
	 * <b>Note</b>: this is a fast but inaccurate algorithm, which may not return expected
	 * information
	 *
	 * @param file
	 *            successor path (as full absolute file path)
	 * @param root
	 *            hg root
	 * @param firstKnownRevision the version at which the information about possible
	 *  parent file should be retrieved
	 * @return full absolute file path which was the source of the given file one changeset
	 *         before given version, or null if the given file was not copied or renamed
	 *         at given version.
	 * @throws HgException
	 */
	public static File guessPossibleSourcePath(HgRoot root, File file, int firstKnownRevision) throws HgException {
		// See issue #10302: we trying to cheat: "hg status -arC --rev firstKnownRevision:tip file" will return
		// A file
		//   source_file
		// in case the given file was known between "firstKnownRevision" and "tip"
		// TODO not sure if "tip" works always as expected, but now it's my best guess for the upper limit
		return getPossibleSourcePath(root, file, firstKnownRevision, HgRevision.TIP.getChangeset());
	}

	private static File getPossibleSourcePath(HgRoot root, File file, int firstRev, String secondRev) throws HgException{
		AbstractShellCommand command = new HgCommand("status", root, true); //$NON-NLS-1$
		command.setUsePreferenceTimeout(MercurialPreferenceConstants.STATUS_TIMEOUT);
		command.addOptions("-arC"); //$NON-NLS-1$
		command.addOptions("--rev"); //$NON-NLS-1$
		command.addOptions(firstRev + ":" + secondRev); //$NON-NLS-1$
		command.addFiles(file.getAbsolutePath());
		String result = command.executeToString();
		if (result == null || result.length() == 0) {
			return null;
		}

		String relativePath = root.toRelative(file);

		String[] statusAndFileNames = result.split("\n"); //$NON-NLS-1$
		String prefixAdded = MercurialStatusCache.CHAR_ADDED + " ";
		String prefixRemoved = MercurialStatusCache.CHAR_REMOVED + " ";
		for (int i = 0; i < statusAndFileNames.length; i++) {
			// looks like: "A folder\foo" or "R folder\foo" or "folder\foo"
			String pathWithStatus = statusAndFileNames[i];
			if(pathWithStatus.startsWith(prefixAdded) || pathWithStatus.startsWith(prefixRemoved)){
				if(i + 1 < statusAndFileNames.length && pathWithStatus.endsWith(relativePath)){
					// XXX should not just trim whitespace in the path, if it contains whitespaces, it will not work
					// on the other side it's just idiotic to have filenames with leading or trailing spaces
					String nextLine = statusAndFileNames[i + 1].trim();
					if(!nextLine.startsWith(prefixAdded) && !nextLine.startsWith(prefixRemoved)) {
						return new File(root, nextLine);
					}
				}
			}
		}
		return null;
	}
}
