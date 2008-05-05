/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Stefan Groschupf          - logError
 *     Stefan C                  - Code cleanup
 *     Bastian Doetsch	         - saving repository to project-specific repos
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
import com.vectrace.MercurialEclipse.commands.HgPushPullClient;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

/**
 * @author zingo
 * 
 */
public class PushRepoWizard extends HgWizard {

    private IProject project;
    private String projectName;

    private PushRepoWizard() {
        super("Push changes to a repository");
        setNeedsProgressMonitor(true);
    }

    public PushRepoWizard(IResource resource) {
        this();
        this.project = resource.getProject();
    }

    @Override
    public void addPages() {
        super.addPages();
        PushRepoPage myPage = new PushRepoPage("PushRepoPage",
                "Push changes to a repository", null, project);
        initPage("Here you can push changes to a repository for sharing them.",
                myPage);
        myPage.setShowCredentials(true);
        page = myPage;
        addPage(page);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.wizard.Wizard#performFinish()
     */
    @Override
    public boolean performFinish() {
        super.performFinish();
        try {
            Properties props = page.getProperties();
            HgRepositoryLocation repo = HgRepositoryLocation
                    .fromProperties(props);

            // Check that this project exist.
            if (project.getLocation() == null) {
                String msg = "Project " + projectName
                        + " don't exists why push?";
                MercurialEclipsePlugin.logError(msg, null);
                // System.out.println( string);
                return false;
            }

            PushRepoPage pushRepoPage = (PushRepoPage) page;

            int timeout = 300000;
            if (!pushRepoPage.isTimeout()) {
                timeout = Integer.MAX_VALUE;
            }

            String result = HgPushPullClient.push(project, repo,
                    repo.getUser(), repo.getPassword(), pushRepoPage.isForce(),
                    pushRepoPage.getRevision(), timeout);

            if (result.length() != 0) {
                Shell shell;
                IWorkbench workbench;

                workbench = PlatformUI.getWorkbench();
                shell = workbench.getActiveWorkbenchWindow().getShell();

                MessageDialog.openInformation(shell,
                        "Mercurial Eclipse Push output", result);
            }

            // It appears good. Stash the repo location.
            MercurialEclipsePlugin.getRepoManager().addRepoLocation(project,
                    repo);
        } catch (MalformedURLException e) {
            MessageDialog.openError(Display.getCurrent().getActiveShell(),
                    "Malformed URL:", e.getMessage());
            return false;

        } catch (Exception e) {
            MercurialEclipsePlugin.logError(e);
            MessageDialog.openError(Display.getCurrent().getActiveShell(), e
                    .getMessage(), e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Creates a ConfigurationWizardPage.
     */
    protected HgWizardPage createPage(String pageName, String pageTitle, String iconPath,
            String description) {
                page = new ConfigurationWizardMainPage(
                        pageName, pageTitle, MercurialEclipsePlugin
                                .getImageDescriptor(iconPath));
                initPage(description, page);
                return page;
            }
}
