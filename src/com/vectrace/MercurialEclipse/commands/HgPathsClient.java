/*******************************************************************************
 * Copyright (c) 2008 Bastian Doetsch and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch - implementation
 *     Zsolt Koppany (Intland) - bug fixes
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/

package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.Branch;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.utils.IniFile;

public class HgPathsClient extends AbstractClient {
	public static final String DEFAULT = Branch.DEFAULT;
	public static final String DEFAULT_PULL = "default-pull"; //$NON-NLS-1$
	public static final String DEFAULT_PUSH = "default-push"; //$NON-NLS-1$
	public static final String PATHS_SECTION = "paths"; //$NON-NLS-1$

	public static Map<String, String> getPaths(HgRoot hgRoot) throws HgException {
		File hgrc = hgRoot.getConfig();
		Map<String,String> paths = new HashMap<String, String>();
		if (hgrc == null) {
			return paths;
		}
		try {
			IniFile ini = new IniFile(hgrc.getAbsolutePath());
			Map<String,String> section = ini.getSection(PATHS_SECTION);
			if (section != null) {
				paths.putAll(section);
			}
		} catch (IOException e) {
			throw new HgException("Unable to read paths from: " + hgrc, e);
		}

		return paths;
	}
}
