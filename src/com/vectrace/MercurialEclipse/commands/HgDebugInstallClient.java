/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian Doetsch  -  implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.vectrace.MercurialEclipse.exception.HgException;

public class HgDebugInstallClient extends AbstractClient {
	public final static Map<String, Boolean> ENCODINGS = Collections
			.synchronizedMap(new HashMap<String, Boolean>());

	public static String debugInstall() throws HgException {
		AbstractShellCommand command = getDebugInstallCommand();
		return new String(command.executeToBytes(Integer.MAX_VALUE)).trim();
	}

	/**
	 * @return
	 */
	private static AbstractShellCommand getDebugInstallCommand() {
		// we don't really need a working dir...
		AbstractShellCommand command = new HgCommand("debuginstall", (File) null, true); //$NON-NLS-1$
		command.setShowOnConsole(false);
		return command;
	}

	/**
	 * @param defaultCharset
	 * @return
	 */
	public static boolean hgSupportsEncoding(String defaultCharset) {
		Boolean b = ENCODINGS.get(defaultCharset);
		if (b == null) {
			AbstractShellCommand cmd = getDebugInstallCommand();
			cmd.addOptions("--encoding", defaultCharset); //$NON-NLS-1$
			try {
				cmd.executeToString();
			} catch (HgException e) {
				// there might have been an exception but it is not necessarily
				// related to the encoding. so only return false if the following
				// string is in the message
				if (e.getMessage().contains("unknown encoding:")) { //$NON-NLS-1$
					ENCODINGS.put(defaultCharset, Boolean.FALSE);
					return false;
				}
			}
			ENCODINGS.put(defaultCharset, Boolean.TRUE);
			return true;
		}
		return b.booleanValue();
	}
}
