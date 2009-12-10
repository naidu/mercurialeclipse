/*******************************************************************************
 * Copyright (c) 2008 MercurialEclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch			 - implementation
 *     Andrei Loskutov (Intland) - bug fixes
 ******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Set;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;
import org.eclipse.ui.statushandlers.StatusManager;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgPushPullClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.history.ChangeSetComparator;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.synchronize.MercurialSynchronizeParticipant;
import com.vectrace.MercurialEclipse.synchronize.Messages;
import com.vectrace.MercurialEclipse.synchronize.cs.ChangesetGroup;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.cache.RefreshJob;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class PushPullSynchronizeOperation extends SynchronizeModelOperation {

	private final MercurialSynchronizeParticipant participant;
	private final boolean update;
	private final boolean isPull;
	private final Object target;

	public PushPullSynchronizeOperation(ISynchronizePageConfiguration configuration,
			IDiffElement[] elements, Object target, boolean isPull, boolean update) {
		super(configuration, elements);
		this.target = target;
		this.participant = (MercurialSynchronizeParticipant) configuration.getParticipant();
		this.isPull = isPull;
		this.update = update;
	}

	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		HgRoot hgRoot = null;
		ChangeSet changeSet = null;
		if (target instanceof IProject) {
			IProject project = (IProject) target;
			try {
				hgRoot = MercurialTeamProvider.getHgRoot(project);
			} catch (HgException e) {
				MercurialEclipsePlugin.logError(e);
			}
		} else if (target instanceof ChangeSet) {
			changeSet = (ChangeSet)target;
			hgRoot = changeSet.getHgRoot();
		} if (target instanceof ChangesetGroup){
			ChangesetGroup group = (ChangesetGroup) target;
			checkChangesets(monitor, group);
			if(monitor.isCanceled()){
				return;
			}
			changeSet = Collections.min(group.getChangesets(),	new ChangeSetComparator());
			hgRoot = changeSet.getHgRoot();
		}

		if(hgRoot == null){
			String message = "No hg root found for: " + target + ". Operation cancelled.";
			Status status = new Status(IStatus.WARNING, MercurialEclipsePlugin.ID, message);
			StatusManager.getManager().handle(status, StatusManager.SHOW);
			monitor.setCanceled(true);
			return;
		}


		checkProjects(monitor, hgRoot);
		if(monitor.isCanceled()){
			return;
		}
		monitor.beginTask(getTaskName(hgRoot), 1);
		String jobName = isPull ? Messages.getString("PushPullSynchronizeOperation.PullJob")
				: Messages.getString("PushPullSynchronizeOperation.PushJob");
		new PushPullJob(jobName, hgRoot, changeSet, monitor).schedule();
	}

	private String getTaskName(HgRoot hgRoot) {
		String taskName;
		if (isPull) {
			taskName = Messages.getString("PushPullSynchronizeOperation.PullTask")
			+ " " + participant.getRepositoryLocation();
		} else {
			taskName = Messages.getString("PushPullSynchronizeOperation.PushTask")
			+ " " + hgRoot.getName();
		}
		return taskName;
	}

	private void checkChangesets(final IProgressMonitor monitor, ChangesetGroup group) {
		int csCount = group.getChangesets().size();
		if(csCount <= 1) {
			if(csCount == 0){
				// paranoia...
				monitor.setCanceled(true);
			}
			return;
		}
		final String title;
		final String message;
		if(isPull){
			title = "Hg Pull";
			message = "Pulling " + csCount + " changesets from remote repository. Continue?";
		} else {
			title = "Hg Push";
			message = "Pushing " + csCount + " changesets to remote repository. Continue?";
		}
		getShell().getDisplay().syncExec(new Runnable(){
			public void run() {
				monitor.setCanceled(!MessageDialog.openConfirm(getShell(), title, message));
			}
		});
	}

	private void checkProjects(final IProgressMonitor monitor, HgRoot hgRoot) {
		Set<IProject> projects = ResourceUtils.getProjects(hgRoot);
		if(!isPull || projects.size() <= 1) {
			if(projects.size() == 0){
				// paranoia
				monitor.setCanceled(true);
			}
			return;
		}
		final String title = "Hg Pull";
		final String message = "Pull will affect " + projects.size() + " projects in workspace. Continue?";
		getShell().getDisplay().syncExec(new Runnable(){
			public void run() {
				monitor.setCanceled(!MessageDialog.openConfirm(getShell(), title, message));
			}
		});
	}

	private final class PushPullJob extends /*NON UI!*/Job {

		private final IProgressMonitor opMonitor;
		private final HgRoot hgRoot;
		private final ChangeSet changeSet;

		private PushPullJob(String name, HgRoot hgRoot, ChangeSet changeSet, IProgressMonitor opMonitor) {
			super(name);
			this.hgRoot = hgRoot;
			this.changeSet = changeSet;
			this.opMonitor = opMonitor;
		}

		@Override
		protected IStatus run(IProgressMonitor moni) {
			HgRepositoryLocation location = participant.getRepositoryLocation();
			if(location == null){
				return Status.OK_STATUS;
			}
			if(opMonitor.isCanceled() || moni.isCanceled()){
				return Status.CANCEL_STATUS;
			}
			try {
				if (isPull) {
					boolean rebase = false;
					boolean force = false;
					boolean timeout = true;
					HgPushPullClient.pull(hgRoot, changeSet, location, update, rebase, force, timeout);
					// pull client does the refresh automatically, no extra job required here
				} else {
					HgPushPullClient.push(hgRoot, location, false, changeSet.getChangeset(), Integer.MAX_VALUE);
					Set<IProject> projects = ResourceUtils.getProjects(hgRoot);
					for (IProject project : projects) {
						if(opMonitor.isCanceled() || moni.isCanceled()){
							return Status.CANCEL_STATUS;
						}
						new RefreshJob("Refreshing " + project.getName(), project, RefreshJob.OUTGOING).schedule();
					}
				}
			} catch (HgException ex) {
				MercurialEclipsePlugin.logError(ex);
				return ex.getStatus();
			} finally {
				opMonitor.done();
			}
			return Status.OK_STATUS;
		}
	}


}
