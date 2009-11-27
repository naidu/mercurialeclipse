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
package com.vectrace.MercurialEclipse.synchronize.cs;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.team.core.diff.IDiffChangeEvent;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.internal.core.subscribers.IChangeSetChangeListener;
import org.eclipse.team.internal.ui.synchronize.SyncInfoSetChangeSetCollector;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.ui.IPropertyListener;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.synchronize.MercurialSynchronizeParticipant;
import com.vectrace.MercurialEclipse.synchronize.MercurialSynchronizeSubscriber;
import com.vectrace.MercurialEclipse.synchronize.RepositorySynchronizationScope;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.cache.AbstractRemoteCache;
import com.vectrace.MercurialEclipse.team.cache.IncomingChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.OutgoingChangesetCache;

/**
 * @author Andrei
 */
@SuppressWarnings("restriction")
public class HgChangesetsCollector extends SyncInfoSetChangeSetCollector {

	private final MercurialSynchronizeParticipant participant;
	private final IPropertyListener branchListener;
	private final static Set<ChangeSet> EMPTY_SET = Collections.unmodifiableSet(new HashSet<ChangeSet>());

	public HgChangesetsCollector(ISynchronizePageConfiguration configuration) {
		super(configuration);
		this.participant = (MercurialSynchronizeParticipant) configuration.getParticipant();
		branchListener = new IPropertyListener() {
			public void propertyChanged(Object source, int propId) {
				branchChanged((IProject) source);
			}
		};
		MercurialTeamProvider.addBranchListener(branchListener);
	}


	protected void branchChanged(IProject source) {
		MercurialSynchronizeSubscriber subscriber = getSubscriber();
		IProject[] projects = subscriber.getProjects();
		for (IProject project : projects) {
			if(project.equals(source)){
				Job job = new Job("Updating branch info for " + project.getName()){
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						initializeSets();
						return Status.OK_STATUS;
					}
				};
				job.schedule(100);
				return;
			}
		}
	}

	@Override
	protected void add(SyncInfo[] infos) {
		// TODO Auto-generated method stub
	}

	@Override
	protected void initializeSets() {
		Set<ChangeSet> oldSets = getChangeSets();
		Set<ChangeSet> newSets = new HashSet<ChangeSet>();


		AbstractRemoteCache in = IncomingChangesetCache.getInstance();
		AbstractRemoteCache out = OutgoingChangesetCache.getInstance();
		int mode = getConfiguration().getMode();
		switch (mode) {
		case ISynchronizePageConfiguration.INCOMING_MODE:
			newSets.addAll(initRemote(in));
			break;
		case ISynchronizePageConfiguration.OUTGOING_MODE:
			newSets.addAll(initRemote(out));
			break;
		case ISynchronizePageConfiguration.BOTH_MODE:
			newSets.addAll(initRemote(in));
			newSets.addAll(initRemote(out));
			break;
		case ISynchronizePageConfiguration.CONFLICTING_MODE:
			newSets.addAll(initRemote(in));
			newSets.addAll(initRemote(out));
			break;
		default:
			break;
		}

		if(mode == ISynchronizePageConfiguration.CONFLICTING_MODE){
			newSets = retainConflicts(newSets);
		}

		for (ChangeSet changeSet : oldSets) {
			if(!newSets.contains(changeSet)){
				remove(changeSet);
			}
		}

		for (ChangeSet changeSet : newSets) {
			if(!oldSets.contains(changeSet)){
				add(changeSet);
			}
		}
	}

	private Set<ChangeSet> retainConflicts(Set<ChangeSet> newSets) {
		// TODO let only changesets with conflicting changes
		return newSets;
	}

	private Set<ChangeSet> initRemote(AbstractRemoteCache cache) {
		MercurialSynchronizeSubscriber subscriber = getSubscriber();
		IProject[] projects = subscriber.getProjects();
		if(projects.length == 0){
			return EMPTY_SET;
		}

		String currentBranch = MercurialTeamProvider.getCurrentBranch(projects[0]);
		HgRepositoryLocation repo = participant.getRepositoryLocation();
		Set<ChangeSet> result = new HashSet<ChangeSet>();
		for (IProject project : projects) {
			try {
				result.addAll(cache.getChangeSets(project, repo, currentBranch));
			} catch (HgException e) {
				MercurialEclipsePlugin.logError(e);
			}
		}
		return result;
	}

	public MercurialSynchronizeSubscriber getSubscriber() {
		ISynchronizationContext context = participant.getContext();
		RepositorySynchronizationScope scope = (RepositorySynchronizationScope) context.getScope();
		return scope.getSubscriber();
	}

	public void handleChange(IDiffChangeEvent event) {
		initializeSets();
	}

	public Set<ChangeSet> getChangeSets() {
		Set<ChangeSet> result = new HashSet<ChangeSet>();
		org.eclipse.team.internal.core.subscribers.ChangeSet[] sets = super.getSets();
		for (org.eclipse.team.internal.core.subscribers.ChangeSet set : sets) {
			result.add((ChangeSet) set);
		}
		return result;
	}

	@Override
	public void dispose() {
		MercurialTeamProvider.removeBranchListener(branchListener);
		Object[] objects = getListeners();
		for (Object object : objects) {
			removeListener((IChangeSetChangeListener) object);
		}
		super.dispose();
	}
}
