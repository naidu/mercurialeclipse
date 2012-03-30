/*******************************************************************************
 * Copyright (c) 2005-2010 Andrei Loskutov and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov          - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import com.aragost.javahg.commands.RevertCommand;
import com.aragost.javahg.commands.flags.RevertCommandFlags;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.FileStatus;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.JHgChangeSet;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.team.Messages;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * @author Andrei
 */
public class HgRevertClient extends AbstractClient {

	/**
	 * @param monitor non null
	 * @param hgRoot the root of all given resources
	 * @param resources resources to revert
	 * @param cs might be null
	 * @return a copy of file paths affected by this command, if any. Never returns null,
	 * but may return empty list. The elements of the set are absolute file paths.
	 * @throws HgException
	 */
	public static Collection<IResource> performRevert(IProgressMonitor monitor, HgRoot hgRoot,
			List<IResource> resources, JHgChangeSet cs) throws HgException {
		Set<IResource> fileSet = new HashSet<IResource>();
		monitor.subTask(Messages.getString("ActionRevert.reverting") + " " + hgRoot.getName() + "..."); //$NON-NLS-1$ //$NON-NLS-2$

		MercurialUtilities.setOfferAutoCommitMerge(true);
		if(resources.isEmpty()) {
			return resources;
		}

		String node = cs == null ? "." : cs.getNode();
		RevertCommand command = RevertCommandFlags.on(hgRoot.getRepository()).noBackup().rev(node);
		IResource firstFile = resources.get(0);

		// TODO: need to handle reverting to renamed revisions generally
		if (resources.size() == 1 && cs != null && (cs.isMoved(firstFile) || cs.isRemoved(firstFile))) {
			// String parentRevision = cs.getParentNode(0);

			if (cs.isMoved(firstFile)) {
				FileStatus status = cs.getStatus(firstFile);
				if (status != null) {
					IPath path = status.getAbsoluteCopySourcePath();
					File base = path.toFile();

					command.rev(node).execute(ResourceUtils.getFileHandle(firstFile), base);

					fileSet.add(firstFile);
					fileSet.add(ResourceUtils.convert(base));
					monitor.worked(1);
					return fileSet;
				}
			}
		}

		command.rev(node).execute(toFileArray(resources));
		fileSet.addAll(resources);
		monitor.worked(1);

		return fileSet;
	}

	public static void performRevertAll(IProgressMonitor monitor, HgRoot hgRoot) {
		RevertCommandFlags.on(hgRoot.getRepository()).noBackup().all().execute();

		MercurialUtilities.setOfferAutoCommitMerge(true);
	}
}
