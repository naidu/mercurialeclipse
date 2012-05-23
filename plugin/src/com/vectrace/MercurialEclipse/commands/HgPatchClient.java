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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;

import com.aragost.javahg.commands.DiffCommand;
import com.aragost.javahg.commands.flags.DiffCommandFlags;
import com.aragost.javahg.commands.flags.ExportCommandFlags;
import com.aragost.javahg.internals.Utils;
import com.google.common.io.ByteStreams;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.history.MercurialRevision;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;

/**
 * TODO: use JavaHg
 */
public class HgPatchClient extends AbstractClient {

	public static final String PATCH_EXTENSION = ".diff";

	/**
	 * Import a patch. Throws an exception if there is a conflict
	 *
	 * @see #isPatchImportConflict(HgException)
	 *
	 * @param hgRoot non null
	 * @param patchLocation non null
	 * @param options non null
	 */
	public static String importPatch(HgRoot hgRoot, File patchLocation,
			ArrayList<String> options) throws HgException {
		AbstractShellCommand command = new HgCommand("import", "Importing patch", hgRoot, true); //$NON-NLS-1$
		command.setExecutionRule(new AbstractShellCommand.ExclusiveExecutionRule(hgRoot));
		command.addFile(patchLocation);
		command.addOptions(options.toArray(new String[options.size()]));
		return command.executeToString();
	}

	/**
	 * Determine if the given exception indicates a conflict occurred
	 *
	 * @param e
	 *            The exception to check
	 * @return True if the exception indicates a conflict occurred
	 */
	public static boolean isPatchImportConflict(HgException e) {
		return e.getMessage().contains("patch failed to apply.");
	}

	/**
	 * @param hgRoot
	 *            non null hg root
	 * @param resources
	 *            non null set of files to export as diff to the latest state. If the set is empty,
	 *            a complete diff of the hg root is exported
	 * @param patchFile
	 *            non null target file for the diff
	 * @param command
	 *            Optional configured command to execute
	 * @throws HgException
	 * @return True on success
	 */
	public static boolean exportPatch(HgRoot hgRoot, List<IResource> resources, File patchFile,
			DiffCommand command) throws HgException {
		if (command == null) {
			command = DiffCommandFlags.on(hgRoot.getRepository());
		}
		copy(command.stream(toFileArray(resources)), patchFile);

		return true;
	}

	/**
	 * export diff file to clipboard
	 *
	 * @param resources
	 * @throws HgException
	 */
	public static String exportPatch(HgRoot hgRoot, List<IResource> resources, DiffCommand command)
			throws HgException {
		if (command == null) {
			command = DiffCommandFlags.on(hgRoot.getRepository());
		}
		return command.execute(toFileArray(resources));
	}

	/**
	 * Export a changeset to a string
	 *
	 * @param root
	 *            The repository root
	 * @param cs
	 *            The changeset
	 * @return The string as a patch
	 * @throws HgException
	 */
	public static String exportPatch(HgRoot root, ChangeSet cs) throws HgException {
		return ExportCommandFlags.on(root.getRepository()).execute(cs.getNode());
	}

	/**
	 * Export a changeset to a file
	 *
	 * @param root
	 *            The repository root
	 * @param cs
	 *            The changeset
	 * @param patchFile
	 *            The file to output to
	 * @return True on success
	 * @throws HgException
	 */
	public static boolean exportPatch(HgRoot root, ChangeSet cs, File patchFile) throws HgException {
		copy(ExportCommandFlags.on(root.getRepository()).stream(cs.getNode()), patchFile);

		return true;
	}

	/**
	 * Get a diff for a single changeSet or a range for revisions.
	 *
	 * Use the extended diff format (--git) that shows renames and file attributes.
	 *
	 * @param hgRoot
	 *            The root. Must not be null.
	 * @param entry
	 *            Revision of the changeset or first revision of changeset-range (if secondEntry !=
	 *            null). Must not be null.
	 * @param secondEntry
	 *            second revision of changeset range. If null entry a diff will be created for
	 *            parameter entry a a single Changeset.
	 * @return Diff as a string in extended diff format (--git).
	 * @throws HgException
	 */
	public static String getDiff(HgRoot hgRoot, MercurialRevision entry,
			MercurialRevision secondEntry) throws HgException {

		DiffCommand command = DiffCommandFlags.on(hgRoot.getRepository());

		if (secondEntry == null) {
			command.change(entry.getChangeSet().getNode());
		} else {
			command.rev(entry.getChangeSet().getNode(), secondEntry.getChangeSet().getNode());
		}

		return command.execute();
	}

	private static void copy(InputStream in, File patchFile) throws HgException {
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(patchFile);
			ByteStreams.copy(in, out);
			in = null;
		} catch (FileNotFoundException e) {
			throw new HgException(e.getMessage(), e);
		} catch (IOException e) {
			throw new HgException(e.getMessage(), e);
		} finally {
			try {
				if (out != null) {
					try {
						out.close();
					} catch (IOException e) {
						throw new HgException(e.getMessage(), e);
					}
				}
			} finally {
				if (in != null) {
					try {
						Utils.consumeAll(in);
					} catch (IOException e) {
						throw new HgException(e.getMessage(), e);
					}
				}
			}
		}
	}
}
