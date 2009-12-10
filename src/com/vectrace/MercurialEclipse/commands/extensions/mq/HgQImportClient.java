/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian Doetsch	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands.extensions.mq;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;

import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.commands.AbstractShellCommand;
import com.vectrace.MercurialEclipse.commands.HgCommand;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;

/**
 * @author bastian
 *
 */
public class HgQImportClient extends AbstractClient {
	public static String qimport(IResource resource, boolean force, boolean git, boolean existing,
			ChangeSet[] changesets, IPath patchFile) throws HgException {
		Assert.isNotNull(resource);
		AbstractShellCommand command = new HgCommand("qimport", //$NON-NLS-1$
				getWorkingDirectory(resource), true);
		command.setUsePreferenceTimeout(MercurialPreferenceConstants.CLONE_TIMEOUT);

		command.addOptions("--config", "extensions.hgext.mq="); //$NON-NLS-1$ //$NON-NLS-2$

		if (force) {
			command.addOptions("--force"); //$NON-NLS-1$
		}

		if (git) {
			command.addOptions("--git"); //$NON-NLS-1$
		}

		if (changesets != null && changesets.length>0) {
			command.addOptions("--rev", changesets[changesets.length-1].getChangeset()+ ":" +changesets[0].getChangeset() );                        //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			Assert.isNotNull(patchFile);
			if (existing) {
				command.addOptions("--existing"); //$NON-NLS-1$
			} else {
				command.addOptions("--name", patchFile.toOSString()); //$NON-NLS-1$
			}
		}

		return command.executeToString();
	}
}
