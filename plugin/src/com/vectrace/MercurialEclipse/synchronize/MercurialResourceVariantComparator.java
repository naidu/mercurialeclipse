/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian Doetsch	implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;

import com.vectrace.MercurialEclipse.compare.RevisionNode;
import com.vectrace.MercurialEclipse.model.Branch;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.model.IHgResource;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;

/**
 * Comparator for the identity with remote content
 * @author Andrei
 */
public class MercurialResourceVariantComparator implements IResourceVariantComparator {

	private final MercurialStatusCache statusCache;

	public MercurialResourceVariantComparator() {
		statusCache = MercurialStatusCache.getInstance();
	}

	public boolean compare(IResource local, IResourceVariant repoRevision) {
		if (!statusCache.isClean(local)) {
			return false;
		}
		if(repoRevision == null){
			return true;
		}

		ChangeSet cs = ((MercurialResourceVariant)repoRevision).getRev().getHgResource().getChangeSet();

		// if this is outgoing or incoming, it can't be equal to any other changeset
		Direction direction = cs.getDirection();
		if (direction == Direction.INCOMING || direction == Direction.OUTGOING) {
			String branch = MercurialTeamProvider.getCurrentBranch(local);
			if (Branch.same(cs.getBranch(), branch)) {
				return false;
			}
		}
		// resource is clean and we compare against our local repository
		return true;
	}

	public boolean compare(IResourceVariant base, IResourceVariant remote) {
		MercurialResourceVariant mbase = (MercurialResourceVariant) base;
		MercurialResourceVariant mremote = (MercurialResourceVariant) remote;
		RevisionNode remoteRev = mremote.getRev();
		if(mbase.getRev().getHgResource() == remoteRev.getHgResource()){
			return true;
		}

		IHgResource resource = remoteRev.getHgResource();
		String remoteBranch = remoteRev.getHgResource().getChangeSet().getBranch();
		String currentBranch = MercurialTeamProvider.getCurrentBranch(resource.getHgRoot());
		if (Branch.same(currentBranch, remoteBranch)) {
			return base.getContentIdentifier().equals(remote.getContentIdentifier());
		}
		return true;
	}

	public boolean isThreeWay() {
		return true;
	}



}
