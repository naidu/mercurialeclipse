package com.vectrace.MercurialEclipse.commands;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

import com.vectrace.MercurialEclipse.exception.HgException;

public class HgLogClient {

	public static String[] getRevisions(IProject project) throws HgException {
		HgCommand command = new HgCommand("log", project, true, true);
		command.addOptions(
				"--template",
				"{rev}:{node} {date|isodate} {author|person}\n");

		return command.executeToString().split("\n");
	}
	
	public static String[] getRevisions(IFile file) throws HgException {
		HgCommand command = new HgCommand("log", file.getParent(), true, true);
		command.addOptions(
				"--template",
				"{rev}:{node} {date|isodate} {author|person}\n");
		command.addFiles(file.getName());

		return command.executeToString().split("\n");
	}
}
