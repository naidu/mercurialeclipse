/*******************************************************************************
 * Copyright (c) 2008-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *     Steeven Lee               - import/export stuff
 *     Bastian Doetsch           - additions
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.compare.patch.IFilePatch;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.history.MercurialRevision;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.utils.PatchUtils;

public class HgPatchClient extends AbstractClient {

	/**
	 *
	 * @param hgRoot non null
	 * @param patchLocation non null
	 * @param options non null
	 */
	public static String importPatch(HgRoot hgRoot, File patchLocation,
			ArrayList<String> options) throws HgException {
		AbstractShellCommand command = new HgCommand("import", hgRoot, true); //$NON-NLS-1$
		command.setExecutionRule(new AbstractShellCommand.ExclusiveExecutionRule(hgRoot));
		command.addFiles(patchLocation.getAbsolutePath());
		command.addOptions(options.toArray(new String[options.size()]));
		return command.executeToString();
	}

	/**
	 * @param hgRoot non null hg root
	 * @param resources non null set of files to export as diff to the latest state. If the set
	 * 	is empty, a complete diff of the hg root is exported
	 * @param patchFile non null target file for the diff
	 * @param options non null list of options, may be empty
	 * @throws HgException
	 */
	public static boolean exportPatch(HgRoot hgRoot, Set<IPath> resources,
			File patchFile, ArrayList<String> options) throws HgException {
		AbstractShellCommand command = new HgCommand("diff", hgRoot, true); //$NON-NLS-1$
		if(resources.size() > 0) {
			command.addFiles(resources);
		}
		command.addOptions(options.toArray(new String[options.size()]));
		return command.executeToFile(patchFile, false);
	}

	/**
	 * export diff file to clipboard
	 *
	 * @param resources
	 * @throws HgException
	 */
	public static String exportPatch(File workDir, List<IResource> resources,
			ArrayList<String> options) throws HgException {
		AbstractShellCommand command = new HgCommand(
				"diff", getWorkingDirectory(workDir), true); //$NON-NLS-1$
		command.addFiles(resources);
		command.addOptions(options.toArray(new String[options.size()]));
		return command.executeToString();
	}

	public static String getDiff(File workDir) throws HgException {
		AbstractShellCommand command = new HgCommand(
				"diff", getWorkingDirectory(workDir), true); //$NON-NLS-1$
		return command.executeToString();
	}

	public static String getDiff(File workDir, File file) throws HgException {
		AbstractShellCommand command = new HgCommand(
				"diff", getWorkingDirectory(workDir), true); //$NON-NLS-1$
		command.addOptions(file.getAbsolutePath());
		return command.executeToString();
	}

	public static String getDiff(HgRoot hgRoot, MercurialRevision entry, MercurialRevision secondEntry) throws HgException {
		HgCommand command = new HgCommand("diff", hgRoot, true);
		if( secondEntry == null ){
			command.addOptions("-c", "" + entry.getChangeSet().getRevision().getChangeset());
		} else {
			command.addOptions("-r", ""+entry.getChangeSet().getRevision().getChangeset());
			command.addOptions("-r", ""+secondEntry.getChangeSet().getRevision().getChangeset());
		}
		command.addOptions("--git");
		return command.executeToString();
	}

	public static enum DiffLineType { HEADER, META, ADDED, REMOVED, CONTEXT }

	// TODO Check this against git diff specification
	public static DiffLineType getDiffLineType(String line) {
		if(line.startsWith("diff ")) {
			return DiffLineType.HEADER;
		} else if(line.startsWith("+++ ")) {
			return DiffLineType.META;
		} else if(line.startsWith("--- ")) {
			return DiffLineType.META;
		} else if(line.startsWith("@@ ")) {
			return DiffLineType.META;
			// TODO there are some more things
		} else if(line.startsWith("new file mode")) {
			return DiffLineType.META;
		} else if(line.startsWith("\\ ")) {
			return DiffLineType.META;
		} else if(line.startsWith("+")) {
			return DiffLineType.ADDED;
		} else if(line.startsWith("-")) {
			return DiffLineType.REMOVED;
		} else {
			return DiffLineType.CONTEXT;
		}
	}

	public IFilePatch[] getFilePatchesFromDiff(File file) throws HgException {
		AbstractShellCommand command = new HgCommand(
				"diff", getWorkingDirectory(getWorkingDirectory(file)), true); //$NON-NLS-1$
		String patchString = command.executeToString();
		return PatchUtils.getFilePatches(patchString);
	}


}
