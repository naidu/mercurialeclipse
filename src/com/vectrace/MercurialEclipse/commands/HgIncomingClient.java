/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others. All rights
 * reserved. This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Bastian Doetsch - implementation
 *     Zsolt Koppany (Intland)   - bug fixes
 *     Andrei Loskutov (Intland) - bug fixes
 *     Philip Graf               - proxy support
 ******************************************************************************/

package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.io.IOException;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.Branch;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.cache.RemoteData;
import com.vectrace.MercurialEclipse.team.cache.RemoteKey;
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
	public static RemoteData getHgIncoming(RemoteKey key) throws HgException {
		HgRoot hgRoot = key.getRoot();
		HgCommand command = new HgCommand("incoming", hgRoot, //$NON-NLS-1$
				false);
		command.setExecutionRule(new AbstractShellCommand.ExclusiveExecutionRule(hgRoot));
		command.setUsePreferenceTimeout(MercurialPreferenceConstants.PULL_TIMEOUT);
		String branch = key.getBranch();
		if (branch != null) {
			if (!Branch.isDefault(branch)) {
				if(HgBranchClient.isKnownRemote(hgRoot, key.getRepo(), branch)) {
					command.addOptions("-r", branch);
				} else {
					// this branch is not known remote, so there can be NO incoming changes
					return new RemoteData(key, Direction.INCOMING);
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
						hgRoot.getName() + "-", ".tmp", null); //$NON-NLS-1$ //$NON-NLS-2$
				bundleFile.deleteOnExit();

				boolean computeFullStatus = MercurialEclipsePlugin.getDefault().getPreferenceStore().getBoolean(MercurialPreferenceConstants.SYNC_COMPUTE_FULL_REMOTE_FILE_STATUS);
				int style = computeFullStatus? AbstractParseChangesetClient.STYLE_WITH_FILES : AbstractParseChangesetClient.STYLE_WITH_FILES_FAST;
				// Fix (?) for issue #10859: in some cases (multiple projects under the same root)
				// hg fails to find right parent for incoming changesets,
				// therefore it seems that we must use "--debug"
				command.addOptions("--debug", "--style", //$NON-NLS-1$
						AbstractParseChangesetClient.getStyleFile(style)
						.getCanonicalPath(), "--bundle", bundleFile //$NON-NLS-1$
						.getCanonicalPath());
			} catch (IOException e) {
				ResourceUtils.delete(bundleFile, false);
				throw new HgException(e.getMessage(), e);
			}

			addRepoToHgCommand(key.getRepo(), command);

			try {
				String result = command.executeToString();
				if (result.trim().endsWith("no changes found")) { //$NON-NLS-1$
					return new RemoteData(key, Direction.INCOMING);
				}
				RemoteData revisions = createRemoteRevisions(key, result, Direction.INCOMING, bundleFile);
				return revisions;
			} catch (HgException hg) {
				if (hg.getStatus().getCode() == 1) {
					return new RemoteData(key, Direction.INCOMING);
				}
				ResourceUtils.delete(bundleFile, false);
				throw new HgException("Incoming comand failed for " + key.getRoot() + ". " + hg.getMessage(), hg);
			}
		} finally {
			// NEVER delete bundle files, because they are used to access not yet pulled content
			// during diffs from the synchronize view
			// TODO Andrei: it would make sense to track created bundle files and delete
			// them on the next incoming operation for same repo/branch pair
		}
	}
}
