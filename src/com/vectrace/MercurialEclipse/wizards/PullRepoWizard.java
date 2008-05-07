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
 *     Bastian Doetsch			 - saving repository to projec specific repos.
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import java.net.MalformedURLException;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgPushPullClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.team.cache.IncomingChangesetCache;


public class PullRepoWizard extends SyncRepoWizard
{

  IProject project;
  boolean doUpdate = false;


  /* (non-Javadoc)
   * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.jface.viewers.IStructuredSelection)
   */
  @Override
public void init(IWorkbench workbench, IStructuredSelection selection)
  {
    project = MercurialUtilities.getProject(selection);
    projectName = project.getName();
    setWindowTitle(Messages.getString("ImportWizard.WizardTitle")); //$NON-NLS-1$
    setNeedsProgressMonitor(true);
  }

    @Override
    public void addPages() {
       PullPage pullPage = new PullPage("PullRepoPage",
               "Pull changes from repository",
               "Select a repository location to pull from",
               project,
               null);
       // legacy - required by super
       super.syncRepoLocationPage = pullPage;
       addPage(pullPage);
       addPage(new IncomingPage("Incoming"));
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.wizard.Wizard#performFinish()
     */
    @Override
    public boolean performFinish() {

        // If there is no project set the wizard can't finish
        if (project.getLocation() == null) {
            return false;
        }

        final HgRepositoryLocation repo = getLocation();

        performPull(repo);

        // It appears good. Stash the repo location.
        try {
            MercurialEclipsePlugin.getRepoManager().addRepoLocation(project,
                    repo);
        } catch (HgException e) {
            MercurialEclipsePlugin.logError(
                    "Adding repository to persistent storage failed.", e);
        }

        return true;
    }

    private void performPull(final HgRepositoryLocation repo) {
        try {
            String result = HgPushPullClient.pull(project, repo, isDoUpdate());
            IncomingChangesetCache.getInstance().clear();
            if (result.length() != 0) {

                Shell shell;
                IWorkbench workbench;

                workbench = PlatformUI.getWorkbench();
                shell = workbench.getActiveWorkbenchWindow().getShell();

                MessageDialog.openInformation(shell,
                        "Mercurial Eclipse Pull output", result);

            }

        } catch (Exception e) {
            MercurialEclipsePlugin.logError("pull operation failed", e);
        }
    }

    HgRepositoryLocation getLocation() {

        try {
            return new HgRepositoryLocation(locationUrl);
        } catch (MalformedURLException e) {
            MessageDialog.openInformation(getShell(), "URL is malformed.",e.getMessage());
            MercurialEclipsePlugin.logInfo(e.getMessage(), e);
            return null;
        }
    }

    /**
     * @return the doUpdate
     */
    public boolean isDoUpdate() {
        return doUpdate;
    }

    /**
     * @param doUpdate
     *            true if the pull should be followed by an update
     */
    public void setDoUpdate(boolean doUpdate) {
        this.doUpdate = doUpdate;
    }
}
