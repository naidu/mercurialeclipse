/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Software Balm Consulting Inc (Peter Hunnisett <peter_hge at softwarebalm dot com>) - implementation
 *     VecTrace (Zingo Andersen) - some updates
 *     Stefan Groschupf          - logError
 *     Stefan C                  - Code cleanup
 *     Bastian Doetsch	         - saving repository to project-specific repos
 *******************************************************************************/

package com.vectrace.MercurialEclipse.wizards;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.internal.resources.ProjectDescription;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.commands.HgCloneClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.cache.RefreshJob;

/*
 * 
 * This class implements the import wizard extension and the new wizard
 * extension.
 * 
 */

public class CloneRepoWizard extends HgWizard implements IImportWizard {
    private ClonePage clonePage;

    private class CloneOperation extends HgOperation {

        private String parentDirectory;
        private HgRepositoryLocation repo;
        private boolean noUpdate;
        private boolean pull;
        private boolean uncompressed;
        private boolean timeout;
        private String rev;
        private String cloneName;
        private IProject project;

        /**
         * @param name
         */
        public CloneOperation(IRunnableContext context, String parentDirectory,
                HgRepositoryLocation repo, boolean noUpdate, boolean pull,
                boolean uncompressed, boolean timeout, String rev,
                String cloneName) {
            super(context);
            this.parentDirectory = parentDirectory;
            this.repo = repo;
            this.noUpdate = noUpdate;
            this.pull = pull;
            this.uncompressed = uncompressed;
            this.timeout = timeout;
            this.rev = rev;
            this.cloneName = cloneName;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
         */
        @SuppressWarnings("restriction")//$NON-NLS-1$
        @Override
        public void run(IProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException {

            IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace()
                    .getRoot();

            monitor.beginTask(Messages
                    .getString("CloneRepoWizard.operation.name"), 15); //$NON-NLS-1$

            // set defaults
            if (this.parentDirectory == null || parentDirectory.length() == 0) {
                parentDirectory = workspaceRoot.getLocation().toOSString();
            }

            monitor
                    .subTask(Messages
                            .getString("CloneRepoWizard.subTaskParentDirectory.name") + parentDirectory); //$NON-NLS-1$
            monitor.worked(1);

            if (cloneName == null || cloneName.length() == 0) {
                cloneName = repo.getUri().getFragment();
            }

            monitor
                    .subTask(Messages
                            .getString("CloneRepoWizard.subTaskCloneDirectory.name") + cloneName); //$NON-NLS-1$
            monitor.worked(1);

            try {

                monitor
                        .subTask(Messages
                                .getString("CloneRepoWizard.subTask.invokingMercurial")); //$NON-NLS-1$
                HgCloneClient.clone(parentDirectory, repo, noUpdate, pull,
                        uncompressed, timeout, rev, cloneName);
                monitor.worked(1);
            } catch (HgException e) {
                MercurialEclipsePlugin.logError(e);
                throw new InvocationTargetException(e);
            }

            project = workspaceRoot.getProject(cloneName);
            IProjectDescription description = new ProjectDescription();
            description.setName(cloneName);
            description.setComment(Messages
                    .getString("CloneRepoWizard.description.comment") + repo); //$NON-NLS-1$

            IPath projectLocation = null;
            if (!workspaceRoot.getLocation().toOSString().equals(
                    this.parentDirectory)) {
                projectLocation = new Path(this.parentDirectory
                        + File.separatorChar + cloneName);
            }

            description.setLocation(projectLocation);

            try {
                project.create(description, monitor);
                project.open(monitor);
            } catch (CoreException e) {
                MercurialEclipsePlugin.logError(e);
                throw new InvocationTargetException(e, e.getMessage());
            }

            try {
                // Register the project with Team. This will bring all the files
                // that
                // we cloned into the project.
                monitor
                        .subTask(Messages
                                .getString("CloneRepoWizard.subTask.registeringProject1") + project.getName() //$NON-NLS-1$
                                + Messages
                                        .getString("CloneRepoWizard.subTaskRegisteringProject2")); //$NON-NLS-1$
                RepositoryProvider.map(project, MercurialTeamProvider.class
                        .getName());
                RepositoryProvider.getProvider(project,
                        MercurialTeamProvider.class.getName());
            } catch (TeamException e) {
                MercurialEclipsePlugin.logError(e);
                throw new InvocationTargetException(e, e.getMessage());
            }
            monitor.worked(1);

            // It appears good. Stash the repo location.
            try {
                monitor
                        .subTask(Messages
                                .getString("CloneRepoWizard.subTask.addingRepository.1") + repo //$NON-NLS-1$
                                + Messages
                                        .getString("CloneRepoWizard.subTask.addingRepository.2")); //$NON-NLS-1$
                MercurialEclipsePlugin.getRepoManager().addRepoLocation(
                        project, repo);
            } catch (HgException e) {
                MercurialEclipsePlugin.logError(e);
                throw new InvocationTargetException(e, e.getMessage());
            }
            monitor.worked(1);
            monitor.done();
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
         * @return the project
         */
        public IProject getProject() {
            return project;
        }

    }

    /**
     * @param windowTitle
     */
    public CloneRepoWizard() {
        super(Messages.getString("CloneRepoWizard.title")); //$NON-NLS-1$
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.wizard.Wizard#performFinish()
     */
    @Override
    public boolean performFinish() {
        clonePage.finish(null);
        HgRepositoryLocation repo;
        IResource res;
        try {
            repo = HgRepositoryLocation.fromProperties(clonePage
                    .getProperties());
            res = ResourcesPlugin.getWorkspace().getRoot().findMember(
                    clonePage.getCloneNameTextField().getText());
        } catch (Exception e) {
            MessageDialog
                    .openError(
                            Display.getCurrent().getActiveShell(),
                            Messages.getString("CloneRepoWizard.malformedURL"), e.getMessage()); //$NON-NLS-1$
            return false;
        }

        // Check that this project doesn't exist.
        if (res != null) {
            MessageDialog
                    .openError(
                            Display.getCurrent().getActiveShell(),
                            "Error occurred while cloning", Messages.getString("CloneRepoWizard.project") + res.getName() + Messages.getString("CloneRepoWizard.alreadyExists")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            return false;
        }

        try {

            CloneOperation cloneOperation = new CloneOperation(getContainer(),
                    clonePage.getDirectoryTextField().getText(), repo,
                    clonePage.getNoUpdateCheckBox().getSelection(), clonePage
                            .getPullCheckBox().getSelection(), clonePage
                            .getUncompressedCheckBox().getSelection(),
                    clonePage.getTimeoutCheckBox().getSelection(), clonePage
                            .getRevisionTextField().getText(), clonePage
                            .getCloneNameTextField().getText());
            getContainer().run(false, false, cloneOperation);
            new RefreshJob(
                    Messages.getString("CloneRepoWizard.refreshJob.name"), null, cloneOperation.getProject()).schedule(); //$NON-NLS-1$
        } catch (Exception e) {
            MercurialEclipsePlugin.showError(e);
            MercurialEclipsePlugin.logError(Messages
                    .getString("CloneRepoWizard.cloneOperationFailed"), e); //$NON-NLS-1$
        }

        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench,
     *      org.eclipse.jface.viewers.IStructuredSelection)
     */
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        setWindowTitle(Messages.getString("CloneRepoWizard.title")); //$NON-NLS-1$
        setNeedsProgressMonitor(true);
        clonePage = new ClonePage(null, Messages
                .getString("CloneRepoWizard.pageName"), //$NON-NLS-1$
                Messages.getString("CloneRepoWizard.pageTitle"), null); //$NON-NLS-1$

        clonePage.setDescription(Messages
                .getString("CloneRepoWizard.pageDescription")); //$NON-NLS-1$
        initPage(clonePage.getDescription(), clonePage);
        addPage(clonePage);
    }

}
