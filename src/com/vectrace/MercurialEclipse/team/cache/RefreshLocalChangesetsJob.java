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

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeWorkspaceJob;
import com.vectrace.MercurialEclipse.exception.HgException;

/**
 * @author bastian
 *
 */
public class RefreshLocalChangesetsJob extends SafeWorkspaceJob {
    /**
     * 
     */
    private final IProject project;

    /**
     * @param name
     * @param project
     */
    public RefreshLocalChangesetsJob(String name, IProject project) {
        super(name);
        this.project = project;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vectrace.MercurialEclipse.SafeWorkspaceJob#runSafe(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    protected IStatus runSafe(IProgressMonitor monitor) {
        try {
            LocalChangesetCache.getInstance()
                    .refreshAllLocalRevisions(project, true);
        } catch (HgException e) {      
            MercurialEclipsePlugin.logError(e);
        }
        return super.runSafe(monitor);
    }
}