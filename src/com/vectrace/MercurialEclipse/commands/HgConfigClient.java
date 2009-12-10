/*******************************************************************************
 * Copyright (c) 2008 Bastian Doetsch and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian Doetsch implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.File;

import com.vectrace.MercurialEclipse.exception.HgException;

/**
 * @author bastian
 *
 */
public class HgConfigClient extends AbstractClient {
	public static String getHgConfigLine(File dir, String key)
			throws HgException {
		String[] lines = getHgConfigLines(dir, key);
		return lines[0];
	}

	/**
	 * @param dir
	 * @param key
	 * @return
	 * @throws HgException
	 */
	public static String[] getHgConfigLines(File dir, String key)
			throws HgException {
		AbstractShellCommand cmd = new HgCommand("showconfig", getWorkingDirectory(dir), //$NON-NLS-1$
				false);
		cmd.addOptions(key);
		String[] lines = cmd.executeToString().split("\n"); //$NON-NLS-1$
		return lines;
	}
}
