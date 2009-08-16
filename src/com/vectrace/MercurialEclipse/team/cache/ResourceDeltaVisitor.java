/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Andrei	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team.cache;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.Team;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeWorkspaceJob;
import com.vectrace.MercurialEclipse.commands.HgClients;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.operations.InitOperation;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

final class ResourceDeltaVisitor implements IResourceDeltaVisitor {

    private final Map<IProject, Set<IResource>> removed;
    private final Map<IProject, Set<IResource>> changed;
    private final Map<IProject, Set<IResource>> added;
    private final boolean completeStatus;
    private final boolean autoShare;
    private final MercurialStatusCache cache;

    ResourceDeltaVisitor(Map<IProject, Set<IResource>> removed, Map<IProject, Set<IResource>> changed,
            Map<IProject, Set<IResource>> added) {
        this.removed = removed;
        this.changed = changed;
        this.added = added;
        cache = MercurialStatusCache.getInstance();
        completeStatus = Boolean
        .valueOf(
                HgClients.getPreference(MercurialPreferenceConstants.RESOURCE_DECORATOR_COMPLETE_STATUS,
                "false")).booleanValue(); //$NON-NLS-1$
        autoShare = Boolean.valueOf(
                HgClients.getPreference(MercurialPreferenceConstants.PREF_AUTO_SHARE_PROJECTS, "false"))
                .booleanValue();
    }

    private IResource getResource(IResource res) {
        IResource myRes = res;
        if (completeStatus) {
            myRes = res.getProject();
        }
        return myRes;
    }

    public boolean visit(IResourceDelta delta) throws CoreException {
        IResource res = delta.getResource();
        // System.out.println("[ME-RV] Flags: "
        // + Integer.toHexString(delta.getFlags()));
        // System.out.println("[ME-RV] Kind: "
        // + Integer.toHexString(delta.getKind()));
        // System.out.println("[ME-RV] Resource: " + res.getFullPath());
        if(!res.isAccessible()){
            return false;
        }
        if (res.getType() == IResource.ROOT) {
            return true;
        }
        final IProject project = res.getProject();

        // handle projects that contain a mercurial repository
        if (autoShare && delta.getFlags() == IResourceDelta.OPEN
                && RepositoryProvider.getProvider(project) == null) {
            autoshareProject(project);
        }

        if (!Team.isIgnoredHint(res) && (RepositoryProvider.getProvider(project, MercurialTeamProvider.ID) != null)) {
            if (res.getType() == IResource.FILE && !res.isTeamPrivateMember() && !res.isDerived()) {
                int flag = delta.getFlags() & MercurialStatusCache.INTERESTING_CHANGES;
                IResource resource = getResource(res);
                Set<IResource> addSet = added.get(project);
                if (addSet == null) {
                    addSet = new HashSet<IResource>();
                }

                Set<IResource> removeSet = removed.get(project);
                if (removeSet == null) {
                    removeSet = new HashSet<IResource>();
                }

                Set<IResource> changeSet = changed.get(project);
                if (changeSet == null) {
                    changeSet = new HashSet<IResource>();
                }
                // System.out.println("[ME-RV] " + res.getFullPath()
                // + " interesting? Result: "
                // + Integer.toHexString(flag));
                switch (delta.getKind()) {
                case IResourceDelta.ADDED:
                    addSet.add(resource);
                    added.put(project, addSet);
                    break;
                case IResourceDelta.CHANGED:
                    if (flag != 0 && cache.isSupervised(res)) {
                        changeSet.add(resource);
                        changed.put(project, changeSet);
                    }
                    break;
                case IResourceDelta.REMOVED:
                    if (cache.isSupervised(res)) {
                        removeSet.add(resource);
                        removed.put(project, removeSet);
                    }
                    break;
                }
            }
            // System.out
            // .println("[ME-RV] Descending to next level (returning with true)");
            return true;
        }
        // System.out.println("[ME-RV] Not descending (returning with false)");
        return false;
    }

    private void autoshareProject(final IProject project) throws HgException {
        HgRoot hgRoot;
        try {
            hgRoot = MercurialTeamProvider.getHgRoot(project);
            MercurialEclipsePlugin.logInfo("Autosharing " + project.getName()
                    + ". Detected repository location: " + hgRoot.getAbsolutePath(), null);
        } catch (HgException e) {
            hgRoot = null;
            MercurialEclipsePlugin.logInfo("Autosharing: " + e.getLocalizedMessage(), e);
        }
        final HgRoot root = hgRoot;
        if (root != null && root.length() > 0) {
            final IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

            try {
                new SafeWorkspaceJob(NLS.bind(Messages.mercurialStatusCache_autoshare, project.getName())) {

                    @Override
                    protected IStatus runSafe(IProgressMonitor monitor) {
                        try {
                            new InitOperation(activeWorkbenchWindow, project, root, root.getAbsolutePath())
                            .run(monitor);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        return super.runSafe(monitor);
                    }
                }.schedule();
            } catch (Exception e) {
                throw new HgException(e.getLocalizedMessage(), e);
            }
        }
    }
}