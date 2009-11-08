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
 *     Adam Berkes (Intland)     - bug fixes
 *     Andrei Loskutov (Intland) - bug fixes
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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgPathsClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.repository.IRepositoryListener;
import com.vectrace.MercurialEclipse.repository.RepositoryResourcesManager;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.team.cache.RefreshStatusJob;

/**
 * A manager for all Mercurial repository locations.
 */
public class HgRepositoryLocationManager {

	private static final RepositoryResourcesManager REPOSITORY_RESOURCES_MANAGER = RepositoryResourcesManager
			.getInstance();

	final static private String REPO_LOCATION_FILE = "repositories.txt"; //$NON-NLS-1$

	private final Map<IProject, SortedSet<HgRepositoryLocation>> projectRepos;
	private final HgRepositoryLocationParserDelegator delegator;

	public HgRepositoryLocationManager() {
		projectRepos = new HashMap<IProject, SortedSet<HgRepositoryLocation>>();
		delegator = new HgRepositoryLocationParserDelegator();
	}

	/**
	 * Return a <code>File</code> object representing the location file. The
	 * file may or may not exist and must be checked before use.
	 */
	private File getLocationFile() {
		return MercurialEclipsePlugin.getDefault().getStateLocation().append(
				REPO_LOCATION_FILE).toFile();
	}

	public boolean cleanup(IProject project) {
		return getProjectLocationFile(project).delete();
	}

	/**
	 * Load all saved repository locations from the plug-in's default area.
	 *
	 * @throws HgException
	 */
	public void start() throws IOException, HgException {
		getProjectRepos(true);
	}

	/**
	 * Flush all repository locations out to a file in the plug-in's default
	 * area.
	 */
	public void stop() throws IOException {
		File file = getLocationFile();

		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(file), "UTF-8")); //$NON-NLS-1$

		try {
			for (HgRepositoryLocation repo : getAllRepoLocations()) {
				String line = delegator.delegateCreate(repo);
				if(line != null){
					writer.write(line);
					writer.write('\n');
				}
			}
		} finally {
			writer.close();
		}
		saveProjectRepos();
	}

	/**
	 * Return an ordered list of all repository locations that are presently
	 * known.
	 */
	public Set<HgRepositoryLocation> getAllRepoLocations() {
		SortedSet<HgRepositoryLocation> allRepos = new TreeSet<HgRepositoryLocation>();
		for (SortedSet<HgRepositoryLocation> locations : projectRepos.values()) {
			allRepos.addAll(locations);
		}
		return allRepos;
	}

	public Set<HgRepositoryLocation> getAllProjectRepoLocations(IProject project) {
		SortedSet<HgRepositoryLocation> loc = projectRepos.get(project);
		if(loc != null) {
			return Collections.unmodifiableSet(loc);
		}
		return Collections.emptySet();
	}

	/**
	 * @param repo non null repo location
	 * @return a set of projects we know managed at given location, never null
	 */
	public Set<IProject> getAllRepoLocationProjects(HgRepositoryLocation repo) {
		synchronized (projectRepos) {
			try {
				getProjectRepos(false);
			} catch (Exception e) {
				MercurialEclipsePlugin.logError(e);
			}
		}
		Set<IProject> loc = projectRepos.keySet();

		Set<IProject> projects = new HashSet<IProject>();
		for (IProject project : loc) {
			SortedSet<HgRepositoryLocation> set = projectRepos.get(project);
			if(set != null && set.contains(repo)){
				projects.add(project);
			}
		}
		return Collections.unmodifiableSet(projects);
	}

	/**
	 * Add a repository location to the database.
	 */
	private boolean addRepoLocation(HgRepositoryLocation loc) {
		if (loc.getProjectName() != null) {
			IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(loc.getProjectName());
			if (project != null) {
				return internalAddRepoLocation(project, loc);
			}
		}
		return false;
	}

	/**
	 * Add a repository location to the database without to triggering loadRepos again
	 */
	private boolean internalAddRepoLocation(IProject project, HgRepositoryLocation loc) {
		if (loc == null) {
			return false;
		}

		synchronized (projectRepos) {
			SortedSet<HgRepositoryLocation> repoSet = projectRepos.get(project);
			if (repoSet == null) {
				repoSet = new TreeSet<HgRepositoryLocation>();
			}
			if (project.getName().equals(loc.getProjectName())) {
				loc.setProjectName(project.getName());
			}
			repoSet.add(loc);
			REPOSITORY_RESOURCES_MANAGER.repositoryAdded(loc);
			projectRepos.put(project, repoSet);
		}

		return true;
	}

	/**
	 * Add a repository location to the database. Associate a repository
	 * location to a particular project.
	 *
	 * @throws HgException
	 */
	public boolean addRepoLocation(IProject project, HgRepositoryLocation loc)
			throws HgException {
		return internalAddRepoLocation(project, loc);
	}

	private Map<IProject, SortedSet<HgRepositoryLocation>> getProjectRepos(boolean initialize)
			throws IOException, HgException {
		if (initialize) {
			Set<IProject> hgProjects = loadProjectRepos();
			for (IProject project : hgProjects) {
				new RefreshStatusJob("Init hg cache for " + project.getName(), project).schedule();
			}
		}
		return projectRepos;
	}

	/**
	 * @return set with ALL projects managed by hg, <b>not only</b> projects for which we know remote repo locations
	 * @throws IOException
	 * @throws HgException
	 */
	private Set<IProject> loadProjectRepos() throws IOException, HgException {
		projectRepos.clear();
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		Set<IProject> hgProjects = new HashSet<IProject>();

		for (IProject project : projects) {
			if (!project.isAccessible()
					|| !MercurialUtilities.hgIsTeamProviderFor(project, false)) {
				continue;
			}
			hgProjects.add(project);

			// Load .hg/hgrc paths first; plugin settings will override these
			Map<String, String> hgrcRepos = HgPathsClient.getPaths(project);
			for (Map.Entry<String, String> entry : hgrcRepos.entrySet()) {
				// if not existent, add to repository browser
				HgRepositoryLocation loc = updateRepoLocation(project,
						entry.getValue(),
						entry.getKey(),
						null, null);
				internalAddRepoLocation(project, loc);
			}

			File file = getProjectLocationFile(project);

			if (file.exists()) {
				String line;
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(new FileInputStream(file), "UTF-8")); //$NON-NLS-1$
				try {
					while ((line = reader.readLine()) != null) {
						try {
							HgRepositoryLocation loc = delegator.delegateParse(line);
							internalAddRepoLocation(project, loc);
						} catch (Exception e) {
							// log exception, but don't bother the user with it.
							MercurialEclipsePlugin.logError(e);
						}
					}
				} finally {
					reader.close();
				}
			}
		}
		return hgProjects;
	}

	/**
	 * Set given location as default (topmost in project repositories)
	 * @param project a valid project (not null)
	 * @param loc a valid repoository location (not null)
	 */
	public void setDefaultProjectRepository(IProject project,
			HgRepositoryLocation loc) {
		Assert.isNotNull(project);
		Assert.isNotNull(loc);
		SortedSet<HgRepositoryLocation> locations = projectRepos.get(project);
		loc.setLastUsage(new Date());
		if (locations != null && !locations.contains(loc)) {
			locations.add(loc);
		} else {
			internalAddRepoLocation(project, loc);
		}
	}

	/**
	 * Returns the default repository location for a project, if it is set.
	 * @return may return null
	 */
	public HgRepositoryLocation getDefaultProjectRepoLocation(IProject project) {
		SortedSet<HgRepositoryLocation> locations = projectRepos.get(project);
		if (locations != null && !locations.isEmpty()) {
			return locations.first();
		}
		return null;
	}

	private File getProjectLocationFile(IProject project) {
		File file = MercurialEclipsePlugin.getDefault().getStateLocation()
				.append(REPO_LOCATION_FILE + "_" + project.getName()).toFile(); //$NON-NLS-1$
		return file;
	}

	private void saveProjectRepos() throws IOException {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
				.getProjects();
		for (IProject project : projects) {
			File file = getProjectLocationFile(project);
			SortedSet<HgRepositoryLocation> repoSet = projectRepos.get(project);
			if (repoSet != null && !repoSet.isEmpty()) {
				BufferedWriter writer = null;
				try {
					writer = new BufferedWriter(new OutputStreamWriter(
							new FileOutputStream(file), "UTF-8"));
					for (HgRepositoryLocation repo : repoSet) {
						String line = delegator.delegateCreate(repo);
						if(line != null){
							writer.write(line);
							writer.write('\n');
						}
					}
				} finally {
					if(writer != null) {
						writer.close();
					}
				}
			}
		}
	}

	/**
	 * Get a repo by its URL. If URL is unknown, returns a new location.
	 * @return never returns null
	 */
	public HgRepositoryLocation getRepoLocation(String url) throws HgException {
		return getRepoLocation(url, null, null);
	}

	/**
	 * Get a repo by its URL. If URL is unknown, returns a new location.
	 * @return never returns null
	 */
	public HgRepositoryLocation getRepoLocation(String url, String user,
			String pass) throws HgException {
		HgRepositoryLocation location = matchRepoLocation(url);
		if (location != null) {
			if (user == null || user.length() == 0 || user.equals(location.getUser())) {
				return location;
			}
		}

		// make a new location if no matches exist or it's a different user
		return HgRepositoryLocationParser.parseLocation(false, url, user, pass);
	}

	/**
	 * Get a repo specified by properties. If repository for given url is unknown,
	 * returns a new location.
	 * @return never returns null
	 */
	public HgRepositoryLocation getRepoLocation(Properties props) throws HgException {
		String user = props.getProperty("user"); //$NON-NLS-1$
		if ((user == null) || (user.length() == 0)) {
			user = null;
		}
		String password = props.getProperty("password"); //$NON-NLS-1$
		if (user == null) {
			password = null;
		}
		String rootUrl = props.getProperty("rootUrl"); //$NON-NLS-1$
		if ((rootUrl == null) || (rootUrl.length() == 0)) {
			rootUrl = null;
		}
		String url = props.getProperty("url"); //$NON-NLS-1$
		if (url == null) {
			throw new HgException(Messages
					.getString("HgRepositoryLocation.urlMustNotBeNull")); //$NON-NLS-1$
		}

		HgRepositoryLocation location = matchRepoLocation(url);
		if (location != null) {
			if (user == null || user.length() == 0 || user.equals(location.getUser())) {
				return location;
			}
		}

		// make a new location if no matches exist or it's a different user
		return HgRepositoryLocationParser.parseLocation(false, url, user, password);
	}

	/**
	 * Simple search on existing repos.
	 * @return may return null
	 */
	private HgRepositoryLocation matchRepoLocation(String url) {
		if (url != null) {
			for (HgRepositoryLocation loc : getAllRepoLocations()) {
				if (url.equals(loc.getLocation())) {
					return loc;
				}
			}
		}
		return null;
	}

	/**
	 * Gets a repo by its URL. If URL is unknown, returns a new location,
	 * adding it to the global repositories cache. Will update stored
	 * last user and password with the provided values.
	 */
	public HgRepositoryLocation updateRepoLocation(IProject project, String url, String logicalName, String user, String pass) throws HgException {
		HgRepositoryLocation loc = matchRepoLocation(url);

		if (loc == null) {
			// in some cases url may be a repository database line
			loc = HgRepositoryLocationParser.parseLocation(logicalName, false, url, user, pass);
			addRepoLocation(loc);
			return loc;
		}

		boolean update = false;

		String myLogicalName = logicalName;
		String myUser = user;
		String myPass = pass;

		if (logicalName != null && logicalName.length() > 0 && !logicalName.equals(loc.getLogicalName())) {
			update = true;
		} else {
			myLogicalName = loc.getLogicalName();
		}
		if (user != null && user.length() > 0 && !user.equals(loc.getUser())) {
			update = true;
		} else {
			myUser = loc.getUser();
		}
		if (pass != null && pass.length() > 0 && !pass.equals(loc.getPassword())) {
			update = true;
		} else {
			myPass = loc.getPassword();
		}

		if (update) {
			HgRepositoryLocation updated = HgRepositoryLocationParser.parseLocation(myLogicalName, false, loc.getLocation(), myUser, myPass);
			updated.setLastUsage(new Date());
			updated.setProjectName(project.getName());

			for (SortedSet<HgRepositoryLocation> locs : projectRepos.values()) {
				if (locs.remove(updated)) {
					locs.add(updated);
				}
			}

			REPOSITORY_RESOURCES_MANAGER.repositoryModified(updated);
			return updated;
		}

		return loc;
	}

	/**
	 * Create a repository location instance from the given properties. The
	 * supported properties are: user The username for the connection (optional)
	 * password The password used for the connection (optional) url The url
	 * where the repository resides rootUrl The repository root url
	 */
	public HgRepositoryLocation fromProperties(IProject project, Properties configuration)
			throws HgException {

		String user = configuration.getProperty("user"); //$NON-NLS-1$
		if ((user == null) || (user.length() == 0)) {
			user = null;
		}
		String password = configuration.getProperty("password"); //$NON-NLS-1$
		if (user == null) {
			password = null;
		}
		String rootUrl = configuration.getProperty("rootUrl"); //$NON-NLS-1$
		if ((rootUrl == null) || (rootUrl.length() == 0)) {
			rootUrl = null;
		}
		String url = configuration.getProperty("url"); //$NON-NLS-1$
		if (url == null) {
			throw new HgException(Messages.getString("HgRepositoryLocation.urlMustNotBeNull")); //$NON-NLS-1$
		}
		return updateRepoLocation(project, url, null, user, password);
	}

	public void addRepositoryListener(IRepositoryListener repListener) {
		REPOSITORY_RESOURCES_MANAGER.addRepositoryListener(repListener);
	}

	public void refreshRepositories(IProgressMonitor monitor)
			throws HgException, IOException {
		stop();
		start();
	}

	/**
	 * Create a repository instance from the given properties. The supported
	 * properties are:
	 *
	 * user The username for the connection (optional) password The password
	 * used for the connection (optional) url The url where the repository
	 * resides
	 *
	 * The created instance is not known by the provider and it's user
	 * information is not cached. The purpose of the created location is to
	 * allow connection validation before adding the location to the provider.
	 *
	 */
	public HgRepositoryLocation createRepository(IProject project, Properties configuration)
			throws HgException {
		// Create a new repository location
		HgRepositoryLocation location = fromProperties(project, configuration);
		addRepoLocation(location);
		return location;
	}

	public void removeRepositoryListener(IRepositoryListener repositoryListener) {
		REPOSITORY_RESOURCES_MANAGER
				.removeRepositoryListener(repositoryListener);

	}

	public void disposeRepository(HgRepositoryLocation hgRepositoryLocation) {
		Assert.isNotNull(hgRepositoryLocation);
		synchronized (projectRepos) {
			for (Iterator<IProject> iterator = projectRepos.keySet().iterator(); iterator
					.hasNext();) {
				IProject project = iterator.next();
				if (project.isAccessible() && MercurialTeamProvider.isHgTeamProviderFor(project)) {
					SortedSet<HgRepositoryLocation> pRepos = projectRepos
							.get(project);
					if (pRepos != null) {
						for (HgRepositoryLocation repo : pRepos) {
							if (repo.equals(hgRepositoryLocation)) {
								pRepos.remove(repo);
								REPOSITORY_RESOURCES_MANAGER.repositoryRemoved(hgRepositoryLocation);
								break;
							}
						}
					}
				}
			}
		}
	}

}
