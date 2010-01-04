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
package com.vectrace.MercurialEclipse.team;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.IDialogConstants;

import com.vectrace.MercurialEclipse.commands.HgTagClient;
import com.vectrace.MercurialEclipse.dialogs.TagDialog;
import com.vectrace.MercurialEclipse.team.cache.RefreshStatusJob;

/**
 *
 * @author Jerome Negre <jerome+hg@jnegre.org>
 *
 */public class TagAction extends SingleResourceAction {

	@Override
	protected void run(IResource resource) throws Exception {
		IProject project = resource.getProject();
		TagDialog dialog = new TagDialog(getShell(), project);

		if(dialog.open() == IDialogConstants.OK_ID) {
			HgTagClient.addTag(
					resource,
					dialog.getName(),
					dialog.getTargetRevision(),
					dialog.getUser(),
					dialog.isLocal(),
					dialog.isForced());

			new RefreshStatusJob("Refresh hg status", project).schedule();
		}
	}

}
