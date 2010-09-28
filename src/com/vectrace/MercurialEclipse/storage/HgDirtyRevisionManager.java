/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and othes.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Soren Mathiasen (Schantz)	- implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;

/**
 * Manager to keep track of dirty mercurial repositories.
 * The thought is that when we finish doing a pull or push we save the latest revision number. Then we can
 * check to see if the current revision number is different then the one we have saved, then we have made some
 * local changes that we need to push some time.
 *
 */
public class HgDirtyRevisionManager {

	private static final String REPOS_FILE = "dirtyrepos.properties"; //$NON-NLS-1$

	private static Properties props;

	private static final HgDirtyRevisionManager manager = new HgDirtyRevisionManager();

	private HgDirtyRevisionManager() {
		props = new Properties();
	}

	public static HgDirtyRevisionManager getInstance() {
		return manager;
	}

	public void saveLatestRevision(String repos, String revision) {
		props.setProperty(repos, revision);
	}

	private File getLocationFile() {
		return MercurialEclipsePlugin.getDefault().getStateLocation().append(REPOS_FILE).toFile();
	}

	/**
	 * Load all saved revisions from the plug-in's default area.
	 *
	 * @throws HgException
	 */
	public void start() throws HgException {
		File file = getLocationFile();
		if (!file.isFile()) {
			return;
		}
		try {
			props.load(new FileInputStream(getLocationFile()));
		} catch (FileNotFoundException e) {
			MercurialEclipsePlugin.logError(e);
		} catch (IOException e) {
			MercurialEclipsePlugin.logError(e);
		}
	}

	public void stop() throws IOException {
		props.store(new FileOutputStream(getLocationFile()), "");
	}

	/**
	 * @param name
	 * @return
	 */
	public boolean isReposDirty(String repos, String revision) {
		return props.getProperty(repos) != null && !props.getProperty(repos).equals(revision);
	}

}
