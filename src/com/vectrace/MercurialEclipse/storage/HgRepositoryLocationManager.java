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
 *     Bastian Doetsch           - support for project specific repository locations
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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

/**
 * A manager for all Mercurial repository locations.
 * 
 * TODO: Need a way to delete these repos. Repo Explorer perspective a la subclipse?
 */
public class HgRepositoryLocationManager {

	final static private String REPO_LOCACTION_FILE = "repositories.txt";

	private Set<HgRepositoryLocation> repos = new TreeSet<HgRepositoryLocation>();
	private Map<IProject, SortedSet<HgRepositoryLocation>> projectRepos = null;

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
	 * @throws HgException 
	 */
	public void start() throws IOException, HgException {
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
		loadProjectRepos();
		
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
		saveProjectRepos();
	}

	/**
	 * Return an ordered list of all repository locations that are presently known.
	 */
	public Set<HgRepositoryLocation> getAllRepoLocations() {
		return Collections.unmodifiableSet(repos);
	}
	
	public Set<HgRepositoryLocation> getAllProjectRepoLocations(IProject project) {		
		return Collections.unmodifiableSet(projectRepos.get(project));
	}

	/**
	 * Add a repository location to the database.
	 */
	public boolean addRepoLocation(HgRepositoryLocation loc) {
		return repos.add(loc);
	}
	
	/**
	 * Add a repository location to the database.
	 * @throws HgException 
	 */
	public boolean addRepoLocation(IProject project, HgRepositoryLocation loc) throws HgException {
		if (loc == null) {
			return false;
		}
		if (projectRepos == null){
			try {
				projectRepos = getProjectRepos();
			} catch (IOException e) {				
				MercurialEclipsePlugin.logError(e);
			}
		}
		SortedSet<HgRepositoryLocation>repoSet = projectRepos.get(project);
		if (repoSet == null){			
			repoSet = new TreeSet<HgRepositoryLocation>();
		}
		repoSet.add(loc);
		projectRepos.put(project,repoSet);
		repos.add(loc);
		return true;
	}

	private Map<IProject, SortedSet<HgRepositoryLocation>> getProjectRepos() throws IOException, HgException {
		if (projectRepos == null){
			loadProjectRepos();
		}		
		return projectRepos;
	}

	private void loadProjectRepos() throws IOException, HgException {
		projectRepos = new HashMap<IProject, SortedSet<HgRepositoryLocation>>();
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
				.getProjects();

		for (IProject project : projects) {
			if (!project.isOpen()){
				continue;
			}
			boolean createSrcRepository = false;
			try {
				String url = project
						.getPersistentProperty(MercurialTeamProvider.QUALIFIED_NAME_PROJECT_SOURCE_REPOSITORY);
				if (url != null) {
					HgRepositoryLocation srcRepository = new HgRepositoryLocation(
							url);
					addRepoLocation(project, srcRepository);
				} else {
					createSrcRepository = true;
				}
			} catch (CoreException e) {
				MercurialEclipsePlugin.logError(e);
				throw new HgException(e);
			}
			File file = getProjectLocationFile(project);

			if (file.exists()) {
				String line;
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(new FileInputStream(file),
								"UTF-8"));

				try {
					while ((line = reader.readLine()) != null) {
						HgRepositoryLocation loc = new HgRepositoryLocation(
								line);
						addRepoLocation(project, loc);
						
						if (createSrcRepository) {
							try {
								project
										.setPersistentProperty(
												MercurialTeamProvider.QUALIFIED_NAME_PROJECT_SOURCE_REPOSITORY,
												loc.getUrl());
							} catch (CoreException e) {
								MercurialEclipsePlugin.logError(e);
							}
							createSrcRepository = false;
						}
					}
				} finally {
					reader.close();
				}
			}
		}
	}

	/**
	 * @param project
	 * @return
	 */
	private File getProjectLocationFile(IProject project) {
		File file = MercurialEclipsePlugin.getDefault().getStateLocation()
				.append(REPO_LOCACTION_FILE + "_" + project.getName())
				.toFile();
		return file;
	}
	
	private void saveProjectRepos() throws IOException {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
				.getProjects();
		for (IProject project : projects) {
			File file = getProjectLocationFile(project);

			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(file), "UTF-8"));

			try {
				SortedSet<HgRepositoryLocation>repoSet = projectRepos.get(project);
				if (repoSet != null) {
					for (HgRepositoryLocation repo : repoSet) {
						writer.write(repo.getUrl());
						writer.write('\n');
					}
				}
			} finally {
				writer.close();
			}
		}
	}
		
}
