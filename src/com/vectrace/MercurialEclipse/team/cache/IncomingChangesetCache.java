/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian Doetsch  implementation
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team.cache;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

/**
 * Cache for all changesets existing locally, but not present on the server
 */
public class IncomingChangesetCache extends AbstractRemoteCache {

	private static IncomingChangesetCache instance;

	private IncomingChangesetCache() {
		super(Direction.INCOMING);
	}

	public synchronized static IncomingChangesetCache getInstance() {
		if (instance == null) {
			instance = new IncomingChangesetCache();
		}
		return instance;
	}

	/**
	 * Gets the newest incoming changeset of <b>all repositories</b>.
	 *
	 * @param resource
	 *            the resource to get the changeset for
	 */
	public ChangeSet getNewestChangeSet(IResource resource) throws HgException {
		HgRoot hgRoot = MercurialTeamProvider.getHgRoot(resource);
		Set<HgRepositoryLocation> locs = MercurialEclipsePlugin.getRepoManager()
				.getAllRepoLocations(hgRoot);
		SortedSet<ChangeSet> changeSets1 = new TreeSet<ChangeSet>();
		for (HgRepositoryLocation repository : locs) {
			ChangeSet candidate = getNewestChangeSet(resource, repository, null);
			if (candidate != null) {
				changeSets1.add(candidate);
			}
		}
		if (changeSets1.size() > 0) {
			return changeSets1.last();
		}
		return null;
	}
}
