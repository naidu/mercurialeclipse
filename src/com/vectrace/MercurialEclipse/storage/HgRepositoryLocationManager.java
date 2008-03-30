/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Software Balm Consulting Inc (Peter Hunnisett <peter_hge at softwarebalm dot com>) - implementation
 *     Stefan Groschupf          - logError
 *     Jerome Negre              - storing in plain text instead of serializing Java Objects
 *******************************************************************************/
package com.vectrace.MercurialEclipse.storage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;

/**
 * A manager for all Mercurial repository locations.
 * 
 * TODO: Need a way to delete these repos. Repo Explorer perspective a la subclipse?
 */
public class HgRepositoryLocationManager {

	final static private String REPO_LOCACTION_FILE = "repositories.txt";

	private Set<HgRepositoryLocation> repos = new TreeSet<HgRepositoryLocation>();

	/**
	 * Return a <code>File</code> object representing the location file.
	 * The file may or may not exist and must be checked before use.
	 */
	private File getLocationFile() {
		return MercurialEclipsePlugin.getDefault().getStateLocation().append(
				REPO_LOCACTION_FILE).toFile();
	}

	/**
	 * Load all saved repository locations from the plug-in's default area.
	 */
	public void start() throws IOException {
		File file = getLocationFile();
		
		if (file.exists()) {
			String line;
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(new FileInputStream(file), "UTF-8"));
			
			try {
				while((line = reader.readLine()) != null) {
					repos.add(new HgRepositoryLocation(line));
				}
			} finally {
				reader.close();
			}
			
		}
	}

	/**
	 * Flush all repository locations out to a file in the plug-in's default area.
	 */
	public void stop() throws IOException {
		File file = getLocationFile();

		BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));

		try {
				for(HgRepositoryLocation repo : repos) {
					writer.write(repo.getUrl());
					writer.write('\n');
				}
		} finally {
			writer.close();
		}
	}

	/**
	 * Return an ordered list of all repository locations that are presently known.
	 */
	public Set<HgRepositoryLocation> getAllRepoLocations() {
		return Collections.unmodifiableSet(repos);
	}

	/**
	 * Add a repository location to the database.
	 */
	public boolean addRepoLocation(HgRepositoryLocation loc) {
		return repos.add(loc);
	}
}
