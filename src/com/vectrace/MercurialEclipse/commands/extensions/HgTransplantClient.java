/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch           - implementation
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands.extensions;

import java.net.URI;
import java.util.SortedSet;

import com.vectrace.MercurialEclipse.commands.AbstractShellCommand;
import com.vectrace.MercurialEclipse.commands.HgCommand;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.cache.RefreshRootJob;
import com.vectrace.MercurialEclipse.team.cache.RefreshWorkspaceStatusJob;

public final class HgTransplantClient {

	public static class TransplantOptions {
		public boolean all;
		public boolean branch;
		public boolean continueLastTransplant;
		public boolean filterChangesets;
		public boolean merge;
		public boolean prune;
		public String branchName;
		public String filter;
		public String mergeNodeId;
		public String pruneNodeId;
		/** changesets sorted in the ascending revision order */
		public SortedSet<ChangeSet> nodes;
	}

	private HgTransplantClient() {
		// hide constructor of utility class.
	}

	/**
	 * Cherrypicks given ChangeSets from repository or branch.
	 */
	public static String transplant(HgRoot hgRoot,
			IHgRepositoryLocation repo, TransplantOptions options) throws HgException {

		AbstractShellCommand command = new HgCommand("transplant", hgRoot, false); //$NON-NLS-1$
		command.setUsePreferenceTimeout(MercurialPreferenceConstants.PULL_TIMEOUT);
		command.addOptions("--config", "extensions.hgext.transplant="); //$NON-NLS-1$ //$NON-NLS-2$
		if (options.continueLastTransplant) {
			command.addOptions("--continue"); //$NON-NLS-1$
		} else {
			command.addOptions("--log"); //$NON-NLS-1$
			if (options.branch) {
				command.addOptions("--branch"); //$NON-NLS-1$
				command.addOptions(options.branchName);
				if (options.all) {
					command.addOptions("--all"); //$NON-NLS-1$
				} else {
					// the exact revision will be specified below via changeset id
				}
			} else {
				command.addOptions("--source"); //$NON-NLS-1$
				URI uri = repo.getUri();
				if (uri != null) {
					command.addOptions(uri.toASCIIString());
				} else {
					command.addOptions(repo.getLocation());
				}
			}

			if (options.prune) {
				command.addOptions("--prune"); //$NON-NLS-1$
				command.addOptions(options.pruneNodeId);
			}

			if (options.merge) {
				command.addOptions("--merge"); //$NON-NLS-1$
				command.addOptions(options.mergeNodeId);
			}

			if (!options.all && options.nodes != null && options.nodes.size() > 0) {
				for (ChangeSet node : options.nodes) {
					command.addOptions(node.getChangeset());
				}
			}

			if (options.filterChangesets) {
				command.addOptions("--filter", options.filter); //$NON-NLS-1$
			}
		}
		return new String(command.executeToBytes());
	}

	/**
	 * Continue a transplant, refreshing the workspace afterwards.
	 *
	 * @param hgRoot The repository in which to invoke hg transplant --continue
	 * @throws HgException
	 */
	public static String continueTransplant(HgRoot hgRoot) throws HgException {
		TransplantOptions options = new TransplantOptions();
		options.continueLastTransplant = true;

		try {
			return transplant(hgRoot, null, options);
		} finally {
			new RefreshWorkspaceStatusJob(hgRoot, RefreshRootJob.ALL).schedule();
		}
	}
}
