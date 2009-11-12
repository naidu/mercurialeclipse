/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov (Intland) - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.operation.IRunnableContext;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeUiJob;
import com.vectrace.MercurialEclipse.actions.HgOperation;
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

class PullOperation extends HgOperation {
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
	private final boolean doCleanUpdate;

	public PullOperation(IRunnableContext context, boolean doUpdate,
			boolean doCleanUpdate, IProject resource, boolean force, HgRepositoryLocation repo,
			ChangeSet pullRevision, boolean timeout, boolean merge,
			boolean showCommitDialog, File bundleFile, boolean forest,
			File snapFile, boolean rebase, boolean svn) {
		super(context);
		this.doUpdate = doUpdate;
		this.doCleanUpdate = doCleanUpdate;
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
			IProgressMonitor monitor) throws CoreException {
		monitor.worked(1);
		monitor.subTask(Messages.getString("PullRepoWizard.pullOperation.incoming")); //$NON-NLS-1$
		String r = Messages.getString("PullRepoWizard.pullOperation.pull.header"); //$NON-NLS-1$
		boolean updateSeparately = false;

		if (svn) {
			r += HgSvnClient.pull(resource);
			if (rebase) {
				r += HgSvnClient.rebase(resource);
			}
		} else if (bundleFile == null) {
			if (forest) {
				File forestRoot = MercurialTeamProvider.getHgRoot(resource).getParentFile();
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
			r += HgPushPullClient.pull(resource, pullRevision, getBundlePath(), false, rebase, force, timeout);
		}

		monitor.worked(1);
		saveRepo(monitor);

		if (updateSeparately) {
			runUpdate();
		}
		return r;

	}

	private String getBundlePath() throws HgException {
		String canonicalPath = null;
		try {
			canonicalPath = bundleFile.getCanonicalPath();
		} catch (IOException e) {
			String message = "Failed to get canonical bundle path for: " + bundleFile;
			MercurialEclipsePlugin.logError(message, e);
			throw new HgException(message, e);
		}
		return canonicalPath;
	}

	private void runUpdate() {
		new Job("Hg update after pull") {
			@Override
			public IStatus run(IProgressMonitor monitor1) {
				try {
					UpdateHandler updateHandler = new UpdateHandler();
					updateHandler.setCleanEnabled(doCleanUpdate);
					updateHandler.run(resource);
					return Status.OK_STATUS;
				} catch (Exception e) {
					if (e instanceof HgException) {
						// no point in complaining, since they want to merge/rebase anyway
						if ((merge || rebase) && e.getMessage().contains("crosses branches")) {
							return Status.OK_STATUS;
						}
					}
					MercurialEclipsePlugin.logError(e);
					MercurialEclipsePlugin.showError(e);
					return Status.CANCEL_STATUS;
				}
			}
		}.schedule();
	}

	private boolean saveRepo(IProgressMonitor monitor) {
		// It appears good. Stash the repo location.
		monitor.subTask(Messages.getString("PullRepoWizard.pullOperation.addRepo") + repo); //$NON-NLS-1$
		try {
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
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		try {
			monitor.beginTask(Messages.getString("PullRepoWizard.pullOperation.pulling"), 6); //$NON-NLS-1$
			output += performPull(repo, monitor);
			if (merge) {
				output += performMerge(monitor);
			}
		} catch (CoreException e) {
			throw new InvocationTargetException(e, e.getMessage());
		}
		monitor.done();
	}

	public String getOutput() {
		return output;
	}
}