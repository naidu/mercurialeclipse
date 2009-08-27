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
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

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

    public boolean visit(IResourceDelta delta) throws CoreException {
        IResource res = delta.getResource();
        if (res.getType() == IResource.ROOT) {
            return true;
        }
        final IProject project = res.getProject();

        // handle projects that contain a mercurial repository
        if (autoShare && delta.getFlags() == IResourceDelta.OPEN && project.isAccessible()
                && RepositoryProvider.getProvider(project) == null) {
            autoshareProject(project);
            // stop tracking changes: after auto-share is completed, we will do a full refresh anyway
            return false;
        }

        if (Team.isIgnoredHint(res) || (RepositoryProvider.getProvider(project, MercurialTeamProvider.ID) == null)
                || res.isTeamPrivateMember() || res.isDerived()) {
            return false;
        }
        // System.out.println("Observing change on: " + res);

        // NB: the resource may not exist at this point (deleted/moved)
        // so any access to the IResource's API should be checked against null
        if (res.getType() == IResource.FILE) {
            IResource resource = completeStatus? project : res;
            switch (delta.getKind()) {
            case IResourceDelta.ADDED:
                addResource(added, project, resource);
                // System.out.println("\t ADDED: " + resource);
                break;
            case IResourceDelta.CHANGED:
                if (hasChangedBits(delta)
                        && cache.isSupervised(project, ResourceUtils.getPath(res))) {
                    addResource(changed, project, resource);
                    // System.out.println("\t CHANGED: " + resource);
                }
                break;
            case IResourceDelta.REMOVED:
                if (cache.isSupervised(project, ResourceUtils.getPath(res))) {
                    addResource(removed, project, resource);
                    // System.out.println("\t REMOVED: " + resource);
                }
                break;
            }
        }
        return true;
    }


    private void addResource(Map<IProject, Set<IResource>> map, IProject project, IResource res){
        Set<IResource> set = map.get(project);
        if(set == null) {
            set = new HashSet<IResource>();
            map.put(project, set);
        }
        set.add(res);
    }

    private boolean hasChangedBits(IResourceDelta delta){
        return (delta.getFlags() & MercurialStatusCache.MASK_CHANGED) != 0;
    }

    private void autoshareProject(final IProject project) {
        final HgRoot hgRoot;
        try {
            hgRoot = MercurialTeamProvider.getHgRoot(project);
            MercurialEclipsePlugin.logInfo("Autosharing " + project.getName()
                    + ". Detected repository location: " + hgRoot.getAbsolutePath(), null);
        } catch (HgException e) {
            MercurialEclipsePlugin.logInfo("Autosharing failed: " + e.getLocalizedMessage(), e);
            return;
        }
        final IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

        new SafeWorkspaceJob(NLS.bind(Messages.mercurialStatusCache_autoshare, project.getName())) {
            @Override
            protected IStatus runSafe(IProgressMonitor monitor) {
                try {
                    new InitOperation(activeWorkbenchWindow, project, hgRoot, hgRoot.getAbsolutePath())
                    .run(monitor);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return super.runSafe(monitor);
            }
        }.schedule();
    }
}