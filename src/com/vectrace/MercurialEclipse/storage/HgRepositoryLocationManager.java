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
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
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
import org.eclipse.team.core.RepositoryProvider;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.repository.IRepositoryListener;
import com.vectrace.MercurialEclipse.repository.RepositoryResourcesManager;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

/**
 * A manager for all Mercurial repository locations.
 * 
 * 
 */
public class HgRepositoryLocationManager {

    private static final RepositoryResourcesManager REPOSITORY_RESOURCES_MANAGER = RepositoryResourcesManager
            .getInstance();

    final static private String REPO_LOCACTION_FILE = "repositories.txt";

    private Set<HgRepositoryLocation> repos = new TreeSet<HgRepositoryLocation>();
    private Map<IProject, SortedSet<HgRepositoryLocation>> projectRepos = null;

    /**
     * Return a <code>File</code> object representing the location file. The
     * file may or may not exist and must be checked before use.
     */
    private File getLocationFile() {
        return MercurialEclipsePlugin.getDefault().getStateLocation().append(
                REPO_LOCACTION_FILE).toFile();
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
                    new FileInputStream(file), "UTF-8"));

            try {
                while ((line = reader.readLine()) != null) {
                    addRepoLocation(new HgRepositoryLocation(line, null, null));
                }
            } catch (URISyntaxException e) {
                // we don't want to load it - it will be cleaned when saving
                MercurialEclipsePlugin.logError(e);
            } finally {
                reader.close();
            }

        }
        loadProjectRepos();

    }

    /**
     * Flush all repository locations out to a file in the plug-in's default
     * area.
     */
    public void stop() throws IOException {
        File file = getLocationFile();

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file), "UTF-8"));

        try {
            for (HgRepositoryLocation repo : repos) {
                writer.write(repo.getUrl());
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
        if (loc == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(loc);
    }

    /**
     * Add a repository location to the database.
     */
    public boolean addRepoLocation(HgRepositoryLocation loc) {
        synchronized (repos) {
            repos.add(loc);
            REPOSITORY_RESOURCES_MANAGER.repositoryAdded(loc);
            return true;
        }
    }

    /**
     * Add a repository location to the database.
     * 
     * @throws HgException
     */
    public boolean addRepoLocation(IProject project, HgRepositoryLocation loc)
            throws HgException {
        if (loc == null) {
            return false;
        }
        synchronized (projectRepos) {
            if (projectRepos == null) {
                try {
                    projectRepos = getProjectRepos();
                } catch (IOException e) {
                    MercurialEclipsePlugin.logError(e);
                }
            }
            SortedSet<HgRepositoryLocation> repoSet = projectRepos.get(project);
            if (repoSet == null) {
                repoSet = new TreeSet<HgRepositoryLocation>();
            }
            repoSet.add(loc);
            projectRepos.put(project, repoSet);
        }
        addRepoLocation(loc);
        return true;
    }

    private Map<IProject, SortedSet<HgRepositoryLocation>> getProjectRepos()
            throws IOException, HgException {
        if (projectRepos == null) {
            loadProjectRepos();
        }
        return projectRepos;
    }

    private void loadProjectRepos() throws IOException, HgException {
        projectRepos = new HashMap<IProject, SortedSet<HgRepositoryLocation>>();
        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
                .getProjects();

        for (IProject project : projects) {
            if (!project.isOpen()) {
                continue;
            }
            boolean createSrcRepository = false;
            try {
                String url = getDefaultProjectRepository(project);
                if (url != null) {
                    HgRepositoryLocation srcRepository = new HgRepositoryLocation(
                            url, null, null);
                    addRepoLocation(project, srcRepository);
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
                                "UTF-8"));

                try {
                    while ((line = reader.readLine()) != null) {
                        try {
                            HgRepositoryLocation loc = new HgRepositoryLocation(
                                    line, null, null);
                            addRepoLocation(project, loc);

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
        }
    }

    /**
     * @param project
     * @param loc
     * @throws CoreException
     */
    public void setDefaultProjectRepository(IProject project,
            HgRepositoryLocation loc) throws CoreException {
        if (loc != null) {
            project
                    .setPersistentProperty(
                            MercurialTeamProvider.QUALIFIED_NAME_PROJECT_SOURCE_REPOSITORY,
                            loc.getUrl());
        } else {
            project
                    .setPersistentProperty(
                            MercurialTeamProvider.QUALIFIED_NAME_PROJECT_SOURCE_REPOSITORY,
                            null);
        }

    }

    /**
     * @param project
     * @return
     * @throws CoreException
     */
    public String getDefaultProjectRepository(IProject project)
            throws CoreException {
        String url = project
                .getPersistentProperty(MercurialTeamProvider.QUALIFIED_NAME_PROJECT_SOURCE_REPOSITORY);
        return url;
    }

    /**
     * @param project
     * @return
     */
    private File getProjectLocationFile(IProject project) {
        File file = MercurialEclipsePlugin.getDefault().getStateLocation()
                .append(REPO_LOCACTION_FILE + "_" + project.getName()).toFile();
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
                SortedSet<HgRepositoryLocation> repoSet = projectRepos
                        .get(project);
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

    /**
     * Gets a repo by its URL
     * 
     * @param url
     * @return
     */
    public HgRepositoryLocation getRepoLocation(String url) {
        if (repos != null) {
            for (HgRepositoryLocation loc : repos) {
                if (loc.getUrl().equals(url)) {
                    return loc;
                }
            }
        }
        return null;
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
     * resides rootUrl The root url of the subversion repository (optional)
     * 
     * The created instance is not known by the provider and it's user
     * information is not cached. The purpose of the created location is to
     * allow connection validation before adding the location to the provider.
     * 
     * This method will throw a HgException if the location for the given
     * configuration already exists.
     */
    public HgRepositoryLocation createRepository(Properties configuration)
            throws HgException {
        // Create a new repository location
        HgRepositoryLocation location;
        try {
            location = HgRepositoryLocation.fromProperties(configuration);
        } catch (URISyntaxException e) {
            MercurialEclipsePlugin.logError(e);
            throw new HgException("Couldn't create repository location.", e);
        }

        // Check the cache for an equivalent instance and if there is one, throw
        // an exception
        HgRepositoryLocation existingLocation = getRepoLocation(location
                .getUrl());
        if (existingLocation != null) {
            throw new HgException("Repository location already known.");
        }
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
                if (null != RepositoryProvider.getProvider(project,
                        MercurialTeamProvider.ID)
                        && project.isAccessible()) {
                    String url = getDefaultProjectRepository(project);
                    if (url != null
                            && url.equals(hgRepositoryLocation.getUrl())) {
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
