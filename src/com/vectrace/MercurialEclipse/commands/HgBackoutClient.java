/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	   Bastian	implementation
 *     Andrei Loskutov (Intland) - bug fixes
 *     Adam Berkes (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.cache.RefreshJob;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * @author bastian
 *
 */
public class HgBackoutClient {

	/**
	 * Backout of a changeset
	 *
	 * @param project
	 *            the project
	 * @param backoutRevision
	 *            revision to backout
	 * @param merge
	 *            flag if merge with a parent is wanted
	 * @param msg
	 *            commit message
	 */
	public static String backout(final IProject project, ChangeSet backoutRevision,
			boolean merge, String msg, String user) throws CoreException {

		HgCommand command = new HgCommand("backout", project, true); //$NON-NLS-1$
		boolean useExternalMergeTool = Boolean.valueOf(
				HgClients.getPreference(MercurialPreferenceConstants.PREF_USE_EXTERNAL_MERGE,
						"false")).booleanValue(); //$NON-NLS-1$

		if (!useExternalMergeTool) {
			command.addOptions("--config", "ui.merge=simplemerge"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		command.addOptions("-r", backoutRevision.getChangeset(), "-m", msg, "-u", user);//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (merge) {
			command.addOptions("--merge"); //$NON-NLS-1$
		}

		String result = command.executeToString();

		Set<IProject> projects = ResourceUtils.getProjects(command.getHgRoot());
		for (final IProject iProject : projects) {
			RefreshWorkspaceStatusJob job = new RefreshWorkspaceStatusJob(iProject);
			job.addJobChangeListener(new JobChangeAdapter(){
			@Override
				public void done(IJobChangeEvent event) {
					new RefreshJob("Refreshing " + iProject.getName(), iProject).schedule();
				}
			});
			job.schedule();
		}
		return result;
	}

}
