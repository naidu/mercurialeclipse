/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		bastian	implementation
 * 		Andrei Loskutov (Intland) - bugfixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.File;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * @author bastian
 */
public class HgInitClient extends AbstractClient {

	/**
	 * Creates hg repository at given location
	 * @param file non null directory (which may not exist yet)
	 */
	public static String init(File file) throws HgException {
		AbstractShellCommand command = new HgCommand("init", ResourceUtils
				.getFirstExistingDirectory(file), false);
		command.addOptions(file.getAbsolutePath());
		command.setUsePreferenceTimeout(MercurialPreferenceConstants.DEFAULT_TIMEOUT);
		return command.executeToString();
	}
}
