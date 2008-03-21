package com.vectrace.MercurialEclipse.commands;

import org.eclipse.core.resources.IProject;

import com.vectrace.MercurialEclipse.exception.HgException;

public class HgTagClient {

	public static String[] getTags(IProject project) throws HgException {
		HgCommand command = new HgCommand("tags", project, true);
		return command.executeToString().split("\n");
	}
}
