/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands.extensions.mq;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IResource;

import com.aragost.javahg.commands.ExecutionException;
import com.aragost.javahg.ext.mq.QNewCommand;
import com.aragost.javahg.ext.mq.flags.QNewCommandFlags;
import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;

/**
 * @author bastian
 *
 */
public class HgQNewClient extends AbstractClient {
	public static void createNewPatch(HgRoot root, String commitMessage, String user, String date,
			String patchName) throws HgException {
		createNewPatch(root, commitMessage, null, user, date, patchName, true);
	}

	public static void createNewPatch(HgRoot root, String commitMessage, List<IResource> resources,
			String user, String date, String patchName) throws HgException {
		createNewPatch(root, commitMessage, resources, user, date, patchName, false);
	}

	private static void createNewPatch(HgRoot root, String commitMessage,
			List<IResource> resources, String user, String date, String patchName, boolean all)
			throws HgException {
		QNewCommand command = QNewCommandFlags.on(root.getRepository());

		if (commitMessage != null && commitMessage.length() > 0) {
			command.message(commitMessage);
		}

		if (user != null && user.length() > 0) {
			command.user(user);
		} else {
			command.currentuser();
		}

		if (date != null && date.length() > 0) {
			command.date(date);
		} else {
			command.currentdate();
		}

		if (!all) {
			if (resources.isEmpty()) {
				command.exclude("*");
			}
		} else {
			resources = Collections.EMPTY_LIST;
		}

		try {
			command.execute(patchName, toFileArray(resources));
		} catch (ExecutionException ex) {
			throw new HgException(ex.getLocalizedMessage(), ex);
		}
	}
}
