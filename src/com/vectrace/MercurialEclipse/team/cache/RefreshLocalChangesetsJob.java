/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch           - init
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team.cache;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeWorkspaceJob;
import com.vectrace.MercurialEclipse.exception.HgException;

/**
 * @author bastian
 *
 */
public class RefreshLocalChangesetsJob extends SafeWorkspaceJob {

    private final IProject project;

    public RefreshLocalChangesetsJob(String name, IProject project) {
        super(name);
        this.project = project;
    }

    public RefreshLocalChangesetsJob(IResource resource) {
        super("Refreshing local changesets.");
        this.project = resource.getProject();
    }

    @Override
    protected IStatus runSafe(IProgressMonitor monitor) {
        try {
            LocalChangesetCache.getInstance().refreshAllLocalRevisions(project, true);
            return super.runSafe(monitor);
        } catch (HgException e) {
            MercurialEclipsePlugin.logError(e);
            return e.getStatus();
        }

    }
}