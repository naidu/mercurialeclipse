/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     steeven
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

import com.vectrace.MercurialEclipse.commands.RefreshWorkspaceStatusJob;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.cache.RefreshRootJob;
import com.vectrace.MercurialEclipse.wizards.ImportPatchWizard;

public class ImportPatchHandler extends SingleResourceHandler {

	@Override
	protected void run(IResource resource) throws Exception {
		openWizard(resource, getShell());
	}

	public void openWizard(IResource resource, Shell shell) throws Exception {
		final HgRoot hgRoot = MercurialTeamProvider.getHgRoot(resource.getProject());
		ImportPatchWizard wizard = new ImportPatchWizard(hgRoot);
		WizardDialog dialog = new WizardDialog(shell, wizard);
		dialog.setBlockOnOpen(true);
		if (Window.OK == dialog.open()) {
			RefreshWorkspaceStatusJob job = new RefreshWorkspaceStatusJob(hgRoot);
			job.addJobChangeListener(new JobChangeAdapter(){
				@Override
				public void done(IJobChangeEvent event) {
					new RefreshRootJob("Refreshing " + hgRoot.getName(), hgRoot, RefreshRootJob.LOCAL).schedule();
				}
			});
			job.schedule();
		}
	}
}
