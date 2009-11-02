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
import org.eclipse.core.runtime.CoreException;
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

    private final SortedSet<HgRepositoryLocation> repos;
    private final Map<IProject, SortedSet<HgRepositoryLocation>> projectRepos;
    private boolean initDone;

    public HgRepositoryLocationManager() {
        repos = new TreeSet<HgRepositoryLocation>();
        projectRepos = new HashMap<IProject, SortedSet<HgRepositoryLocation>>();
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
        File file = getLocationFile();

        if (file.exists()) {
            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(file), "UTF-8")); //$NON-NLS-1$

            try {
                while ((line = reader.readLine()) != null) {
                    addRepoLocation(new HgRepositoryLocation(null, false, line, null,
                            null));
                }
            } catch (HgException e) {
                // we don't want to load it - it will be cleaned when saving
                MercurialEclipsePlugin.logError(e);
            } finally {
                reader.close();
            }

        }
        Set<IProject> managedProjects = loadProjectRepos();
        for (IProject project : managedProjects) {
            new RefreshStatusJob("Init hg cache for " + project.getName(), project).schedule(50);
        }
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
            for (HgRepositoryLocation repo : repos) {
                writer.write(repo.getSaveString());
                writer.write('\n');
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
        return Collections.unmodifiableSet(repos);
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
                getProjectRepos();
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
        synchronized (repos) {
            repos.remove(loc);
            repos.add(loc);
            REPOSITORY_RESOURCES_MANAGER.repositoryAdded(loc);
            return true;
        }
    }

    /**
     * Add a repository location to the database without to triggering loadRepos again
     */
    private boolean internalAddRepoLocation(IProject project, HgRepositoryLocation loc) {
        if (loc == null) {
            return false;
        }

        addRepoLocation(loc);

        synchronized (projectRepos) {

            SortedSet<HgRepositoryLocation> repoSet = projectRepos.get(project);
            if (repoSet == null) {
                repoSet = new TreeSet<HgRepositoryLocation>();
            }
            repoSet.add(loc);
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
        if (loc == null) {
            return false;
        }

        addRepoLocation(loc);

        synchronized (projectRepos) {
            try {
                getProjectRepos();
            } catch (IOException e) {
                MercurialEclipsePlugin.logError(e);
            }

            SortedSet<HgRepositoryLocation> repoSet = projectRepos.get(project);
            if (repoSet == null) {
                repoSet = new TreeSet<HgRepositoryLocation>();
            }
            repoSet.add(loc);
            projectRepos.put(project, repoSet);
        }

        return true;
    }

    private Map<IProject, SortedSet<HgRepositoryLocation>> getProjectRepos()
            throws IOException, HgException {
        if (!initDone) {
            initDone = true;
            loadProjectRepos();
        }
        return projectRepos;
    }

    private Set<IProject> loadProjectRepos() throws IOException, HgException {
        projectRepos.clear();
        Set<IProject> managedProjects = new HashSet<IProject>();
        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
                .getProjects();

        for (IProject project : projects) {
            if (!project.isAccessible()
                    || !MercurialUtilities.hgIsTeamProviderFor(project, false)) {
                continue;
            }

            // Load .hg/hgrc paths first; plugin settings will override these
            Map<String, String> hgrcRepos = HgPathsClient.getPaths(project);
            for (Map.Entry<String, String> entry : hgrcRepos.entrySet()) {
                try {
                    // if not existent, add to repository browser
                    HgRepositoryLocation loc = updateRepoLocation(
                            entry.getValue(),
                            entry.getKey(),
                            null, null);
                    internalAddRepoLocation(project, loc);
                } catch (HgException e) {
                    MercurialEclipsePlugin.logError(e);
                }
            }

            boolean createSrcRepository = false;
            try {
                String url = getDefaultProjectRepository(project);
                if (url != null) {
                    HgRepositoryLocation srcRepository = updateRepoLocation(
                            url, null, null, null);
                    internalAddRepoLocation(project, srcRepository);
                } else {
                    createSrcRepository = true;
                }
            } catch (Exception e) {
                // log the exception - but don't bother the user
                MercurialEclipsePlugin.logError(e);
            }
            File file = getProjectLocationFile(project);

            if (file.exists()) {
                String line;
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(new FileInputStream(file),
                                "UTF-8")); //$NON-NLS-1$

                try {
                    while ((line = reader.readLine()) != null) {
                        try {
                            HgRepositoryLocation loc = updateRepoLocation(
                                    line, null, null, null);
                            internalAddRepoLocation(project, loc);

                            if (createSrcRepository) {

                                setDefaultProjectRepository(project, loc);
                                createSrcRepository = false;
                            }
                        } catch (Exception e) {
                            // log exception, but don't bother the user with it.
                            MercurialEclipsePlugin.logError(e);
                        }

                    }
                } finally {
                    reader.close();
                }
            }
            managedProjects.add(project);
        }
        return managedProjects;
    }

    public void setDefaultProjectRepository(IProject project,
            HgRepositoryLocation loc) throws CoreException {
        if (loc != null) {
            project
                    .setPersistentProperty(
                            MercurialTeamProvider.QUALIFIED_NAME_PROJECT_SOURCE_REPOSITORY,
                            loc.getLocation());
        } else {
            project
                    .setPersistentProperty(
                            MercurialTeamProvider.QUALIFIED_NAME_PROJECT_SOURCE_REPOSITORY,
                            null);
        }

    }

    /**
     * Returns the default repository location for a project, if it is set.
     * @return may return null
     */
    public HgRepositoryLocation getDefaultProjectRepoLocation(IProject project) {
        try {
            return matchRepoLocation(getDefaultProjectRepository(project));
        } catch (CoreException e) {
            // log the exception - but don't bother the user
            MercurialEclipsePlugin.logError(e);
            return null;
        }
    }

    private String getDefaultProjectRepository(IProject project)
            throws CoreException {
        String url = project
                .getPersistentProperty(MercurialTeamProvider.QUALIFIED_NAME_PROJECT_SOURCE_REPOSITORY);
        return url;
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

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(file), "UTF-8")); //$NON-NLS-1$

            try {
                SortedSet<HgRepositoryLocation> repoSet = projectRepos
                        .get(project);
                if (repoSet != null) {
                    for (HgRepositoryLocation repo : repoSet) {
                        writer.write(repo.getSaveString());
                        writer.write('\n');
                    }
                }
            } finally {
                writer.close();
            }
        }
    }

    /**
     * Get a repo by its URL. If URL is unknown, returns a new location.
     * @return never returns null
     */
    public HgRepositoryLocation getRepoLocation(String url)
            throws HgException {
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
        return new HgRepositoryLocation(null, false, url, user, pass);
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
        return new HgRepositoryLocation(null, false, url, user, password);
    }

    /**
     * Simple search on existing repos.
     * @return may return null
     */
    private HgRepositoryLocation matchRepoLocation(String url) {
        for (HgRepositoryLocation loc : repos) {
            if (loc.getLocation().equals(url)) {
                return loc;
            }
        }
        return null;
    }

    /**
     * Gets a repo by its URL. If URL is unknown, returns a new location,
     * adding it to the global repositories cache. Will update stored
     * last user and password with the provided values.
     */
    public HgRepositoryLocation updateRepoLocation(String url,
            String logicalName, String user, String pass)
            throws HgException {
        HgRepositoryLocation loc = matchRepoLocation(url);

        if (loc == null) {
            // in some cases url may be a repository database line
            loc = new HgRepositoryLocation(logicalName, false, url, user, pass);
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
            HgRepositoryLocation updated = new HgRepositoryLocation(
                    myLogicalName, false, loc.getLocation(), myUser, myPass);
            repos.remove(updated);
            repos.add(updated);

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
    public HgRepositoryLocation fromProperties(Properties configuration)
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
            throw new HgException(Messages
                    .getString("HgRepositoryLocation.urlMustNotBeNull")); //$NON-NLS-1$
        }
        return updateRepoLocation(url, null, user, password);
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
    public HgRepositoryLocation createRepository(Properties configuration)
            throws HgException {
        // Create a new repository location
        HgRepositoryLocation location = fromProperties(configuration);
        addRepoLocation(location);
        return location;
    }

    public void removeRepositoryListener(IRepositoryListener repositoryListener) {
        REPOSITORY_RESOURCES_MANAGER
                .removeRepositoryListener(repositoryListener);

    }

    public void disposeRepository(HgRepositoryLocation hgRepositoryLocation)
            throws CoreException {
        Assert.isNotNull(hgRepositoryLocation);
        synchronized (projectRepos) {
            for (Iterator<IProject> iterator = projectRepos.keySet().iterator(); iterator
                    .hasNext();) {
                IProject project = iterator.next();
                if (project.isAccessible() && MercurialTeamProvider.isHgTeamProviderFor(project)) {
                    String url = getDefaultProjectRepository(project);
                    if (url != null
                            && url.equals(hgRepositoryLocation.getLocation())) {
                        setDefaultProjectRepository(project, null);
                    }
                    SortedSet<HgRepositoryLocation> pRepos = projectRepos
                            .get(project);
                    if (pRepos != null) {
                        for (HgRepositoryLocation repo : pRepos) {
                            if (repo.equals(hgRepositoryLocation)) {
                                pRepos.remove(repo);
                                break;
                            }
                        }
                    }
                }
            }
        }

        synchronized (repos) {
            for (HgRepositoryLocation loc : repos) {
                if (loc.equals(hgRepositoryLocation)) {
                    repos.remove(loc);
                    break;
                }
            }
            REPOSITORY_RESOURCES_MANAGER
                    .repositoryRemoved(hgRepositoryLocation);
        }

    }

}
