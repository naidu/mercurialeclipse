/*******************************************************************************
 * Copyright (c) 2008 MercurialEclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch			 - implementation
 *     Andrei Loskutov           - bug fixes
 ******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

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
import com.vectrace.MercurialEclipse.menu.PushHandler;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.synchronize.MercurialSynchronizeParticipant;
import com.vectrace.MercurialEclipse.synchronize.MercurialSynchronizeSubscriber;
import com.vectrace.MercurialEclipse.synchronize.Messages;
import com.vectrace.MercurialEclipse.synchronize.cs.ChangesetGroup;
import com.vectrace.MercurialEclipse.synchronize.cs.RepositoryChangesetGroup;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.cache.RefreshRootJob;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class PushPullSynchronizeOperation extends SynchronizeModelOperation {

	private final MercurialSynchronizeParticipant participant;
	private final boolean update;
	private final boolean isPull;
	private final Set<? extends Object> targets;

	public PushPullSynchronizeOperation(ISynchronizePageConfiguration configuration,
			IDiffElement[] elements, Set<? extends Object> target, boolean isPull, boolean update) {
		super(configuration, elements);
		this.targets = target;
		this.participant = (MercurialSynchronizeParticipant) configuration.getParticipant();
		this.isPull = isPull;
		this.update = update;
	}

	/**
	 * Collect roots for the given object
	 * @param monitor The progress monitor
	 * @param hgRoots (Output) Collected roots are added to this
	 * @param changeSet Current changeset.
	 * @param target The object to get roots from
	 * @return The new value for the selected changeset
	 */
	private ChangeSet getRoots(final IProgressMonitor monitor, final Set<HgRoot> hgRoots,
			ChangeSet changeSet, final Object target) {
		HgRoot hgRoot = null;
		if (target instanceof IProject) {
			hgRoot = MercurialTeamProvider.getHgRoot((IProject) target);
		} else if (target instanceof ChangeSet) {
			changeSet = (ChangeSet) target;
			hgRoot = changeSet.getHgRoot();
		}
		if (target instanceof ChangesetGroup) {
			ChangesetGroup group = (ChangesetGroup) target;
			checkChangesets(monitor, group);
			if(monitor.isCanceled()){
				return null;
			}

			// Alternative: Find all the heads and push/pull them individually (without doing
			// workspace refreshes in between)
			changeSet = null;
			hgRoot = group.getChangesets().iterator().next().getHgRoot();
		}
		if (target instanceof RepositoryChangesetGroup) {
			RepositoryChangesetGroup group = (RepositoryChangesetGroup) target;
			checkChangesets(monitor, group);
			if (monitor.isCanceled()) {
				return null;
			}
			if (isPull) {
				changeSet = null;
				hgRoot = group.getIncoming().getChangesets().iterator().next().getHgRoot();
			} else {
				changeSet = Collections.min(group.getOutgoing().getChangesets(),new ChangeSetComparator());
				hgRoot = changeSet.getHgRoot();
			}
		}
		if(hgRoot == null){
			String message = "No hg root found for: " + target + ". Operation cancelled.";
			Status status = new Status(IStatus.WARNING, MercurialEclipsePlugin.ID, message);
			StatusManager.getManager().handle(status, StatusManager.SHOW);
			monitor.setCanceled(true);
			return null;
		}
		hgRoots.add(hgRoot);
		return changeSet;
	}

	public void run(IProgressMonitor monitor) throws InvocationTargetException,	InterruptedException {
		Set<HgRoot> hgRoots = new TreeSet<HgRoot>();
		ChangeSet changeSet = null; // ok to have changeset outside loop, if it's not null in the end we have only selected one thing, and the loop below runs only once.
		// TODO: logic wrt 'changeset' is not correct
		// TODO Make a changeRequest class of a pair with hgroot and changeset(s)
		for (Object target : targets) {
			changeSet = getRoots(monitor, hgRoots, changeSet, target);
			if(monitor.isCanceled()){
				return;
			}
		}
		//Inform the user on how many projects is about to update
		for(HgRoot root: hgRoots) {
			checkProjects(monitor, root);
			if(monitor.isCanceled()){
				return;
			}
		}
		monitor.beginTask(getTaskName(hgRoots), 1);
		String jobName = isPull ? Messages.getString("PushPullSynchronizeOperation.PullJob")
				: Messages.getString("PushPullSynchronizeOperation.PushJob");
		PushPullJob job = new PushPullJob(jobName, hgRoots, changeSet, monitor);

		if (changeSet == null)
		{
			for(HgRoot hgRoot : hgRoots) {
				job.setBranch(hgRoot, MercurialSynchronizeSubscriber.getSyncBranch(hgRoot));
			}
		}

		job.schedule();
	}

	private String getTaskName(Set<HgRoot> hgRoot) {
		String taskName;

		// TODO: use repo location map information better
		if (isPull) {
			taskName = Messages.getString("PushPullSynchronizeOperation.PullTask") + " ";
			for(IHgRepositoryLocation loc : participant.getRepositoryLocations().getLocations()) {
				taskName += loc.getLocation() + " ";
			}
		} else {
			taskName = Messages.getString("PushPullSynchronizeOperation.PushTask")+ " ";
			for(HgRoot root : hgRoot) {
				taskName += root.getName()+ " ";
			}
		}
		return taskName;
	}

	private void checkChangesets(final IProgressMonitor monitor, ChangesetGroup group) {
		checkChangesets(monitor, group.getChangesets().size());
	}

	private void checkChangesets(final IProgressMonitor monitor, RepositoryChangesetGroup group) {
		int csCount;
		if (isPull) {
			csCount = group.getIncoming().getChangesets().size();
		} else {
			csCount = group.getOutgoing().getChangesets().size();
		}
		checkChangesets(monitor, csCount);
	}

	private void checkChangesets(final IProgressMonitor monitor, int csCount)
	{
		if(csCount < 1){
			// paranoia...
			monitor.setCanceled(true);
			return;
		}
		final String title;
		final String message;
		if(isPull){
			title = "Hg Pull";
			message = "Pulling " + csCount + " changesets (or more) from the remote repository.\n"
					+ "The pull will fetch the *latest* version available remote.\n" + "Continue?";
		} else {
			if(csCount == 1){
				return;
			}
			title = "Hg Push";
			message = "Pushing " + csCount + " changesets to the remote repository. Continue?";
		}
		getShell().getDisplay().syncExec(new Runnable(){
			public void run() {
				if (!MercurialEclipsePlugin.showDontShowAgainConfirmDialog(title, message,
						MessageDialog.CONFIRM,
						MercurialPreferenceConstants.PREF_SHOW_PULL_WARNING_DIALOG, getShell())) {
					monitor.setCanceled(true);
				}
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
				if (!MercurialEclipsePlugin
						.showDontShowAgainConfirmDialog(title, message, MessageDialog.CONFIRM,
								MercurialPreferenceConstants.PREF_SHOW_MULTIPLE_PROJECTS_DIALOG,
								getShell())) {
					monitor.setCanceled(true);
				}
			}
		});
	}

	private final class PushPullJob extends /*NON UI!*/Job {

		private final IProgressMonitor opMonitor;
		private final Set<HgRoot> hgRoots;
		private final ChangeSet changeSet;
		private final HashMap<HgRoot,String> branches = new HashMap<HgRoot, String>();

		/**
		 * @param name Human readable name
		 * @param hgRoot The hg root
		 * @param changeSet The changeset, may be null to push/pull everything
		 * @param opMonitor The progress monitor
		 */
		private PushPullJob(String name, Set<HgRoot> hgRoot, ChangeSet changeSet, IProgressMonitor opMonitor) {
			super(name);
			this.hgRoots = hgRoot;
			this.changeSet = changeSet;
			this.opMonitor = opMonitor;
		}

		/**
		 * @param branch The branch name, or null for all/any
		 */
		public void setBranch(HgRoot root, String branch) {
			this.branches.put(root, branch);
		}

		@Override
		protected IStatus run(IProgressMonitor moni) {
			try {
				for (HgRoot hgRoot : hgRoots) {
					IStatus stat = run(moni, hgRoot);

					if (stat != null) {
						return stat;
					}
				}
			} finally {
				opMonitor.done();
			}
			return Status.OK_STATUS;
		}

		protected IStatus run(IProgressMonitor moni, final HgRoot hgRoot) {
			String branch = branches.get(hgRoot);
			IHgRepositoryLocation location = participant.getRepositoryLocation(hgRoot);
			if(location == null){
				return Status.OK_STATUS;
			}
			// re-validate the location as it might have changed credentials...
			try {
				location = MercurialEclipsePlugin.getRepoManager().getRepoLocation(location.getLocation());
			} catch (HgException e1) {
				MercurialEclipsePlugin.logError(e1);
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
					HgPushPullClient.pull(hgRoot, changeSet, location, update, rebase, force, timeout, false, branch);
					// pull client does the refresh automatically, no extra job required here
				} else {
					HgPushPullClient.push(hgRoot, location, false, changeSet, Integer.MAX_VALUE, branch);
					new RefreshRootJob(hgRoot, RefreshRootJob.OUTGOING).schedule();
				}
				return null;
			} catch (final HgException ex) {
				MercurialEclipsePlugin.logError(ex);
				if(!isPull){
					// try to recover: open the default dialog, where user can change some
					// settings like password/force flag etc (issue #10720)
					MercurialEclipsePlugin.getStandardDisplay().asyncExec(new Runnable() {
						public void run() {
							try {
								PushHandler handler = new PushHandler();

								handler.setInitialMessage(Messages
										.getString("PushPullSynchronizeOperation.PushFailed") + " "
										+ ex.getConciseMessage());
								handler.run(hgRoot);
							} catch (Exception e) {
								MercurialEclipsePlugin.logError(e);
							}
						}
					});
				}
				return Status.CANCEL_STATUS;
			}
		}
	}


}
