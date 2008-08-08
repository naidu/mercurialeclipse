/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.operations;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.team.core.RepositoryProvider;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.commands.HgInitClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.cache.RefreshStatusJob;

public class InitOperation extends HgOperation {

    private IProject project;
    private String hgPath;
    private String foundHgPath;

    /**
     * 
     */
    public InitOperation(IRunnableContext ctx, IProject project,
            String foundHgPath, String hgPath) {
        super(ctx);
        this.hgPath = hgPath;
        this.project = project;
        this.foundHgPath = foundHgPath;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.vectrace.MercurialEclipse.actions.HgOperation#getActionDescription
     * ()
     */
    @Override
    protected String getActionDescription() {
        return "Creating repository";
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.vectrace.MercurialEclipse.actions.HgOperation#run(org.eclipse
     * .core.runtime.IProgressMonitor)
     */
    @Override
    public void run(IProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException {
        try {
            monitor.beginTask("Share: ", 3);
            if ((this.foundHgPath == null)
                    || (!this.foundHgPath.equals(hgPath))) {
                monitor
                        .subTask("Calling Mercurial...");
                HgInitClient.init(project, hgPath);
                monitor.worked(1);
            }
            monitor.subTask("Mapping " + project.getName()
                    + " to team provider...");
            RepositoryProvider.map(project, MercurialTeamProvider.class
                    .getName());
            monitor.worked(1);
            project.touch(monitor);
            monitor
                    .subTask("Scheduling status refresh...");
            new RefreshStatusJob("Refresh project " + project
                    + " after mapping team provider...", project)
                    .schedule();
            monitor.worked(1);
        } catch (HgException e) {
            MercurialEclipsePlugin.logError(e);
            throw new InvocationTargetException(e);
        } catch (CoreException e) {
            MercurialEclipsePlugin.logError(e);
            throw new InvocationTargetException(e);
        }
        monitor.done();
    }

}