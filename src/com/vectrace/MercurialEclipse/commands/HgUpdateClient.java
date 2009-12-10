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

import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.Branch;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.cache.RefreshJob;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class HgUpdateClient {

	public static void update(final IProject project, String revision, boolean clean)
			throws HgException {
		final String oldBranch = MercurialTeamProvider.getCurrentBranch(project);
		HgCommand command = new HgCommand("update", project, false); //$NON-NLS-1$
		command.setUsePreferenceTimeout(MercurialPreferenceConstants.UPDATE_TIMEOUT);
		if (revision != null) {
			command.addOptions("-r", revision); //$NON-NLS-1$
		}
		if (clean) {
			command.addOptions("-C"); //$NON-NLS-1$
		}
		command.executeToBytes();

		Set<IProject> projects = ResourceUtils.getProjects(command.getHgRoot());
		for (final IProject iProject : projects) {
			RefreshWorkspaceStatusJob job = new RefreshWorkspaceStatusJob(project);
			job.addJobChangeListener(new JobChangeAdapter(){
			@Override
				public void done(IJobChangeEvent event) {
					String newBranch = MercurialTeamProvider.getCurrentBranch(project);
					int refreshFlags;
					if(Branch.same(oldBranch, newBranch)){
						refreshFlags = RefreshJob.LOCAL;
					} else {
						refreshFlags = RefreshJob.ALL;
					}
					new RefreshJob("Refreshing " + iProject.getName(), iProject, refreshFlags).schedule();
				}
			});
			job.schedule();
		}
	}
}
