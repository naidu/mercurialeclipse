/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * soren	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands.extensions.lock;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.HgRoot;

/**
 * @author soren
 *
 */
public class LockHelper {

	public static List<String> getLockedFiles(HgRoot root) {
		ArrayList<String> files = new ArrayList<String>();

		File lockFile = new File(root, ".hg/lock.lck");
		if (lockFile.isFile()) {
			FileReader r = null;
			BufferedReader br = null;
			try {
				r = new FileReader(lockFile);
				br = new BufferedReader(r);
				String line = null;
				while ((line = br.readLine()) != null) {
					files.add("L "+line);
				}
			} catch (Exception e) {
				MercurialEclipsePlugin.logError(e);
			} finally {
				try {
					if(br != null) {
						br.close();
					}
					if(r != null) {
						r.close();
					}
				} catch (Exception e) {
					MercurialEclipsePlugin.logError(e);
				}
			}

		}

		return files;
	}
}
