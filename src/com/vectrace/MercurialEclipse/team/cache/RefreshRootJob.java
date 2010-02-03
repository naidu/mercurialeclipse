/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch           - init
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team.cache;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;

/**
 * Refreshes status, local changesets, incoming changesets and outgoing
 * changesets. If you only want to refresh the status use
 * {@link RefreshStatusJob}.
 *
 * For big repositories this can be quite slow when "withFiles" is set to true
 * in constructor.
 *
 * @author Bastian Doetsch
 *
 */
public final class RefreshRootJob extends Job {
	public static final int LOCAL = 1;
	public static final int INCOMING = 2;
	public static final int OUTGOING = 4;
	public static final int LOCAL_AND_INCOMING = LOCAL | INCOMING;
	public static final int LOCAL_AND_OUTGOING = LOCAL | OUTGOING;
	public static final int ALL = LOCAL | INCOMING | OUTGOING;

	private final static MercurialStatusCache mercurialStatusCache = MercurialStatusCache
			.getInstance();

	private final HgRoot hgRoot;
	//private final boolean withFiles;
	private final int type;

	public RefreshRootJob(String name, HgRoot hgRoot, int type) {
		super(name);
		this.hgRoot = hgRoot;
		//this.withFiles = getWithFilesProperty();
		this.type = type;
		if(hgRoot != null) {
			setRule(new HgRootRule(hgRoot));
		}
	}

	public RefreshRootJob(String name, HgRoot root) {
		this(name, root, ALL);
	}

//	private static boolean getWithFilesProperty() {
//		return Boolean.valueOf(
//				HgClients.getPreference(
//						MercurialPreferenceConstants.RESOURCE_DECORATOR_SHOW_CHANGESET,
//						"false")).booleanValue();
//	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		if(monitor.isCanceled()){
			return Status.CANCEL_STATUS;
		}

		if(MercurialEclipsePlugin.getDefault().isDebugging()) {
			System.out.println("Refresh Job for: " + hgRoot.getName());
		}

		try {
			if((type & LOCAL) != 0){
				monitor.subTask(Messages.refreshJob_LoadingLocalRevisions);
				//Set<IProject> projects = ResourceUtils.getProjects(hgRoot);
				LocalChangesetCache.getInstance().clear(hgRoot, true);
				//for (IProject project : projects) {
					// TODO fetch log info ?
					// LocalChangesetCache.getInstance().refreshAllLocalRevisions(project, true, withFiles);
				//}
				monitor.worked(1);

				monitor.subTask(Messages.refreshJob_UpdatingStatusAndVersionCache);
				mercurialStatusCache.clear(hgRoot, false);
				mercurialStatusCache.refreshStatus(hgRoot, monitor);
				monitor.worked(1);
			}
			if((type & OUTGOING) == 0 && (type & INCOMING) == 0){
				return Status.OK_STATUS;
			}
			if((type & INCOMING) != 0){
				monitor.subTask(Messages.refreshJob_LoadingIncomingRevisions + hgRoot.getName());
				IncomingChangesetCache.getInstance().clear(hgRoot, true);
				monitor.worked(1);
			}
			if((type & OUTGOING) != 0){
				monitor.subTask(Messages.refreshJob_LoadingOutgoingRevisionsFor + hgRoot.getName());
				OutgoingChangesetCache.getInstance().clear(hgRoot, true);
				monitor.worked(1);
			}
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
		} finally {
			monitor.done();
		}
		return Status.OK_STATUS;
	}

	@Override
	public boolean belongsTo(Object family) {
		return RefreshRootJob.class == family;
	}

	@Override
	public boolean shouldSchedule() {
		Job[] jobs = Job.getJobManager().find(RefreshRootJob.class);
		for (Job job : jobs) {
			if(job.getState() == WAITING){
				return false;
			}
		}
		return true;
	}
}