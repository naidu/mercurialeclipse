/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Andrei Loskutov (Intland) - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team.cache;

import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.Branch;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

/**
 * A key to identify remote cache. The key is direction-insensitive, e.g. it can be
 * used in both outgoing/incoming cache
 * @author Andrei
 */
public class RemoteKey {

	private final HgRoot root;
	private final IHgRepositoryLocation repo;

	/** never null */
	private final String branch;

	/**
	 * @param branch can be null (means all branches)
	 */
	public RemoteKey(HgRoot root, IHgRepositoryLocation repo, String branch) {
		this.root = root;
		this.repo = repo;
		this.branch = branch != null && Branch.isDefault(branch)? Branch.DEFAULT : branch;
	}

	public static RemoteKey create(IResource res, IHgRepositoryLocation repo, String branch){
		try {
			HgRoot hgRoot = MercurialTeamProvider.getHgRoot(res);
			return new RemoteKey(hgRoot, repo, branch);
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
		}
		return new RemoteKey(null, repo, Branch.DEFAULT);
	}

	public IHgRepositoryLocation getRepo() {
		return repo;
	}

	public HgRoot getRoot() {
		return root;
	}

	/**
	 * Can be null (means all branches)
	 */
	public String getBranch() {
		return branch;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((repo == null) ? 0 : repo.hashCode());
		result = prime * result + ((root == null) ? 0 : root.hashCode());
		result = prime * result + ((branch == null) ? 0 : branch.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof RemoteKey)) {
			return false;
		}
		RemoteKey other = (RemoteKey) obj;
		if (repo == null) {
			if (other.repo != null) {
				return false;
			}
		} else if (!repo.equals(other.repo)) {
			return false;
		}
		if (root == null) {
			if (other.root != null) {
				return false;
			}
		} else if (!root.equals(other.root)) {
			return false;
		}
		return Branch.same(branch, other.branch);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RemoteKey [");
		if (branch != null) {
			builder.append("branch=");
			builder.append(branch);
			builder.append(", ");
		}
		if (repo != null) {
			builder.append("repo=");
			builder.append(repo);
			builder.append(", ");
		}
		if (root != null) {
			builder.append("root=");
			builder.append(root);
		}
		builder.append("]");
		return builder.toString();
	}

}
