/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.model;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.core.RepositoryProvider;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgIdentClient;
import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.ResourceProperties;

public class FlagManager implements IResourceChangeListener {

    private Map<IProject, FlaggedProject> projects;
    private List<FlagManagerListener> listeners;

    public FlagManager() {
        projects = new HashMap<IProject, FlaggedProject>();
        listeners = new ArrayList<FlagManagerListener>();
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
    }

    public void dispose() {
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
    }

    public void addListener(FlagManagerListener listener) {
        listeners.add(listener);
    }

    public void removeListener(FlagManagerListener listener) {
        listeners.remove(listener);
    }

    public FlaggedProject getFlaggedProject(IProject project) throws CoreException, HgException {
        if (project == null || RepositoryProvider.getProvider(project, MercurialTeamProvider.ID) == null) {
            return null;
        }
        if (!projects.containsKey(project)) {
            refresh(project);
        }
        return projects.get(project);
    }

    // FIXME should be replaced byrefreshHgStatus and refreshMergeStatus
    // FIXME need synchronization
    public FlaggedProject refresh(IProject project) throws CoreException, HgException {
        // status
        String statusOutput = HgStatusClient.getStatus(project);
        IContainer workspace = project.getParent();
        Map<IResource, FlaggedResource> statusMap = new HashMap<IResource, FlaggedResource>();
        Scanner scanner = new Scanner(statusOutput);
        while (scanner.hasNext()) {
            String status = scanner.next();
            String localName = scanner.nextLine();
            IResource member = project.getFile(localName.trim());

            BitSet bitSet = new BitSet();
            bitSet.set(getBitIndex(status.charAt(0)));
            statusMap.put(member, new FlaggedResource(member, bitSet));

            // ancestors
            for (IResource parent = member.getParent(); parent != workspace; parent = parent
                    .getParent()) {
                FlaggedResource parentFlagged = statusMap.get(parent);
                if (parentFlagged == null) {
                    statusMap.put(parent, new FlaggedResource(parent, bitSet));
                } else {
                    bitSet = parentFlagged.combineStatus(bitSet);
                }
            }
        }
        // version
        String version = HgIdentClient.getCurrentRevision(project);
        // merge status
        if (project.getPersistentProperty(ResourceProperties.MERGING) != null) {
            // TODO merge status
        }
        // populate map
        FlaggedProject fp = new FlaggedProject(project, statusMap, version);
        projects.put(project, fp);
        // notification
        for (FlagManagerListener listener : listeners) {
            listener.onRefresh(project);
        }
        return fp;
    }

    private final int getBitIndex(char status) {
        switch (status) {
            case '!':
                return FlaggedResource.BIT_DELETED;
            case 'R':
                return FlaggedResource.BIT_REMOVED;
            case 'I':
                return FlaggedResource.BIT_IGNORE;
            case 'C':
                return FlaggedResource.BIT_CLEAN;
            case '?':
                return FlaggedResource.BIT_UNKNOWN;
            case 'A':
                return FlaggedResource.BIT_ADDED;
            case 'M':
                return FlaggedResource.BIT_MODIFIED;
            default:
                MercurialEclipsePlugin.logWarning("Unknown status: '" + status + "'", null);
                return FlaggedResource.BIT_IMPOSSIBLE;
        }
    }

    public void resourceChanged(IResourceChangeEvent event) {
        Set<IProject> changedProjects = new HashSet<IProject>();
        if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
            IResourceDelta[] children = event.getDelta().getAffectedChildren();
            for (IResourceDelta delta : children) {
                IProject project = delta.getResource().getProject();                
                if (null != RepositoryProvider.getProvider(project, MercurialTeamProvider.ID)) {
                    changedProjects.add(project);
                }
            }
        }
        for (IProject project : changedProjects) {
            try {
                refresh(project);
            } catch (Exception e) {
                MercurialEclipsePlugin.logError(e);
            }
        }
    }

}
