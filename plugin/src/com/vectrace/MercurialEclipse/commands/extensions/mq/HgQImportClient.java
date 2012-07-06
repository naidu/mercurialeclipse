/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian Doetsch   implementation
 * Philip Graf       Fix for importing from a patch file
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands.extensions.mq;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;

import com.aragost.javahg.commands.ExecutionException;
import com.aragost.javahg.ext.mq.QImportCommand;
import com.aragost.javahg.ext.mq.flags.QImportCommandFlags;
import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;

/**
 * @author bastian
 *
 */
public class HgQImportClient extends AbstractClient {

	/**
	 * Import a changeset with a specific name
	 */
	public static void qimport(HgRoot root, boolean force, ChangeSet changeset, String name)
			throws HgException {
		qimport(root, force, false, new ChangeSet[] { changeset }, null, name);
	}

	public static void qimport(HgRoot root, boolean force, boolean existing,
			ChangeSet[] changesets, IPath patchFile) throws HgException {
		qimport(root, force, existing, changesets, patchFile, null);
	}

	private static void qimport(HgRoot root, boolean force, boolean existing,
			ChangeSet[] changesets, IPath patchFile, String name) throws HgException {
		Assert.isNotNull(root);

		QImportCommand command = QImportCommandFlags.on(root.getRepository());

		if (force) {
			command.force();
		}

		if (name != null) {
			command.name(name);
		}

		try {
			if (changesets != null && changesets.length > 0) {
				command.rev(changesets[changesets.length - 1].getNode() + ":"
						+ changesets[0].getNode());
				command.execute();
			} else {
				if (existing) {
					command.existing();
				}

				command.execute(patchFile.toOSString());
			}
		} catch (ExecutionException ee) {
			throw new HgException(ee.getLocalizedMessage(), ee);
		}
	}
}
