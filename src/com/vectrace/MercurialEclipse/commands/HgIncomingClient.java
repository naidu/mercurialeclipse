/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others. All rights
 * reserved. This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Bastian Doetsch - implementation
 *     Zsolt Koppany zsolt.koppany@intland.com
 *     Andrei Loskutov (Intland) - bug fixes
 ******************************************************************************/

package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.Branch;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class HgIncomingClient extends AbstractParseChangesetClient {

	/**
	 * Gets all File Revisions that are incoming and saves them in a bundle
	 * file. There can be more than one revision per file as this method obtains
	 * all new changesets.
	 *
	 * @return Never return null. Map containing all revisions of the IResources contained in the
	 *         Changesets. The sorting is ascending by date.
	 * @throws HgException
	 */
	public static Map<IPath, Set<ChangeSet>> getHgIncoming(IResource res,
			HgRepositoryLocation repository, String branch) throws HgException {
		HgCommand command = new HgCommand("incoming", getWorkingDirectory(res), //$NON-NLS-1$
				false);
		command.setUsePreferenceTimeout(MercurialPreferenceConstants.PULL_TIMEOUT);
		if (branch != null) {
			if (!Branch.isDefault(branch)) {
				HgRoot root = command.getHgRoot();
				if(HgBranchClient.isKnownRemote(root, repository, branch)) {
					command.addOptions("-r", branch);
				} else {
					// this branch is not known remote, so there can be NO incoming changes
					return new HashMap<IPath, Set<ChangeSet>>();
				}
			} else {
				// see issue 10495: there can be many "default" heads, so show all of them
				// otherwise if "-r default" is used, only unnamed at "tip" is shown, if any
			}
		}
		File bundleFile = null;
		try {
			try {
				bundleFile = File.createTempFile("bundleFile-" + //$NON-NLS-1$
						res.getProject().getName() + "-", ".tmp", null); //$NON-NLS-1$ //$NON-NLS-2$
				bundleFile.deleteOnExit();
				command.addOptions("--debug", "--style", //$NON-NLS-1$ //$NON-NLS-2$
						AbstractParseChangesetClient.getStyleFile(true)
						.getCanonicalPath(), "--bundle", bundleFile //$NON-NLS-1$
						.getCanonicalPath());
			} catch (IOException e) {
				ResourceUtils.delete(bundleFile, false);
				throw new HgException(e.getMessage(), e);
			}

			URI uri = repository.getUri();
			if (uri != null) {
				command.addOptions(uri.toASCIIString());
			} else {
				command.addOptions(repository.getLocation());
			}
			try {
				String result = command.executeToString();
				if (result.trim().endsWith("no changes found")) { //$NON-NLS-1$
					return new HashMap<IPath, Set<ChangeSet>>();
				}
				Map<IPath, Set<ChangeSet>> revisions = createMercurialRevisions(
						res, result, true,
						Direction.INCOMING, repository, bundleFile);
				return revisions;
			} catch (HgException hg) {
				if (hg.getStatus().getCode() == 1) {
					return new HashMap<IPath, Set<ChangeSet>>();
				}
				ResourceUtils.delete(bundleFile, false);
				throw new HgException("Incoming comand failed for " + res + ". " + hg.getMessage(), hg);
			}
		} finally {
			// NEVER delete bundle files, because they are used to access not yet pulled content
			// during diffs from the synchronize view
		}
	}


}
