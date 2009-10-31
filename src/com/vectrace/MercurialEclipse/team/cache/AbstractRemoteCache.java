/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Andrei   implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team.cache;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.preference.IPreferenceStore;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgIncomingClient;
import com.vectrace.MercurialEclipse.commands.HgOutgoingClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * A base class for remote caches (caching changesets which are either not present
 * locally but existing on the server, or existing locally, but not present on the server).
 * <p>
 * The cache keeps the state automatically (and fetches the data on client request only), to avoid
 * unneeded client-server communication.
 * <p>
 * There is no guarantee that the data in the cache is up-to-date with the server. To get the
 * latest data, clients have explicitely refresh or clean the cache before using it.
 * <p>
 * The cache has empty ("invalid" state) before the first client request and automatically
 * retrieves the data on first client request. So it becames "valid" state and does not refresh the
 * data until some operation "clears" or explicitely requests a "refresh" of the cache. After the
 * "clear" operation the cache is going to the initial "invalid" state again. After "clear" and
 * "refresh", a notification is sent to the observing clients.
 * <p>
 * <b>Implementation note 1</b> this cache <b>automatically</b> keeps the "valid" state for given
 * project/repository pair. Before each "get" request the cache validates itself. If the cached
 * value is NULL, then the cache state is invalid, and new data is fetched. If the cached value is
 * an object (even empty set), then the cache is "valid" (there is simply no data on the server).
 * <p>
 * <b>Implementation note 2</b> the cache sends different notifications depending on what kind of
 * "state change" happened. After "clear", a set with only one "project" object is sent. After
 * "refresh", a set with all changed elements is sent, which may also include a project.
 *
 * @author bastian
 * @author Andrei Loskutov
 * @author <a href="mailto:adam.berkes@intland.com">Adam Berkes</a>
 */
public abstract class AbstractRemoteCache extends AbstractCache {

    /**
     * The Map has the following structure: RepositoryLocation -> IResource ->
     * Changeset-Set
     */
    protected final Map<HgRepositoryLocation, Map<IPath, SortedSet<ChangeSet>>> repoChangeSets;

    protected final Direction direction;

    /**
     * @param direction non null
     */
    public AbstractRemoteCache(Direction direction) {
        this.direction = direction;
        repoChangeSets = new HashMap<HgRepositoryLocation, Map<IPath, SortedSet<ChangeSet>>>();
    }

    /**
     * does nothing, clients has to override and update preferences
     */
    @Override
    protected void configureFromPreferences(IPreferenceStore store) {
        // does nothing
    }

    public void clear(HgRepositoryLocation repo) {
        synchronized (repoChangeSets) {
            repoChangeSets.remove(repo);
        }
    }

    /**
     * @param notify true to send a notification if the cache state changes after this operation,
     * false to supress the event notification
     */
    public void clear(HgRepositoryLocation repo, IProject project, boolean notify) {
        synchronized (repoChangeSets) {
            Map<IPath, SortedSet<ChangeSet>> map = repoChangeSets.get(repo);
            if(map != null){
                map.remove(project.getLocation());
            }
        }
        if(notify) {
            notifyChanged(project, false);
        }
    }

    @Override
    protected void clearProjectCache(IProject project) {
        Set<HgRepositoryLocation> repos = MercurialEclipsePlugin.getRepoManager()
                .getAllProjectRepoLocations(project);
        for (HgRepositoryLocation repo : repos) {
            clear(repo, project, false);
        }
    }

    /**
     * Gets all (in or out) changesets of the given location for the given
     * IResource.
     *
     * @param branch name of branch (default or "" for unnamed) or null if branch unaware
     * @return never null
     */
    public SortedSet<ChangeSet> getChangeSets(IResource resource,
            HgRepositoryLocation repository, String branch) throws HgException {
        Map<IPath, SortedSet<ChangeSet>> repoMap;
        synchronized (repoChangeSets){
            repoMap = repoChangeSets.get(repository);
            IPath location = resource.getLocation();
            if (repoMap == null || ((resource instanceof IProject) && repoMap.get(location) == null)) {
                // lazy loading: refresh cache on demand only.
                refreshChangeSets(resource.getProject(), repository, branch);
                repoMap = repoChangeSets.get(repository);
            }
            if (repoMap != null) {
                SortedSet<ChangeSet> revisions = repoMap.get(location);
                if (revisions != null) {
                    return Collections.unmodifiableSortedSet(revisions);
                }
            }
        }
        return new TreeSet<ChangeSet>();
    }

    /**
     * Gets all resources that are changed in (in or out) changesets of given
     * repository, even resources not known in local workspace.
     *
     * @return never null
     */
    public Set<IResource> getMembers(IResource resource,
            HgRepositoryLocation repository, String branch) throws HgException {
        Map<IPath, SortedSet<ChangeSet>> changeSets;
        synchronized (repoChangeSets){
            changeSets = getMap(resource, repository, branch);
        }
        return getMembers(resource, changeSets);
    }

    /**
     * @return never null
     */
    private static Set<IResource> getMembers(IResource resource,
            Map<IPath, SortedSet<ChangeSet>> changeSets) {
        Set<IResource> members = new HashSet<IResource>();
        if (changeSets == null) {
            return members;
        }
        IWorkspaceRoot root = resource.getWorkspace().getRoot();
        IPath location = ResourceUtils.getPath(resource);
        for (IPath path : changeSets.keySet()) {
            IFile member = root.getFileForLocation(path);
            if (member != null) {
                IPath memberLocation = ResourceUtils.getPath(member);
                if (location.isPrefixOf(memberLocation)) {
                    members.add(member);
                }
            }
        }
        return members;
    }

    private Map<IPath, SortedSet<ChangeSet>> getMap(IResource resource, HgRepositoryLocation repository, String branch)
            throws HgException {
        // make sure data is there: will refresh (in or out) changesets if needed
        getChangeSets(resource, repository, branch);
        return repoChangeSets.get(repository);
    }


    /**
     * Gets all (in or out) changesets by querying Mercurial and adds them to the caches.
     */
    private void refreshChangeSets(IProject project, HgRepositoryLocation repository, String branch) throws HgException {
        Assert.isNotNull(project);

        // check if mercurial is team provider and if we're working on an open project
        if (project.isAccessible() && MercurialTeamProvider.isHgTeamProviderFor(project)) {

            // lock the cache till update is complete
            synchronized (repoChangeSets){
                addResourcesToCache(project, repository, branch);
            }
            notifyChanged(project, true);
        }
    }

    private void addResourcesToCache(IProject project, HgRepositoryLocation repository, String branch)
            throws HgException {

        if(debug) {
            System.out.println("\n!fetch " + direction + " for " + project);
        }

        // clear cache of old members
        final Map<IPath, SortedSet<ChangeSet>> removeMap = repoChangeSets.get(repository);

        if (removeMap != null) {
            removeMap.clear();
            repoChangeSets.remove(repository);
        }

        // get changesets from hg
        Map<IPath, SortedSet<ChangeSet>> resources;
        if (direction == Direction.OUTGOING) {
            resources = HgOutgoingClient.getOutgoing(project, repository, branch);
        } else {
            resources = HgIncomingClient.getHgIncoming(project, repository, branch);
        }

        HashMap<IPath, SortedSet<ChangeSet>> map = new HashMap<IPath, SortedSet<ChangeSet>>();
        repoChangeSets.put(repository, map);
        IPath projectPath = project.getLocation();
        map.put(projectPath, new TreeSet<ChangeSet>());

        // add them to cache(s)
        for (Map.Entry<IPath, SortedSet<ChangeSet>> mapEntry : resources.entrySet()) {
            IPath path = mapEntry.getKey();
            SortedSet<ChangeSet> changes = mapEntry.getValue();
            if (changes != null && changes.size() > 0) {
                map.put(path, changes);
            }
        }
    }

    /**
     * Get newest revision of resource regardless of branch
     */
    public ChangeSet getNewestChangeSet(IResource resource,
            HgRepositoryLocation repository) throws HgException {
        return getNewestChangeSet(resource, repository, null);
    }

    /**
     * Get newest revision of resource on given branch
     * @param resource Eclipse resource (e.g. a file) to find latest changeset for
     * @param branch name of branch (default or "" for unnamed) or null if branch unaware
     */
    public ChangeSet getNewestChangeSet(IResource resource,
            HgRepositoryLocation repository, String branch) throws HgException {

        if (MercurialStatusCache.getInstance().isSupervised(resource) || !resource.exists()) {
            synchronized (repoChangeSets){
                Map<IPath, SortedSet<ChangeSet>> repoMap = getMap(resource, repository, branch);

                if (repoMap != null) {
                    SortedSet<ChangeSet> revisions = repoMap.get(resource.getLocation());
                    if (revisions != null && revisions.size() > 0) {
                        return revisions.last();
                    }
                }
            }
        }
        return null;
    }
}
