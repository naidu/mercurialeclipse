/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Software Balm Consulting Inc (Peter Hunnisett <peter_hge at softwarebalm dot com>) - some updates
 *     Stefan Groschupf          - logError
 *     Jerome Negre              - major rewrite
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.window.Window;
import org.eclipse.team.core.TeamException;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgAddClient;
import com.vectrace.MercurialEclipse.commands.HgCommitClient;
import com.vectrace.MercurialEclipse.dialogs.CommitDialog;
import com.vectrace.MercurialEclipse.exception.HgException;

/**
 * Attempt to have a much quicker commit. Will NOT work if the root
 * of the repository is not a project. 
 * @author Jerome Negre <jerome+hg@jnegre.org>
 */
public class CommitAction extends MultipleResourcesAction {
	@Override
	public void run(final List<IResource> resources) throws HgException{
		//FIXME let's pray that all resources are in the same project...
		IProject project = resources.get(0).getProject();
		for(IResource res : resources) {
			if(!res.getProject().equals(project)) {
				throw new HgException("All resources must be in the same project. It will be fixed soon ;)");
			}
		}

		IResource[] selectedResourceArray = resources.toArray(new IResource[0]);

		CommitDialog commitDialog = new CommitDialog(getShell(), project,
				selectedResourceArray);

		if (commitDialog.open() == Window.OK) {
			//adding new resources
			List<IResource> filesToAdd = commitDialog.getResourcesToAdd();
			HgAddClient.addResources(filesToAdd, null);

			//commit
			IResource[] resourcesToCommit = commitDialog.getResourcesToCommit();
			String messageToCommit = commitDialog.getCommitMessage();
			
			HgCommitClient.commitResources(
					Arrays.asList(resourcesToCommit),
					null, //user
					messageToCommit,
					null); //monitor
			
			
			try {
				MercurialStatusCache.getInstance().refresh(project);
			} catch (TeamException e) {
				MercurialEclipsePlugin.logError("Unable to refresh project: ",
						e);
			}
			// TODO Refresh history view TeamUI.getHistoryView().refresh();
		}
	}

}
