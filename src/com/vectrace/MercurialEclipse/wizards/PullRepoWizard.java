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
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeUiJob;
import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.commands.HgClients;
import com.vectrace.MercurialEclipse.commands.HgLogClient;
import com.vectrace.MercurialEclipse.commands.HgPushPullClient;
import com.vectrace.MercurialEclipse.commands.extensions.HgSvnClient;
import com.vectrace.MercurialEclipse.commands.extensions.forest.HgFpushPullClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.menu.MergeHandler;
import com.vectrace.MercurialEclipse.menu.UpdateHandler;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.cache.IncomingChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;

public class PullRepoWizard extends HgWizard {

    private boolean doUpdate;
    private PullPage pullPage;
    private IncomingPage incomingPage;
    private final IProject resource;
    private HgRepositoryLocation repo;

    private static class PullOperation extends HgOperation {
        private final boolean doUpdate;
        private final IProject resource;
        private final HgRepositoryLocation repo;
        private final boolean force;
        private final ChangeSet pullRevision;
        private final boolean timeout;
        private final boolean merge;
        private String output = ""; //$NON-NLS-1$
        private final boolean showCommitDialog;
        private final File bundleFile;
        private final boolean forest;
        private final File snapFile;
        private final boolean rebase;
        private final boolean svn;

        public PullOperation(IRunnableContext context, boolean doUpdate,
                IProject resource, boolean force, HgRepositoryLocation repo,
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

        @Override
        protected String getActionDescription() {
            return Messages.getString("PullRepoWizard.pullOperation.description"); //$NON-NLS-1$
        }

        private String performMerge(IProgressMonitor monitor) throws HgException, InterruptedException {
            String r = Messages.getString("PullRepoWizard.pullOperation.mergeHeader"); //$NON-NLS-1$
            monitor.subTask(Messages.getString("PullRepoWizard.pullOperation.merging")); //$NON-NLS-1$
            if (HgLogClient.getHeads(resource.getProject()).length > 1) {

                SafeUiJob job = new SafeUiJob(Messages.getString("PullRepoWizard.pullOperation.mergeJob.description")) { //$NON-NLS-1$
                    @Override
                    protected IStatus runSafe(IProgressMonitor m) {
                        try {
                            String res = MergeHandler.merge(resource, getShell(), m, true, showCommitDialog);
                            return new Status(IStatus.OK, MercurialEclipsePlugin.ID, res);
                        } catch (CoreException e) {
                            MercurialEclipsePlugin.logError(e);
                            return e.getStatus();
                        }
                    }
                };
                job.schedule();
                job.join();
                IStatus jobResult = job.getResult();
                if (jobResult.getSeverity() == IStatus.OK) {
                    r += jobResult.getMessage();
                } else {
                    throw new HgException(jobResult);
                }
            }
            monitor.worked(1);
            return r;
        }

        private String performPull(final HgRepositoryLocation repository,
                IProgressMonitor monitor) throws InvocationTargetException {
            try {
                monitor.worked(1);
                monitor.subTask(Messages.getString("PullRepoWizard.pullOperation.incoming")); //$NON-NLS-1$
                String r = Messages.getString("PullRepoWizard.pullOperation.pull.header"); //$NON-NLS-1$
                boolean updateSeparately = false;

                if (svn) {
                    r += HgSvnClient.pull(resource.getLocation().toFile());
                    if (rebase) {
                        r += HgSvnClient.rebase(resource.getLocation().toFile());
                    }
                } else if (bundleFile == null) {
                    if (forest) {
                        File forestRoot = MercurialTeamProvider.getHgRoot(
                                resource.getLocation().toFile()).getParentFile();
                        r += HgFpushPullClient.fpull(forestRoot, repo,
                                doUpdate, timeout, pullRevision, true, snapFile, false);
                    } else {
                        if (doUpdate) {
                            updateSeparately = true;
                        }
                        r += HgPushPullClient.pull(resource, pullRevision, repo, false, rebase, force, timeout);
                    }
                } else {
                    if (doUpdate) {
                        updateSeparately = true;
                    }
                    r += HgPushPullClient.pull(resource, pullRevision,
                            bundleFile.getCanonicalPath(), false, rebase, force, timeout);
                }

                monitor.worked(1);

                monitor.subTask(Messages.getString("PullRepoWizard.pullOperation.refresh.description")); //$NON-NLS-1$
                IncomingChangesetCache.getInstance().clear(repository);
                LocalChangesetCache.getInstance().refreshAllLocalRevisions(resource.getProject(), true);
                monitor.worked(1);
                monitor.subTask(Messages.getString("PullRepoWizard.pullOperation.status")); //$NON-NLS-1$

                saveRepo(monitor);

                if (updateSeparately) {
                    final IResource res = resource;
                    Display.getDefault().syncExec(new Runnable() {
                        public void run() {
                            try {
                                new UpdateHandler().run(res);
                            } catch (Exception e) {
                                if (e instanceof HgException) {
                                    // no point in complaining, since they want to merge/rebase anyway
                                    if ((merge || rebase) && e.getMessage().contains("crosses branches")) {
                                        return;
                                    }
                                }
                                MercurialEclipsePlugin.logError(e);
                                MercurialEclipsePlugin.showError(e);
                            }
                        }
                    });
                }
                return r;

            } catch (CoreException e) {
                MercurialEclipsePlugin.logError(Messages
                        .getString("PullRepoWizard.pullOperationFailed"), e); //$NON-NLS-1$
                throw new InvocationTargetException(e, e.getMessage());
            } catch (IOException e) {
                MercurialEclipsePlugin.logError(Messages
                        .getString("PullRepoWizard.pullOperationFailed"), e); //$NON-NLS-1$
                throw new InvocationTargetException(e, e.getMessage());
            }
        }

        private boolean saveRepo(IProgressMonitor monitor) {
            // It appears good. Stash the repo location.
            try {
                monitor.subTask(Messages.getString("PullRepoWizard.pullOperation.addRepo") + this.repo); //$NON-NLS-1$
                MercurialEclipsePlugin.getRepoManager().addRepoLocation(
                        resource.getProject(), repo);
            } catch (HgException e) {
                MercurialEclipsePlugin.logError(Messages
                        .getString("PullRepoWizard.addingRepositoryFailed"), e); //$NON-NLS-1$
            }
            monitor.worked(1);
            return true;
        }

        @Override
        public void run(IProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException {
            try {
                monitor.beginTask(Messages.getString("PullRepoWizard.pullOperation.pulling"), 6); //$NON-NLS-1$
                this.output += performPull(repo, monitor);
                if (merge) {
                    output += performMerge(monitor);
                }
            } catch (Exception e) {
                throw new InvocationTargetException(e, e.getMessage());
            }
            monitor.done();
        }

        public String getOutput() {
            return output;
        }
    }

    public PullRepoWizard(IProject resource) {
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
                HgClients.getConsole().printMessage(output, null);
            }

            IncomingChangesetCache.getInstance().clear(repo);

        } catch (Exception e) {
            MercurialEclipsePlugin.logError(e);
            MercurialEclipsePlugin.showError(e.getCause());
            return false;
        }

        return true;
    }

    private HgRepositoryLocation getLocation() {
        try {
            return MercurialEclipsePlugin.getRepoManager()
                    .fromProperties(pullPage.getProperties());
        } catch (Exception e) {
            MessageDialog.openInformation(getShell(), Messages
                    .getString("PullRepoWizard.malformedURL"), e.getMessage()); //$NON-NLS-1$
            MercurialEclipsePlugin.logInfo(e.getMessage(), e);
            return null;
        }
    }
}
