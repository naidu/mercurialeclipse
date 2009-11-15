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

import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSet;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSetManager;
import org.eclipse.team.internal.ui.synchronize.ChangeSetCapability;
import org.eclipse.team.internal.ui.synchronize.SyncInfoSetChangeSetCollector;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizePageActionGroup;

import com.vectrace.MercurialEclipse.synchronize.MercurialSynchronizeParticipant;

/**
 * @author Andrei
 */
@SuppressWarnings("restriction")
public class HgChangeSetCapability extends ChangeSetCapability {

	private final MercurialSynchronizeParticipant participant;
	private HgChangesetsCollector changesetsCollector;

	public HgChangeSetCapability(MercurialSynchronizeParticipant mercurialSynchronizeParticipant) {
		super();
		this.participant = mercurialSynchronizeParticipant;
	}

	@Override
	public SyncInfoSetChangeSetCollector createSyncInfoSetChangeSetCollector(
			ISynchronizePageConfiguration configuration) {
		return new HgChangesetsCollector(participant, configuration);
	}

	@Override
	public ActiveChangeSetManager getActiveChangeSetManager() {
		// TODO Auto-generated method stub
		return super.getActiveChangeSetManager();
	}

	@Override
	public ActiveChangeSet createChangeSet(ISynchronizePageConfiguration configuration,
			IDiff[] diffs) {
		// TODO Auto-generated method stub
		return super.createChangeSet(configuration, diffs);
	}

	@Override
	public boolean supportsActiveChangeSets() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public SynchronizePageActionGroup getActionGroup() {
		// TODO Auto-generated method stub
		return super.getActionGroup();
	}

	@Override
	public boolean enableChangeSetsByDefault() {
		return true;
	}

	@Override
	public boolean supportsCheckedInChangeSets() {
		return true;
	}

	public HgChangesetsCollector createCheckedInChangeSetCollector(
			ISynchronizePageConfiguration configuration) {
		if(changesetsCollector == null) {
			changesetsCollector = new HgChangesetsCollector(participant, configuration);
		}
		return changesetsCollector;
	}
}
