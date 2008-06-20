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
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.operation.IRunnableContext;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.commands.HgCloneClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgFolder;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.wizards.Messages;

public class CloneOperation extends HgOperation {

    private String parentDirectory;
    private HgRepositoryLocation repo;
    private boolean noUpdate;
    private boolean pull;
    private boolean uncompressed;
    private boolean timeout;
    private String rev;
    private String cloneName;
    private List<File> projectFiles;
    private Path projectLocation;
    private boolean searchForProjectFiles;

    /**
     * @param name
     */
    public CloneOperation(IRunnableContext context, String parentDirectory,
            HgRepositoryLocation repo, boolean noUpdate, boolean pull,
            boolean uncompressed, boolean timeout, String rev,
            String cloneName, boolean searchForProjectFiles) {
        super(context);
        this.parentDirectory = parentDirectory;
        this.repo = repo;
        this.noUpdate = noUpdate;
        this.pull = pull;
        this.uncompressed = uncompressed;
        this.timeout = timeout;
        this.rev = rev;
        this.cloneName = cloneName;
        this.searchForProjectFiles = searchForProjectFiles;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
     */
    @SuppressWarnings("restriction")//$NON-NLS-1$
    @Override
    public void run(IProgressMonitor m) throws InvocationTargetException,
            InterruptedException {

        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();

        m.beginTask(Messages.getString("CloneRepoWizard.operation.name"), 50); //$NON-NLS-1$

        // set defaults
        if (this.parentDirectory == null || parentDirectory.length() == 0) {
            parentDirectory = workspaceRoot.getLocation().toOSString();
        }

        m
                .subTask(Messages
                        .getString("CloneRepoWizard.subTaskParentDirectory.name") + parentDirectory); //$NON-NLS-1$
        m.worked(1);

        if (cloneName == null || cloneName.length() == 0) {
            if (repo.getUri() != null) {
                cloneName = repo.getUri().getFragment();
            } else {
                cloneName = null;
            }
        }

        m
                .subTask(Messages
                        .getString("CloneRepoWizard.subTaskCloneDirectory.name") + cloneName); //$NON-NLS-1$
        m.worked(1);

        try {

            m.subTask(Messages
                    .getString("CloneRepoWizard.subTask.invokingMercurial")); //$NON-NLS-1$
            HgCloneClient.clone(parentDirectory, repo, noUpdate, pull,
                    uncompressed, timeout, rev, cloneName);
            m.worked(1);
        } catch (HgException e) {
            MercurialEclipsePlugin.logError(e);
            throw new InvocationTargetException(e);
        }

        projectLocation = new Path(this.parentDirectory + File.separatorChar
                + cloneName);

        if (searchForProjectFiles) {
            HgFolder folder = new HgFolder(projectLocation.toOSString());
            projectFiles = folder.getProjectFiles();
        } else {
            projectFiles = null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vectrace.MercurialEclipse.actions.HgOperation#getActionDescription()
     */
    @Override
    protected String getActionDescription() {
        return Messages.getString("CloneRepoWizard.actionDescription.1") + repo + Messages.getString("CloneRepoWizard.actionDescription.2") + cloneName; //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * @return the projectFiles
     */
    public List<File> getProjectFiles() {
        return projectFiles;
    }

    /**
     * @return the projectLocation
     */
    public Path getProjectLocation() {
        return projectLocation;
    }

    /**
     * @return the parentDirectory
     */
    public String getParentDirectory() {
        return parentDirectory;
    }

}