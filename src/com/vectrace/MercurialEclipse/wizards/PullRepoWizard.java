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

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeUiJob;
import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.commands.HgLogClient;
import com.vectrace.MercurialEclipse.commands.HgPushPullClient;
import com.vectrace.MercurialEclipse.commands.HgResolveClient;
import com.vectrace.MercurialEclipse.commands.HgSvnClient;
import com.vectrace.MercurialEclipse.commands.forest.HgFpushPullClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.menu.CommitMergeHandler;
import com.vectrace.MercurialEclipse.menu.MergeHandler;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.FlaggedAdaptable;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.ResourceProperties;
import com.vectrace.MercurialEclipse.team.cache.IncomingChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.RefreshStatusJob;

public class PullRepoWizard extends HgWizard {

    private boolean doUpdate;
    private PullPage pullPage;
    private IncomingPage incomingPage;
    private IResource resource;
    private HgRepositoryLocation repo;

    private class PullOperation extends HgOperation {
        private boolean doUpdate;
        private IResource resource;
        private HgRepositoryLocation repo;
        private boolean force;
        private ChangeSet pullRevision;
        private boolean timeout;
        private boolean merge;
        private String output = "";
        private boolean showCommitDialog;
        private File bundleFile;
        private boolean forest;
        private File snapFile;
        private boolean rebase;
        private boolean svn;

        /**
         * @param context
         * @param merge
         * @param svn
         */
        public PullOperation(IRunnableContext context, boolean doUpdate,
                IResource resource, boolean force, HgRepositoryLocation repo,
                ChangeSet pullRevision, boolean timeout, boolean merge,
                boolean showCommitDialog, File bundleFile, boolean forest,
                File snapFile, boolean rebase, boolean svn) {
            super(context);
            this.doUpdate = doUpdate;
            this.resource = resource;
            this.force = force;
            this.repo = repo;
            this.pullRevision = pullRevision;
            this.timeout = timeout;
            this.merge = merge;
            this.showCommitDialog = showCommitDialog;
            this.bundleFile = bundleFile;
            this.forest = forest;
            this.snapFile = snapFile;
            this.rebase = rebase;
            this.svn = svn;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * com.vectrace.MercurialEclipse.actions.HgOperation#getActionDescription
         * ()
         */
        @Override
        protected String getActionDescription() {
            return "Pulling...";
        }

        /**
         * @param monitor
         * @return
         * @throws CoreException
         * @throws PartInitException
         * @throws HgException
         * @throws InterruptedException
         */
        private String performMerge(IProgressMonitor monitor)
                throws HgException, PartInitException, CoreException,
                InterruptedException {
            String r = "Output of Merge:\n";
            monitor.subTask("Merging...");
            if (HgLogClient.getHeads(resource.getProject()).length > 1) {

                SafeUiJob job = new SafeUiJob("Merging...") {
                    /*
                     * (non-Javadoc)
                     * 
                     * @see
                     * com.vectrace.MercurialEclipse.SafeUiJob#runSafe(org.eclipse
                     * .core.runtime.IProgressMonitor)
                     */
                    @Override
                    protected IStatus runSafe(IProgressMonitor m) {
                        try {
                            String res = MergeHandler.merge(resource,
                                    getShell());
                            return new Status(IStatus.OK,
                                    MercurialEclipsePlugin.ID, res);
                        } catch (Exception e) {
                            MercurialEclipsePlugin.logError(e);
                            return new Status(IStatus.ERROR,
                                    MercurialEclipsePlugin.ID, e
                                            .getLocalizedMessage(), e);
                        }
                    }
                };
                job.schedule();
                job.join();
                IStatus jobResult = job.getResult();
                if (jobResult.getSeverity() == IStatus.OK) {
                    r += jobResult.getMessage();
                } else {
                    throw new HgException(jobResult.getMessage(), jobResult
                            .getException());
                }
            }
            monitor.worked(1);
            return r;
        }

        private String performPull(final HgRepositoryLocation repository,
                IProgressMonitor monitor) throws InvocationTargetException {
            try {
                monitor.worked(1);
                monitor.subTask("Pulling incoming changesets...");
                String r = "Output of Pull:\n";
                if (svn) {
                    r += HgSvnClient.pull(resource.getLocation().toFile());
                    if (rebase) {
                        r += HgSvnClient
                                .rebase(resource.getLocation().toFile());
                    }
                } else if (bundleFile == null) {
                    if (forest) {
                        File forestRoot = MercurialTeamProvider.getHgRoot(
                                resource.getLocation().toFile())
                                .getParentFile();
                        r += HgFpushPullClient.fpull(forestRoot, this.repo,
                                this.doUpdate, this.timeout, this.pullRevision,
                                true, snapFile, false);
                    } else {
                        r += HgPushPullClient.pull(resource, this.repo,
                                this.doUpdate, this.force, this.timeout,
                                pullRevision, rebase);
                    }
                } else {
                    r += HgPushPullClient.pull(resource, this.doUpdate,
                            this.force, this.timeout, pullRevision, bundleFile
                                    .getCanonicalPath(), rebase);
                }

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

        /**
         * @return
         */
        private boolean saveRepo(IProgressMonitor monitor) {
            // It appears good. Stash the repo location.
            try {
                monitor.subTask("Adding repository " + this.repo);
                MercurialEclipsePlugin.getRepoManager().addRepoLocation(
                        resource.getProject(), repo);
            } catch (HgException e) {
                MercurialEclipsePlugin.logError(Messages
                        .getString("PullRepoWizard.addingRepositoryFailed"), e); //$NON-NLS-1$
            }
            monitor.worked(1);
            return true;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * com.vectrace.MercurialEclipse.actions.HgOperation#run(org.eclipse
         * .core.runtime.IProgressMonitor)
         */
        @Override
        public void run(IProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException {
            try {
                monitor.beginTask("Pulling...", 6);
                this.output += performPull(repo, monitor);
                if (merge) {
                    String mergeResult = performMerge(monitor);
                    output += mergeResult;
                    commitMerge(monitor, mergeResult);
                }
            } catch (Exception e) {
                MercurialEclipsePlugin.logError(e);
                throw new InvocationTargetException(e, e.getMessage());
            }
            monitor.done();
        }

        /**
         * @param monitor
         * @param mergeResult
         * @throws HgException
         * @throws CoreException
         * @throws InterruptedException
         */
        private void commitMerge(IProgressMonitor monitor, String mergeResult)
                throws HgException, CoreException, InterruptedException {
            if (resource != null
                    && resource.getProject() != null
                    && resource.getProject().getPersistentProperty(
                            ResourceProperties.MERGING) != null) {
                boolean commit = true;
                if (!HgResolveClient.checkAvailable()) {
                    if (!mergeResult.contains("all conflicts resolved")) {
                        commit = false;
                    }
                } else {
                    List<FlaggedAdaptable> mergeAdaptables = HgResolveClient
                            .list(resource);
                    monitor.subTask("Getting merge status...");
                    for (FlaggedAdaptable flaggedAdaptable : mergeAdaptables) {
                        if (flaggedAdaptable.getFlag() == 'U') {
                            commit = false;
                            break;
                        }
                    }
                    monitor.worked(1);
                }
                if (commit) {
                    monitor.subTask("Committing...");
                    output += "Output of Commit:\n";
                    if (!showCommitDialog) {
                        output += CommitMergeHandler.commitMerge(resource);
                    } else {
                        SafeUiJob job = new SafeUiJob(
                                "Opening commit dialog...") {
                            /*
                             * (non-Javadoc)
                             * 
                             * @see
                             * com.vectrace.MercurialEclipse.SafeUiJob#runSafe
                             * (org .eclipse.core.runtime.IProgressMonitor)
                             */
                            @Override
                            protected IStatus runSafe(IProgressMonitor moni) {
                                try {
                                    String res = new CommitMergeHandler()
                                            .commitMergeWithCommitDialog(
                                                    resource, getShell());
                                    return new Status(IStatus.OK,
                                            MercurialEclipsePlugin.ID, res);
                                } catch (HgException e) {
                                    MercurialEclipsePlugin.logError(e);
                                    return new Status(IStatus.ERROR,
                                            MercurialEclipsePlugin.ID, e
                                                    .getLocalizedMessage(), e);
                                }

                            }
                        };
                        job.schedule();
                        job.join();
                        IStatus jobResult = job.getResult();
                        if (jobResult.getSeverity() == IStatus.OK) {
                            output += jobResult.getMessage();
                        } else {
                            throw new HgException(jobResult.getMessage(),
                                    jobResult.getException());
                        }
                    }
                    monitor.worked(1);
                }
            }
        }

        public String getOutput() {
            return output;
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
        initPage(incomingPage.getDescription(), incomingPage);
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
            doUpdate = pullPage.getUpdateCheckBox().getSelection();
            boolean force = pullPage.getForceCheckBox().getSelection();

            ChangeSet cs = null;
            if (incomingPage.getRevisionCheckBox().getSelection()) {
                cs = incomingPage.getRevision();
            }

            boolean timeout = pullPage.getTimeoutCheckBox().getSelection();
            boolean merge = pullPage.getMergeCheckBox().getSelection();
            boolean rebase = false;
            Button rebase_button = pullPage.getRebaseCheckBox();
            if (rebase_button != null ) {
                rebase = rebase_button.getSelection();
            }
            boolean showCommitDialog = pullPage.getCommitDialogCheckBox()
                    .getSelection();
            boolean svn = false;
            if (pullPage.isShowSvn()) {
                svn = pullPage.getSvnCheckBox().getSelection();
            }
            boolean forest = false;
            File snapFile = null;
            if (pullPage.isShowForest()) {
                forest = pullPage.getForestCheckBox().getSelection();                
                String snapFileText = pullPage.getSnapFileCombo().getText();
                if (snapFileText.length() > 0) {
                    snapFile = new File(snapFileText);
                }
            }

            File bundleFile = null;
            if (incomingPage.getChangesets() != null
                    && incomingPage.getChangesets().size() > 0) {
                bundleFile = incomingPage.getChangesets().first()
                        .getBundleFile();
            }

            PullOperation pullOperation = new PullOperation(getContainer(),
                    doUpdate, resource, force, repo, cs, timeout, merge,
                    showCommitDialog, bundleFile, forest, snapFile, rebase, svn);
            getContainer().run(true, false, pullOperation);

            String output = pullOperation.getOutput();

            if (output.length() != 0) {
                IWorkbench workbench = PlatformUI.getWorkbench();
                Shell shell = workbench.getActiveWorkbenchWindow().getShell();
                MessageDialog
                        .openInformation(
                                shell,
                                Messages
                                        .getString("PullRepoWizard.messageDialog.title"), output); //$NON-NLS-1$

            }

        } catch (Exception e) {
            MercurialEclipsePlugin.logError(e);
            MercurialEclipsePlugin.showError(e.getCause());
            return false;
        }

        return true;
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
}
