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
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgFetchClient;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

/**
 * @author bastian
 * 
 */
public class FetchWizard extends HgWizard {

    private IProject project;
    private String projectName;
    private IncomingPage incomingPage;

    private FetchWizard() {
        super(Messages.getString("FetchWizard.title")); //$NON-NLS-1$        
        setNeedsProgressMonitor(true);
    }

    public FetchWizard(IResource resource) {
        this();
        this.project = resource.getProject();
    }

    @Override
    public void addPages() {
        super.addPages();
        FetchPage fetchPage = new FetchPage(
                Messages.getString("FetchWizard.fetchPage.name"), Messages.getString("FetchWizard.fetchPage.title"), null, project); //$NON-NLS-1$ //$NON-NLS-2$
        initPage(Messages.getString("FetchWizard.fetchPage.description"), //$NON-NLS-1$
                fetchPage);
        fetchPage.setShowCredentials(true);
        page = fetchPage;
        addPage(page);

        incomingPage = new IncomingPage(Messages
                .getString("FetchWizard.incomingPage.name")); //$NON-NLS-1$
        addPage(incomingPage);

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
                String msg = Messages.getString("PushRepoWizard.project") + projectName //$NON-NLS-1$
                        + Messages.getString("PushRepoWizard.notExists"); //$NON-NLS-1$
                MercurialEclipsePlugin.logError(msg, null);
                // System.out.println( string);
                return false;
            }

            incomingPage.finish(new NullProgressMonitor());
            
            String result = HgFetchClient.fetch(project, repo, incomingPage.getRevision());

            if (result.length() != 0) {
                Shell shell;
                IWorkbench workbench;

                workbench = PlatformUI.getWorkbench();
                shell = workbench.getActiveWorkbenchWindow().getShell();

                MessageDialog
                        .openInformation(
                                shell,
                                Messages
                                        .getString("FetchWizard.fetchOutputDialog.title"), result); //$NON-NLS-1$
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
