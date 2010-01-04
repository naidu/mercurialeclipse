/*******************************************************************************
 * Copyright (c) 2007-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch	- implementation
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.cache.RefreshRootJob;

public class HgRollbackClient {

	public static String rollback(final HgRoot hgRoot) throws CoreException {
		HgCommand command = new HgCommand("rollback", hgRoot, true);
		String result = command.executeToString();

		RefreshWorkspaceStatusJob job = new RefreshWorkspaceStatusJob(hgRoot);
		job.addJobChangeListener(new JobChangeAdapter(){
			@Override
			public void done(IJobChangeEvent event) {
				new RefreshRootJob("Refreshing " + hgRoot.getName(), hgRoot).schedule();
			}
		});
		job.schedule();
		return result;
	}

}
