/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Jerome Negre		- 	Initial implementation
 * 		Bastian Doetsch	-	implemented some safeguards for the ok button
 *     	Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.IDialogConstants;

import com.vectrace.MercurialEclipse.commands.HgTagClient;
import com.vectrace.MercurialEclipse.dialogs.TagDialog;
import com.vectrace.MercurialEclipse.team.cache.RefreshJob;

/**
 *
 * @author Jerome Negre <jerome+hg@jnegre.org>
 *
 */
public class TagHandler extends SingleResourceHandler {

	@Override
	protected void run(IResource resource) throws Exception {
		IProject project = resource.getProject();
		TagDialog dialog = new TagDialog(getShell(), project);

		if (dialog.open() == IDialogConstants.OK_ID && dialog.getName() != null
				&& !dialog.getName().equals("") && dialog.getTargetRevision() != null
				&& !dialog.getTargetRevision().equals("")) {
			HgTagClient.addTag(resource, dialog.getName(), dialog.getTargetRevision(), dialog.getUser(),
					dialog.isLocal(), dialog.isForced());
			new RefreshJob(Messages.getString("TagHandler.refreshing"), project).schedule(); //$NON-NLS-1$
		}
	}

}
