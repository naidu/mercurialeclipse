/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian Doetsch	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import java.net.MalformedURLException;
import java.util.Properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgTransplantClient;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

/**
 * @author bastian
 * 
 */
public class TransplantWizard extends HgWizard {

    private IProject project;

    public TransplantWizard(IResource resource) {
        super("Transplant Wizard");
        setNeedsProgressMonitor(true);
        this.project = resource.getProject();
    }

    @Override
    public void addPages() {
        super.addPages();
        TransplantPage transplantPage = new TransplantPage("TransplantPage",
                "Transplant changesets", null, project);
        initPage("Transplant changesets from another repository or branch.", transplantPage);
        transplantPage.setShowCredentials(true);
        page = transplantPage;
        addPage(page);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.wizard.Wizard#performFinish()
     */
    @Override
    public boolean performFinish() {
        try {
            Properties props = page.getProperties();
            HgRepositoryLocation repo = HgRepositoryLocation
                    .fromProperties(props);

            // Check that this project exist.
            if (project.getLocation() == null) {
                String msg = Messages.getString("PushRepoWizard.project") + project.getName() //$NON-NLS-1$
                        + Messages.getString("PushRepoWizard.notExists"); //$NON-NLS-1$
                MercurialEclipsePlugin.logError(msg, null);
                // System.out.println( string);
                return false;
            }

            TransplantPage tPage = (TransplantPage) page;

            String result = HgTransplantClient.transplant(project, tPage
                    .getNodeIds(), repo, tPage.isBranch(), tPage
                    .getBranchName(), tPage.isAll(), tPage.isMerge(), tPage
                    .getMergeNodeId(), tPage.isPrune(), tPage.getPruneNodeId(),
                    tPage.isContinueLastTransplant(), tPage
                            .isFilterChangesets(), tPage.getFilter());

            if (result.length() != 0) {
                Shell shell;
                IWorkbench workbench;

                workbench = PlatformUI.getWorkbench();
                shell = workbench.getActiveWorkbenchWindow().getShell();

                MessageDialog.openInformation(shell, "Transplant output",
                        result);
            }

            // It appears good. Stash the repo location.
            MercurialEclipsePlugin.getRepoManager().addRepoLocation(project,
                    repo);
        } catch (MalformedURLException e) {
            MessageDialog
                    .openError(
                            Display.getCurrent().getActiveShell(),
                            Messages.getString("PushRepoWizard.malformedUrl"), e.getMessage()); //$NON-NLS-1$
            return false;

        } catch (Exception e) {
            MercurialEclipsePlugin.logError(e);
            MessageDialog.openError(Display.getCurrent().getActiveShell(), e
                    .getMessage(), e.getMessage());
            return false;
        }
        return true;
    }

}
