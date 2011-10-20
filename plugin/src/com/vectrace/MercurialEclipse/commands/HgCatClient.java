/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	   Bastian	implementation
 *     Andrei Loskutov - bug fixes
 *     Adam Berkes (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgFile;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class HgCatClient extends AbstractClient {

	public static String getContent(IFile resource, String revision) throws HgException {
		HgRoot hgRoot = getHgRoot(resource);
		File file = ResourceUtils.getFileHandle(resource);
		AbstractShellCommand command = new HgCommand("cat", "Retrieving file contents", hgRoot, true);

		if (revision != null && revision.length() != 0) {
			command.addOptions("-r", revision); //$NON-NLS-1$

		}

		command.addOptions("--decode"); //$NON-NLS-1$
		command.addOptions(hgRoot.toRelative(file));
		command.setEncoding(ResourceUtils.getFileEncoding(resource));

		return command.executeToString();
	}

	public static String getContent(IHgFile hgfile) throws HgException {
		HgRoot hgRoot = hgfile.getHgRoot();
		AbstractShellCommand command = new HgCommand("cat", "Retrieving file contents", hgRoot, true);

		String revision = hgfile.getChangeSet() == null? null : hgfile.getChangeSet().getChangeset();
		if (revision != null && revision.length() != 0) {
			command.addOptions("-r", revision); //$NON-NLS-1$
		}

		command.addOptions("--decode"); //$NON-NLS-1$
		command.addOptions(hgfile.getIPath().toOSString());
		try {
			command.setEncoding(hgfile.getCharset());
		} catch (CoreException e) {
			MercurialEclipsePlugin.logError(e);
		}

		return command.executeToString();
	}

	public static String getContentFromBundle(IFile resource, String revision, File overlayBundle)
			throws HgException, IOException {
		Assert.isNotNull(overlayBundle);
		HgRoot hgRoot = getHgRoot(resource);
		File file = ResourceUtils.getFileHandle(resource);
		HgCommand hgCommand = new HgCommand("cat", "Retrieving file contents from bundle", hgRoot, true);

		hgCommand.setBundleOverlay(overlayBundle);

		if (revision != null && revision.length() != 0) {
			hgCommand.addOptions("-r", revision);
		}

		hgCommand.addOptions("--decode", hgRoot.toRelative(file));
		hgCommand.setEncoding(ResourceUtils.getFileEncoding(resource));

		return hgCommand.executeToString();
	}

	public static String getContentFromBundle(IHgFile hgfile, String revision, File overlayBundle)
			throws HgException, IOException {
		Assert.isNotNull(overlayBundle);
		HgRoot hgRoot = hgfile.getHgRoot();
		HgCommand hgCommand = new HgCommand("cat", "Retrieving file contents from bundle", hgRoot, true);

		hgCommand.setBundleOverlay(overlayBundle);

		if (revision != null && revision.length() != 0) {
			hgCommand.addOptions("-r", revision);
		}

		hgCommand.addOptions("--decode", hgfile.getIPath().toOSString());
		try {
			hgCommand.setEncoding(hgfile.getCharset());
		} catch (CoreException e) {
			MercurialEclipsePlugin.logError(e);
		}

		return hgCommand.executeToString();
	}

}
