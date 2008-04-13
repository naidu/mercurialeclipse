package com.vectrace.MercurialEclipse.commands;

import java.util.Map;
import java.util.SortedSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;

public class HgLogClient {

	private static final Pattern GET_REVISIONS_PATTERN = Pattern
			.compile("^([0-9]+):([a-f0-9]+) ([^ ]+ [^ ]+ [^ ]+) (.+)$");

	public static ChangeSet[] getRevisions(IProject project) throws HgException {
		HgCommand command = new HgCommand("log", project, true);
		return getRevisions(command);
	}

	public static ChangeSet[] getRevisions(IFile file) throws HgException {
		HgCommand command = new HgCommand("log", file.getParent(), true);
		command.addOptions("-f");
		command.addFiles(file.getName());
		return getRevisions(command);
	}

	public static ChangeSet[] getHeads(IProject project) throws HgException {
		HgCommand command = new HgCommand("heads", project, true);
		return getRevisions(command);
	}

	public static String getGraphicalLog(IProject project) throws HgException {
		HgCommand command = new HgCommand("glog", project, false);
		command.addOptions("--config", "extensions.hgext.graphlog=");
		return command.executeToString();
	}

	/**
	 * 
	 * @param command
	 *            a command with optionally its Files set
	 * @return
	 * @throws HgException
	 */
	private static ChangeSet[] getRevisions(HgCommand command)
			throws HgException {
		command.addOptions("--template",
				"{rev}:{node} {date|isodate} {author|person}\n");

		String[] lines = null;
		try {
			lines = command.executeToString().split("\n");
		} catch (HgException e) {
			if (!e
					.getMessage()
					.contains(
							"abort: can only follow copies/renames for explicit file names")) {
				throw new HgException(e);
			}
			return null;
		}
		int length = lines.length;
		ChangeSet[] changeSets = new ChangeSet[length];
		for (int i = 0; i < length; i++) {
			Matcher m = GET_REVISIONS_PATTERN.matcher(lines[i]);
			if (m.matches()) {
				ChangeSet changeSet = new ChangeSet(Integer
						.parseInt(m.group(1)), m.group(2), m.group(4), m
						.group(3));
				changeSets[i] = changeSet;
			} else {
				throw new HgException("Parse exception: '" + lines[i] + "'");
			}

		}

		return changeSets;
	}

	public static Map<IResource, SortedSet<ChangeSet>> getCompleteProjectLog(
			IProject proj) throws HgException {
		HgCommand command = new HgCommand("log", proj, false);

		command.addOptions("--template", HgIncomingClient.template);
		String result = command.executeToString();
		result = result.substring(result.lastIndexOf("\n") + 1);
		if (result.contains("no changes found")) {
			return null;
		}
		Map<IResource, SortedSet<ChangeSet>> revisions = HgIncomingClient
				.createMercurialRevisions(result, proj, "!!",
						HgIncomingClient.template, ";;", null);
		return revisions;
	}
}
