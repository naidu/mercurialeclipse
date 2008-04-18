package com.vectrace.MercurialEclipse.commands;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

public class HgCatClient {

	public static String getContent(IFile file, String revision)
			throws HgException {
		HgCommand command = new HgCommand("cat", file.getProject()
				.getLocation().toFile(), true);
		if (revision != null && revision.length() != 0) {
			command.addOptions("--rev", revision);

		}
		command.addOptions(file.getProjectRelativePath().toOSString());
		return command.executeToString();
	}

	public static String getContentFromBundle(IFile file, String revision,
			String overlayBundle) throws HgException {
		List<String> command = new ArrayList<String>();
		command.add(MercurialUtilities.getHGExecutable());
		command.add("-R");
		command.add(overlayBundle);
		command.add("cat");
		if (revision != null && revision.length() != 0) {
			command.add("-r");
			command.add("tip");
		}
		command.add(file.getProjectRelativePath().toOSString());
		HgCommand hgCommand = new HgCommand(command, file
				.getProject().getLocation().toFile(), true);
		
		
		return hgCommand.executeToString();
	}
}
