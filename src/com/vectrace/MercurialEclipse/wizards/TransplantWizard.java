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

import java.net.URISyntaxException;
import java.util.Properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.extensions.HgTransplantClient;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

/**
 * @author bastian
 * 
 */
public class TransplantWizard extends HgWizard {

    private IProject project;

    public TransplantWizard(IResource resource) {
        super(Messages.getString("TransplantWizard.title")); //$NON-NLS-1$
        setNeedsProgressMonitor(true);
        this.project = resource.getProject();
    }

    @Override
    public void addPages() {
        super.addPages();
        TransplantPage transplantPage = new TransplantPage(Messages.getString("TransplantWizard.transplantPage.name"), //$NON-NLS-1$
                Messages.getString("TransplantWizard.transplantPage.title"), null, project); //$NON-NLS-1$
        initPage(Messages.getString("TransplantWizard.transplantPage.description"), //$NON-NLS-1$
                transplantPage);
        transplantPage.setShowCredentials(true);
        page = transplantPage;
        addPage(page);

        TransplantOptionsPage optionsPage = new TransplantOptionsPage(
                Messages.getString("TransplantWizard.optionsPage.name"), Messages.getString("TransplantWizard.optionsPage.title"), null, project); //$NON-NLS-1$ //$NON-NLS-2$
        initPage(Messages.getString("TransplantWizard.optionsPage.description"), optionsPage); //$NON-NLS-1$
        addPage(optionsPage);
    }
    
    

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.wizard.Wizard#performFinish()
     */
    @Override
    public boolean performFinish() {
        try {
            page.finish(new NullProgressMonitor());
            Properties props = page.getProperties();
            HgRepositoryLocation repo = MercurialEclipsePlugin.getRepoManager()
                    .fromProperties(props);

            // Check that this project exist.
            if (project.getLocation() == null) {
                String msg = Messages.getString("PushRepoWizard.project") + project.getName() //$NON-NLS-1$
                        + Messages.getString("PushRepoWizard.notExists"); //$NON-NLS-1$
                MercurialEclipsePlugin.logError(msg, null);
                // System.out.println( string);
                return false;
            }

            TransplantPage transplantPage = (TransplantPage) page;
            TransplantOptionsPage optionsPage = (TransplantOptionsPage) page
                    .getNextPage();

            String result = HgTransplantClient.transplant(project,
                    transplantPage.getNodeIds(), repo, transplantPage
                            .isBranch(), transplantPage.getBranchName(),
                    transplantPage.isAll(), optionsPage.isMerge(), optionsPage
                            .getMergeNodeId(), optionsPage.isPrune(),
                    optionsPage.getPruneNodeId(), optionsPage
                            .isContinueLastTransplant(), optionsPage
                            .isFilterChangesets(), optionsPage.getFilter());

            if (result.length() != 0) {
                Shell shell;
                IWorkbench workbench;

                workbench = PlatformUI.getWorkbench();
                shell = workbench.getActiveWorkbenchWindow().getShell();

                MessageDialog.openInformation(shell, Messages.getString("TransplantWizard.outputMessage"), //$NON-NLS-1$
                        result);
            }

            // It appears good. Stash the repo location.
            MercurialEclipsePlugin.getRepoManager().addRepoLocation(project,
                    repo);
        } catch (URISyntaxException e) {
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
