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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;

import com.aragost.javahg.commands.ExecutionException;
import com.aragost.javahg.ext.mq.Patch;
import com.aragost.javahg.ext.mq.QFoldCommand;
import com.aragost.javahg.ext.mq.flags.QFoldCommandFlags;
import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;

/**
 * @author bastian
 *
 */
public class HgQFoldClient extends AbstractClient {

	public static void fold(HgRoot root, boolean keep, String message, String patchName)
			throws HgException {
		List<String> patchNames = new ArrayList<String>(1);

		patchNames.add(patchName);

		doFold(root, keep, message, patchNames);
	}

	public static void fold(HgRoot root, boolean keep, String message, List<Patch> patches)
			throws HgException {
		Assert.isNotNull(patches);

		List<String> patchNames = new ArrayList<String>(patches.size());

		for (Patch patch : patches) {
			patchNames.add(patch.getName());
		}

		doFold(root, keep, message, patchNames);
	}

	private static void doFold(HgRoot root, boolean keep, String message, List<String> patches)
			throws HgException {

		Assert.isNotNull(patches);
		Assert.isNotNull(root);
		QFoldCommand command = QFoldCommandFlags.on(root.getRepository());

		if (keep) {
			command.keep();
		}
		if (message != null && message.length() > 0) {
			command.message(message);
		}

		try {
			command.execute(patches.toArray(new String[patches.size()]));
		} catch (ExecutionException ex) {
			throw new HgException(ex.getLocalizedMessage(), ex);
		}
	}

	public static boolean isPatchConflict(HgException e) {
		return e.getMessage().contains("patch failed, unable to continue");
	}
}
