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

import java.util.SortedSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.team.core.diff.IDiffChangeEvent;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.internal.ui.synchronize.SyncInfoSetChangeSetCollector;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
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

	public HgChangesetsCollector(ISynchronizePageConfiguration configuration) {
		super(configuration);
		this.participant = (MercurialSynchronizeParticipant) configuration.getParticipant();
	}

	@Override
	protected void add(SyncInfo[] infos) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void initializeSets() {
		org.eclipse.team.internal.core.subscribers.ChangeSet[] sets2 = getSets();
		for (org.eclipse.team.internal.core.subscribers.ChangeSet changeSet : sets2) {
			remove(changeSet);
		}

		AbstractRemoteCache in = IncomingChangesetCache.getInstance();
		AbstractRemoteCache out = OutgoingChangesetCache.getInstance();
		int mode = getConfiguration().getMode();
		switch (mode) {
		case ISynchronizePageConfiguration.INCOMING_MODE:
			initRemote(in);
			break;
		case ISynchronizePageConfiguration.OUTGOING_MODE:
			initRemote(out);
			break;
		case ISynchronizePageConfiguration.BOTH_MODE:
			initRemote(in);
			initRemote(out);
			break;
		case ISynchronizePageConfiguration.CONFLICTING_MODE:
			initRemote(in);
			initRemote(out);
			retainConflicts();
			break;
		default:
			break;
		}

	}

	private void retainConflicts() {
		// TODO Auto-generated method stub

	}

	private void initRemote(AbstractRemoteCache cache) {
		HgRepositoryLocation repo = participant.getRepositoryLocation();
		ISynchronizationContext context = participant.getContext();
		RepositorySynchronizationScope scope = (RepositorySynchronizationScope) context.getScope();
		MercurialSynchronizeSubscriber subscriber = scope.getSubscriber();
		IProject[] projects = subscriber.getProjects();
		if(projects.length == 0){
			return;
		}
		HgRoot root;
		try {
			root = MercurialTeamProvider.getHgRoot(projects[0]);
		} catch (HgException e1) {
			MercurialEclipsePlugin.logError(e1);
			return;
		}
		String currentBranch = subscriber.getCurrentBranch(projects[0], root);
		if(MercurialSynchronizeSubscriber.isUncommitedBranch(currentBranch)) {
			if (cache instanceof IncomingChangesetCache) {
				return;
			}
			currentBranch = MercurialSynchronizeSubscriber.getRealBranchName(currentBranch);
		}
		for (IProject project : projects) {
			try {
				SortedSet<ChangeSet> changeSets = cache.getChangeSets(project, repo, currentBranch);
				for (ChangeSet changeSet : changeSets) {
					add(changeSet);
				}
			} catch (HgException e) {
				MercurialEclipsePlugin.logError(e);
			}
		}
	}

	public void handleChange(IDiffChangeEvent event) {
		initializeSets();
	}



}
