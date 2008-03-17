package com.vectrace.MercurialEclipse.commands;

import org.eclipse.core.resources.IContainer;

import com.vectrace.MercurialEclipse.exception.HgException;

public class HgStatusClient {

	public static String getStatus(IContainer root) throws HgException {
		HgCommand command = new HgCommand("status", root, true);
		return command.executeToString();
	}
}
