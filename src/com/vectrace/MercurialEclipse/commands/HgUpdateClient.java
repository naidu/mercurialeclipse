/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.Branch;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.cache.RefreshJob;
import com.vectrace.MercurialEclipse.team.cache.RefreshRootJob;

public class HgUpdateClient {

	public static void update(final HgRoot hgRoot, String revision, boolean clean)
			throws HgException {
		final String oldBranch = HgBranchClient.getActiveBranch(hgRoot);
		HgCommand command = new HgCommand("update", hgRoot, false); //$NON-NLS-1$
		command.setUsePreferenceTimeout(MercurialPreferenceConstants.UPDATE_TIMEOUT);
		if (revision != null) {
			command.addOptions("-r", revision); //$NON-NLS-1$
		}
		if (clean) {
			command.addOptions("-C"); //$NON-NLS-1$
		}
		command.executeToBytes();

		RefreshWorkspaceStatusJob job = new RefreshWorkspaceStatusJob(hgRoot);
		job.addJobChangeListener(new JobChangeAdapter(){
			@Override
			public void done(IJobChangeEvent event) {
				String newBranch = null;
				try {
					newBranch = HgBranchClient.getActiveBranch(hgRoot);
				} catch (HgException e) {
					MercurialEclipsePlugin.logError(e);
				}
				int refreshFlags;
				if(Branch.same(oldBranch, newBranch)){
					refreshFlags = RefreshJob.LOCAL;
				} else {
					refreshFlags = RefreshJob.ALL;
				}
				new RefreshRootJob("Refreshing " + hgRoot.getName(), hgRoot, refreshFlags).schedule();
			}
		});
		job.schedule();
	}
}
