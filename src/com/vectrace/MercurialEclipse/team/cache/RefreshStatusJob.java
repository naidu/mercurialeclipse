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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.core.TeamException;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeWorkspaceJob;

/**
 * @author bastian
 *
 */
public class RefreshStatusJob extends SafeWorkspaceJob {

    private static final MercurialStatusCache mercurialStatusCache = MercurialStatusCache
            .getInstance();
    private IProject project;

    public RefreshStatusJob(String name) {
        super(name);
    }

    public RefreshStatusJob(String name, IProject project) {
        super(name);
        this.project = project;
    }

    @Override
    protected IStatus runSafe(IProgressMonitor monitor) {
        try {
            monitor.beginTask(Messages.refreshStatusJob_OptainingMercurialStatusInformation, 5);
            mercurialStatusCache.refreshStatus(project, monitor);
        } catch (TeamException e) {
            MercurialEclipsePlugin.logError(e);
        }
        return super.runSafe(monitor);
    }
}