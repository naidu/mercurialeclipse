package com.vectrace.MercurialEclipse.commands;

import org.eclipse.core.resources.IContainer;

import com.vectrace.MercurialEclipse.exception.HgException;

public class HgIdentClient {

	public static String getCurrentRevision(IContainer root) throws HgException {
		HgCommand command = new HgCommand("ident", root, true);
		command.addOptions(
				"-n",
				"-i");
		return command.executeToString().trim();
	}
}
