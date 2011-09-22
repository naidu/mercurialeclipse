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

import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.commands.AbstractShellCommand;
import com.vectrace.MercurialEclipse.commands.HgCommand;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.Patch;

/**
 * @author bastian
 *
 */
public class HgQDeleteClient extends AbstractClient {
	public static String delete(HgRoot root, boolean keep, String patch) throws HgException {
		List<String> patches = new ArrayList<String>(1);
		patches.add(patch);
		return doDelete(root, keep, null, patches);
	}

	public static String delete(HgRoot root, boolean keep,
			ChangeSet changeset, List<Patch> patches) throws HgException {
		Assert.isNotNull(patches);

		List<String> patcheNames = new ArrayList<String>(patches.size());
		for (Patch patch : patches) {
			patcheNames.add(patch.getName());
		}
		return doDelete(root, keep, changeset, patcheNames);
	}

	private static String doDelete(HgRoot root, boolean keep,
			ChangeSet changeset, List<String> patches) throws HgException {
		Assert.isNotNull(patches);
		Assert.isNotNull(root);
		AbstractShellCommand command = new HgCommand("qdelete", //$NON-NLS-1$
				"Invoking qdelete", root, true);
		command.setExecutionRule(new AbstractShellCommand.ExclusiveExecutionRule(root));
		command.addOptions("--config", "extensions.hgext.mq="); //$NON-NLS-1$ //$NON-NLS-2$

		if (keep) {
			command.addOptions("--keep"); //$NON-NLS-1$
		}
		if (changeset != null) {
			command.addOptions("--rev", changeset.getChangeset()); //$NON-NLS-1$
		} else {
			for (String patch : patches) {
				command.addOptions(patch);
			}
		}
		return command.executeToString();
	}
}
