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
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.operations.CloneOperation;
import com.vectrace.MercurialEclipse.operations.CreateProjectOperation;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.cache.RefreshJob;

/*
 * 
 * This class implements the import wizard extension and the new wizard
 * extension.
 * 
 */

public class CloneRepoWizard extends HgWizard implements IImportWizard {
    private ClonePage clonePage;

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
            // run clone
            CloneOperation cloneOperation = new CloneOperation(getContainer(),
                    clonePage.getDirectoryTextField().getText(), repo,
                    clonePage.getNoUpdateCheckBox().getSelection(), clonePage
                            .getPullCheckBox().getSelection(), clonePage
                            .getUncompressedCheckBox().getSelection(),
                    clonePage.getTimeoutCheckBox().getSelection(), clonePage
                            .getRevisionTextField().getText(), clonePage
                            .getCloneNameTextField().getText(), clonePage
                            .getSearchProjectFilesCheckBox().getSelection());
            getContainer().run(true, false, cloneOperation);

            // create project(s)
            List<File> projectFiles = cloneOperation.getProjectFiles();

            if (projectFiles == null || projectFiles.size() == 0) {
                CreateProjectOperation op = new CreateProjectOperation(
                        getContainer(), new File(cloneOperation
                                .getParentDirectory()), null, repo, false,
                        clonePage.getCloneNameTextField().getText());
                getContainer().run(true, false, op);
                new RefreshJob(
                        Messages.getString("CloneRepoWizard.refreshJob.name"), null, op.getProject()).schedule(); //$NON-NLS-1$
            } else {
                for (File file : projectFiles) {
                    CreateProjectOperation op = new CreateProjectOperation(
                            getContainer(), file.getParentFile(), file, repo,
                            true, null);
                    getContainer().run(true, false, op);
                    new RefreshJob(
                            Messages
                                    .getString("CloneRepoWizard.refreshJob.name"), null, op.getProject()).schedule(); //$NON-NLS-1$
                }
            }

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
