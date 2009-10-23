/*
 * Copyright by Intland Software
 *
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Intland Software. ("Confidential Information"). You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Intland.
 */

package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.Branch;
import com.vectrace.MercurialEclipse.utils.IniFile;

/**
 * @author <a href="mailto:zsolt.koppany@intland.com">Zsolt Koppany</a>
 * @version $Id$
 */
public class HgPathsClient {
	public static final String DEFAULT = Branch.DEFAULT;
	public static final String DEFAULT_PULL = "default-pull"; //$NON-NLS-1$
	public static final String DEFAULT_PUSH = "default-push"; //$NON-NLS-1$
	public static final String PATHS_LOCATION = "/.hg/hgrc"; //$NON-NLS-1$
	public static final String PATHS_SECTION = "paths"; //$NON-NLS-1$

	public static Map<String, String> getPaths(IProject project) throws HgException {
		File hgrc = new File (project.getLocation() + PATHS_LOCATION);

		if (!hgrc.exists()) {
			return new HashMap<String, String>();
		}

		Map<String,String> paths = new HashMap<String,String>();

		try {
			FileInputStream input = new FileInputStream(hgrc);
			try {
			    URL hgrcUrl = new URL("file", "", hgrc.getAbsolutePath());

				IniFile ini = new IniFile(hgrcUrl);
				Map<String,String> section = ini.getSection(PATHS_SECTION);
				if (section != null) {
					paths.putAll(section);
				}
			} finally {
				input.close();
			}
		} catch (IOException e) {
			// TODO: Fix log message
			throw new HgException("Unable to read paths", e);
		}

		return paths;
	}
}
