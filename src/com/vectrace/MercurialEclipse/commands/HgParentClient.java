/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

public class HgParentClient extends AbstractClient {

	private static final Pattern ANCESTOR_PATTERN = Pattern
			.compile("^([0-9]+):([0-9a-f]+)$"); //$NON-NLS-1$

	private static final Pattern LINE_SEPERATOR_PATTERN = Pattern.compile("\n");

	public static int[] getParents(IProject project) throws HgException {
		AbstractShellCommand command = new HgCommand("parents", project, false); //$NON-NLS-1$
		command.addOptions("--template", "{rev}\n"); //$NON-NLS-1$ //$NON-NLS-2$
		String[] lines = getLines(command.executeToString());
		int[] parents = new int[lines.length];
		for (int i = 0; i < lines.length; i++) {
			parents[i] = Integer.parseInt(lines[i]);
		}
		return parents;
	}

	public static String[] getParentNodeIds(HgRoot hgRoot)
			throws HgException {
		AbstractShellCommand command = new HgCommand("parents", hgRoot, false);
		command.addOptions("--template", "{node}\n"); //$NON-NLS-1$ //$NON-NLS-2$
		String[] lines = getLines(command.executeToString());
		String[] parents = new String[lines.length];
		for (int i = 0; i < lines.length; i++) {
			parents[i] = lines[i].trim();
		}
		return parents;
	}

	public static String[] getParentNodeIds(IResource resource, ChangeSet cs)
			throws HgException {
		AbstractShellCommand command = new HgCommand("parents", //$NON-NLS-1$
				getWorkingDirectory(resource), false);
		command
				.addOptions("--template", "{node}\n", "--rev", cs //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						.getChangeset());
		String[] lines = getLines(command.executeToString());
		String[] parents = new String[lines.length];
		for (int i = 0; i < lines.length; i++) {
			parents[i] = lines[i].trim();
		}
		return parents;
	}

	public static int findCommonAncestor(IProject project, int r1, int r2)
			throws HgException {
		AbstractShellCommand command = new HgCommand("debugancestor", project, false); //$NON-NLS-1$
		command.addOptions(Integer.toString(r1), Integer.toString(r2));
		String result = command.executeToString().trim();
		Matcher m = ANCESTOR_PATTERN.matcher(result);
		if (m.matches()) {
			return Integer.parseInt(m.group(1));
		}
		throw new HgException("Parse exception: '" + result + "'"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public static int findCommonAncestor(File file, String node1, String node2)
			throws HgException {
		AbstractShellCommand command = new HgCommand("debugancestor", //$NON-NLS-1$
				getWorkingDirectory(file), false);
		command.addOptions(node1, node2);
		String result = command.executeToString().trim();
		Matcher m = ANCESTOR_PATTERN.matcher(result);
		if (m.matches()) {
			return Integer.parseInt(m.group(1));
		}
		throw new HgException("Parse exception: '" + result + "'"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * This methods finds the common ancestor of two changesets, supporting
	 * overlays for using incoming changesets. Only one changeset may be
	 * incoming.
	 *
	 * @param file
	 *            workingDirectory
	 * @param cs1
	 *            first changeset
	 * @param cs2
	 *            second changeset
	 * @return the id of the ancestor
	 * @throws HgException
	 */
	public static int findCommonAncestor(File file, ChangeSet cs1, ChangeSet cs2)
			throws HgException {
		String result;
		try {
			List<String> commands = new ArrayList<String>();
			commands.add(MercurialUtilities.getHGExecutable());
			if (cs1.getBundleFile() != null || cs2.getBundleFile() != null) {
				commands.add("-R"); //$NON-NLS-1$
				if (cs1.getBundleFile() != null) {
					commands.add(cs1.getBundleFile().getCanonicalPath());
				} else {
					commands.add(cs2.getBundleFile().getCanonicalPath());
				}
			}
			commands.add("debugancestor"); //$NON-NLS-1$
			commands.add(cs1.getChangeset());
			commands.add(cs2.getChangeset());

			AbstractShellCommand command = new HgCommand(commands, getWorkingDirectory(file),
					false);
			result = command.executeToString().trim();
			Matcher m = ANCESTOR_PATTERN.matcher(result);
			if (m.matches()) {
				return Integer.parseInt(m.group(1));
			}
			throw new HgException("Parse exception: '" + result + "'"); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (NumberFormatException e) {
			throw new HgException(e.getLocalizedMessage(), e);
		} catch (IOException e) {
			throw new HgException(e.getLocalizedMessage(), e);
		}
	}

	public static String findCommonAncestorNodeId(IResource resource,
			String node1, String node2) throws HgException {
		AbstractShellCommand command = new HgCommand("debugancestor", //$NON-NLS-1$
				getWorkingDirectory(resource), false);
		command.addOptions(node1, node2);
		String result = command.executeToString().trim();
		Matcher m = ANCESTOR_PATTERN.matcher(result);
		if (m.matches()) {
			return m.group(2);
		}
		throw new HgException("Parse exception: '" + result + "'"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public static String[] getParents(IResource rev, String node)
			throws HgException {
		AbstractShellCommand command = new HgCommand("parents", rev.getProject(), false); //$NON-NLS-1$
		command.addOptions("--template", "{rev}:{node|short}\n"); //$NON-NLS-1$ //$NON-NLS-2$
		command.addOptions("-r", node); //$NON-NLS-1$
		String[] lines = getLines(command.executeToString());
		return lines;
	}

	/**
	 * Splits an output of a command into lines. Lines are separated by a newline character (\n).
	 *
	 * @param output
	 *            The output of a command.
	 * @return The lines of the output.
	 */
	private static String[] getLines(String output) {
		if (output == null || output.length() == 0) {
			return new String[0];
		}
		return LINE_SEPERATOR_PATTERN.split(output);
	}
}
