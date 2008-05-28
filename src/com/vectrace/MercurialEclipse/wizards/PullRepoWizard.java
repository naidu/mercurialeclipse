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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.commands.HgLogClient;
import com.vectrace.MercurialEclipse.commands.HgPushPullClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.menu.CommitMergeHandler;
import com.vectrace.MercurialEclipse.menu.MergeHandler;
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
    private HgRepositoryLocation repo;
    private String result = "";

    private class PullOperation extends HgOperation {

        /**
         * @param context
         */
        public PullOperation(IRunnableContext context) {
            super(context);
            // TODO Auto-generated constructor stub
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.vectrace.MercurialEclipse.actions.HgOperation#getActionDescription()
         */
        @Override
        protected String getActionDescription() {
            return "Pulling...";
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.vectrace.MercurialEclipse.actions.HgOperation#run(org.eclipse.core.runtime.IProgressMonitor)
         */
        @Override
        public void run(IProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException {
            try {
                monitor.beginTask("Pulling...", 6);
                result += performPull(repo, monitor);
                if (pullPage.getFetchCheckBox().getSelection()) {
                    String mergeResult = performMerge(monitor);
                    result += mergeResult;
                    if (mergeResult.contains("0 files unresolved")) {
                        result += CommitMergeHandler.commitMerge(resource);
                    }
                    
                }
            } catch (Exception e) {
                MercurialEclipsePlugin.logError(e);
                throw new InvocationTargetException(e, e.getMessage());
            }
            if (result.length() != 0) {

                IWorkbench workbench = PlatformUI.getWorkbench();
                Shell shell = workbench.getActiveWorkbenchWindow().getShell();

                MessageDialog
                        .openInformation(
                                shell,
                                Messages
                                        .getString("PullRepoWizard.messageDialog.title"), result); //$NON-NLS-1$

            }
        }

    }

    /**
     * 
     */
    public PullRepoWizard(IResource resource) {
        super(Messages.getString("PullRepoWizard.title")); //$NON-NLS-1$
        this.resource = resource;
        setNeedsProgressMonitor(true);
    }

    /**
     * @param monitor
     * @return
     * @throws CoreException
     * @throws PartInitException
     * @throws HgException
     */
    private String performMerge(IProgressMonitor monitor) throws HgException,
            PartInitException, CoreException {
        String r = "";
        if (HgLogClient.getHeads(resource.getProject()).length > 1) {
            r = MergeHandler.merge(resource, getShell());
        }
        return r;
    }

    @Override
    public void addPages() {
        pullPage = new PullPage(Messages
                .getString("PullRepoWizard.pullPage.name"), //$NON-NLS-1$
                Messages.getString("PullRepoWizard.pullPage.title"), //$NON-NLS-1$
                Messages.getString("PullRepoWizard.pullPage.description"), //$NON-NLS-1$
                resource.getProject(), null);

        initPage(pullPage.getDescription(), pullPage);                
        addPage(pullPage);
        
        incomingPage = new IncomingPage(Messages
                .getString("PullRepoWizard.incomingPage.name")); //$NON-NLS-1$
        initPage(incomingPage.getDescription(), pullPage);
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
        repo = getLocation();

        try {
            getContainer().run(false, false, new PullOperation(getContainer()));
        } catch (Exception e) {
            MercurialEclipsePlugin.logError(e);
            MercurialEclipsePlugin.showError(e);
            return false;
        }

        return true;
    }

    /**
     * @return
     */
    private boolean saveRepo(IProgressMonitor monitor) {
        // It appears good. Stash the repo location.
        try {
            monitor.subTask("Adding repository " + repo);
            MercurialEclipsePlugin.getRepoManager().addRepoLocation(
                    resource.getProject(), repo);
        } catch (HgException e) {
            MercurialEclipsePlugin.logError(Messages
                    .getString("PullRepoWizard.addingRepositoryFailed"), e); //$NON-NLS-1$
        }
        monitor.worked(1);
        return true;
    }

    private String performPull(final HgRepositoryLocation repository,
            IProgressMonitor monitor) throws InvocationTargetException {
        try {
            monitor.subTask("Pulling incoming changesets...");
            ChangeSet cs = null;
            if (incomingPage.getRevisionCheckBox().getSelection()) {
                cs = incomingPage.getRevision();
            }
            this.doUpdate = pullPage.getUpdateCheckBox().getSelection();
            String r = HgPushPullClient.pull(resource, repository,
                    isDoUpdate(), pullPage.getForceCheckBox().getSelection(),
                    pullPage.getTimeoutCheckBox().getSelection(), cs);

            monitor.worked(1);

            monitor.subTask("Refreshing local changesets after pull...");
            IncomingChangesetCache.getInstance().clear();
            LocalChangesetCache.getInstance().clear(resource.getProject());
            LocalChangesetCache.getInstance().refreshAllLocalRevisions(
                    resource.getProject());
            monitor.worked(1);
            monitor.subTask("Refreshing status...");
            new RefreshStatusJob(
                    Messages.getString("PullRepoWizard.refreshJob.title"), resource.getProject()).schedule(); //$NON-NLS-1$
            monitor.worked(1);
            saveRepo(monitor);
            return r;

        } catch (Exception e) {
            MercurialEclipsePlugin.logError(Messages
                    .getString("PullRepoWizard.pullOperationFailed"), e); //$NON-NLS-1$
            throw new InvocationTargetException(e, e.getMessage());
        }
    }

    private HgRepositoryLocation getLocation() {
        try {
            return HgRepositoryLocation
                    .fromProperties(pullPage.getProperties());
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
