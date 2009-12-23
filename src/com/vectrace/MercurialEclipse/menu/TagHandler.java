/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre			     - implementation
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.IDialogConstants;

import com.vectrace.MercurialEclipse.commands.HgTagClient;
import com.vectrace.MercurialEclipse.dialogs.TagDialog;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.cache.RefreshRootJob;

/**
 * @author Jerome Negre <jerome+hg@jnegre.org>
 */
public class TagHandler extends SingleResourceHandler {

	@Override
	protected void run(IResource resource) throws Exception {
		final HgRoot hgRoot = MercurialTeamProvider.getHgRoot(resource.getProject());
		TagDialog dialog = new TagDialog(getShell(), hgRoot);

		if(dialog.open() == IDialogConstants.OK_ID) {
			HgTagClient.addTag(
					resource,
					dialog.getName(),
					dialog.getTargetRevision(),
					null, //user
					dialog.isLocal(),
					dialog.isForced());
			new RefreshRootJob(Messages.getString("TagHandler.refreshing"), hgRoot, RefreshRootJob.LOCAL).schedule(); //$NON-NLS-1$
		}
	}

}
