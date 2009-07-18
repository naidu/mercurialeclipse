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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.team.core.RepositoryProvider;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.wizards.Messages;

/**
 * @author bastian
 * 
 */
public class CreateProjectOperation extends HgOperation {

    private File projectFile;
    private HgRepositoryLocation repo;
    private boolean readProjectFile;
    private String projectName;
    private IProject project;
    private File projectDirectory;

    /**
     * @param context
     */
    public CreateProjectOperation(IRunnableContext context,
            File projectDirectory, File projectFile, HgRepositoryLocation repo,
            boolean readProjectFile, String projectName) {
        super(context);
        this.projectFile = projectFile;
        this.repo = repo;
        this.readProjectFile = readProjectFile;
        this.projectName = projectName;
        this.projectDirectory = projectDirectory;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vectrace.MercurialEclipse.actions.HgOperation#getActionDescription()
     */
    @Override
    protected String getActionDescription() {
        return Messages.getString("CreateProjectOperation.taskDescription"); //$NON-NLS-1$
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vectrace.MercurialEclipse.actions.HgOperation#run(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void run(IProgressMonitor monitor) throws InvocationTargetException,
            InterruptedException {

        try {
            createProject(monitor);
            monitor.done();
        } catch (HgException e1) {
            throw new InvocationTargetException(e1);
        }
    }

    private void createProject(IProgressMonitor monitor) throws HgException {
        InputStream in = null;
        try {
            if (monitor.isCanceled()) {
                return;
            }
            IWorkspace workspace = ResourcesPlugin.getWorkspace();
            IProjectDescription projectDesc = null;
            monitor.beginTask(Messages
                    .getString("CreateProjectOperation.beginTask"), 15); //$NON-NLS-1$
            if (readProjectFile && projectFile != null) {
                // load the project description from the retrieved metafile
                monitor
                        .subTask(Messages
                                .getString("CreateProjectOperation.subTaskReadingProjectFile")); //$NON-NLS-1$
                in = new FileInputStream(projectFile);
                projectDesc = workspace.loadProjectDescription(in);
                monitor.worked(1);
            } else {
                // create project description
                monitor
                        .subTask(Messages
                                .getString("CreateProjectOperation.subTaskCreatingProjectFile")); //$NON-NLS-1$
                projectDesc = workspace.newProjectDescription(projectName);
                projectDesc
                        .setComment(Messages
                                .getString("CloneRepoWizard.description.comment") + repo); //$NON-NLS-1$                
                monitor.worked(1);
            }

            // set location in file system (parentdir/projectname)
            if (!workspace.getRoot().getLocation().toFile().getAbsolutePath()
                    .equals(projectDirectory.getParentFile().getAbsolutePath())) {
                projectDesc.setLocation(new Path(projectDirectory.getAbsolutePath()));
            } else {
                projectDesc.setLocation(null);                
            }

            // now get resource handle and create & open project
            IProject p = workspace.getRoot().getProject(projectDesc.getName());
            p.create(projectDesc, monitor);
            p.open(monitor);

            // associate new project with MercurialEclipse
            registerWithTeamProvider(p, monitor);
            this.project = p;
        } catch (Exception e) {
            throw new HgException(e.getMessage(), e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                // ignore : we cannot read the file, so it's like it is not
                // there
            }
        }
    }

    /**
     * @param monitor
     * @throws InvocationTargetException
     * @throws HgException
     */
    private void registerWithTeamProvider(IProject p, IProgressMonitor monitor)
            throws HgException {
        try {
            // Register the project with Team. This will bring all the
            // files
            // that
            // we cloned into the project.
            monitor
                    .subTask(Messages
                            .getString("CloneRepoWizard.subTask.registeringProject1") + p.getName() //$NON-NLS-1$
                            + Messages
                                    .getString("CloneRepoWizard.subTaskRegisteringProject2")); //$NON-NLS-1$
            RepositoryProvider.map(p, MercurialTeamProvider.class.getName());
            monitor.worked(1);

            // It appears good. Stash the repo location.
            monitor
                    .subTask(Messages
                            .getString("CloneRepoWizard.subTask.addingRepository.1") + repo //$NON-NLS-1$
                            + Messages
                                    .getString("CloneRepoWizard.subTask.addingRepository.2")); //$NON-NLS-1$
            MercurialEclipsePlugin.getRepoManager().addRepoLocation(p, repo);
            monitor.worked(1);
        } catch (Exception e) {
            throw new HgException(e.getMessage(), e);
        }
    }

    /**
     * @return the project
     */
    public IProject getProject() {
        return project;
    }

}
