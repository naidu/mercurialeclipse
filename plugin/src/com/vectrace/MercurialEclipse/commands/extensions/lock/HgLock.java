/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	   soren					 - implementation
 *     Andrei Loskutov           - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands.extensions.lock;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.MessageDialog;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgCommand;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * @author soren
 *
 */
public class HgLock extends HgCommand {

	public HgLock(IResource resource) throws HgException {
		super("lock", "Locking a resource", getHgRoot(resource), false);
		String loc = getHgRoot().toRelative(ResourceUtils.getFileHandle(resource));
		addOptions(loc);
		String executeToString = executeToString();
		if(!executeToString.contains("Status:200")) {
			String message = executeToString.substring(executeToString.indexOf("\n"), executeToString.length());
			MessageDialog.openError(MercurialEclipsePlugin.getActiveShell(), "Lock taken", message);
		} else { //ok
			MessageDialog.openInformation(MercurialEclipsePlugin.getActiveShell(), "Lock acquired", "Successfully acquired lock");
		}
		MercurialStatusCache.getInstance().refreshStatus(resource, null);
	}

}
