package com.vectrace.MercurialEclipse.commands;

import org.eclipse.core.resources.IFile;

import com.vectrace.MercurialEclipse.exception.HgException;

public class HgCatClient {

	public static String getContent(IFile file, String revision) throws HgException {
		HgCommand command = new HgCommand("cat", file.getParent(), true, true);
		if(revision != null && revision.length() != 0) {
			command.addOptions(
					"--rev",
					revision);
		}
		command.addFiles(file.getName());
		return command.executeToString();
	}
}
