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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgPushPullClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.cache.IncomingChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.RefreshStatusJob;

public class PullRepoWizard extends HgWizard {

    private boolean doUpdate;
    private PullPage pullPage;
    private IncomingPage incomingPage;
    private IResource resource;

    /**
     * 
     */
    public PullRepoWizard(IResource resource) {
        super(Messages.getString("PullRepoWizard.title")); //$NON-NLS-1$
        this.resource = resource;
    }

    @Override
    public void addPages() {
        pullPage = new PullPage(Messages
                .getString("PullRepoWizard.pullPage.name"), //$NON-NLS-1$
                Messages.getString("PullRepoWizard.pullPage.title"), //$NON-NLS-1$
                Messages.getString("PullRepoWizard.pullPage.description"), //$NON-NLS-1$
                resource.getProject(), null);

        addPage(pullPage);
        incomingPage = new IncomingPage(Messages
                .getString("PullRepoWizard.incomingPage.name")); //$NON-NLS-1$
        addPage(incomingPage);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.wizard.Wizard#performFinish()
     */
    @Override
    public boolean performFinish() {

        // If there is no project set the wizard can't finish
        if (resource.getProject().getLocation() == null) {
            return false;
        }

        pullPage.finish(new NullProgressMonitor());
        incomingPage.finish(new NullProgressMonitor());
        
        final HgRepositoryLocation repo = getLocation();
        performPull(repo);

        // It appears good. Stash the repo location.
        try {
            MercurialEclipsePlugin.getRepoManager().addRepoLocation(
                    resource.getProject(), repo);
        } catch (HgException e) {
            MercurialEclipsePlugin.logError(Messages
                    .getString("PullRepoWizard.addingRepositoryFailed"), e); //$NON-NLS-1$
        }
        return true;
    }

    private void performPull(final HgRepositoryLocation repo) {
        try {
            ChangeSet cs = null;
            if (incomingPage.getRevisionCheckBox().getSelection()) {
                cs = incomingPage.getRevision();
            }
            this.doUpdate=pullPage.getUpdateCheckBox().getSelection();
            String result = HgPushPullClient.pull(resource, repo, isDoUpdate(),
                    pullPage.getForceCheckBox().getSelection(), pullPage
                            .getTimeoutCheckBox().getSelection(), cs);
            IncomingChangesetCache.getInstance().clear();
            LocalChangesetCache.getInstance().clear(resource.getProject());
            new RefreshStatusJob(Messages.getString("PullRepoWizard.refreshJob.title"),resource.getProject()).schedule(); //$NON-NLS-1$
            if (result.length() != 0) {

                Shell shell;
                IWorkbench workbench;

                workbench = PlatformUI.getWorkbench();
                shell = workbench.getActiveWorkbenchWindow().getShell();

                MessageDialog
                        .openInformation(
                                shell,
                                Messages
                                        .getString("PullRepoWizard.messageDialog.title"), result); //$NON-NLS-1$

            }

        } catch (Exception e) {
            MercurialEclipsePlugin.logError(Messages
                    .getString("PullRepoWizard.pullOperationFailed"), e); //$NON-NLS-1$
        }
    }

    private HgRepositoryLocation getLocation() {
        try {
            return HgRepositoryLocation.fromProperties(pullPage.getProperties());
        } catch (Exception e) {
            MessageDialog.openInformation(getShell(), Messages
                    .getString("PullRepoWizard.malformedURL"), e.getMessage()); //$NON-NLS-1$
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
