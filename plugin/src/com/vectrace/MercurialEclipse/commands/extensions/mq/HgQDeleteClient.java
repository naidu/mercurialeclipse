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
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.Assert;

import com.aragost.javahg.commands.ExecutionException;
import com.aragost.javahg.ext.mq.Patch;
import com.aragost.javahg.ext.mq.QDeleteCommand;
import com.aragost.javahg.ext.mq.flags.QDeleteCommandFlags;
import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;

/**
 * @author bastian
 *
 */
public class HgQDeleteClient extends AbstractClient {
	public static void delete(HgRoot root, boolean keep, String patch) throws HgException {
		List<String> patches = new ArrayList<String>(1);
		patches.add(patch);
		doDelete(root, keep, null, patches);
	}

	public static void delete(HgRoot root, boolean keep, ChangeSet changeset, List<Patch> patches)
			throws HgException {
		Assert.isNotNull(patches);

		List<String> patcheNames = new ArrayList<String>(patches.size());
		for (Patch patch : patches) {
			patcheNames.add(patch.getName());
		}
		doDelete(root, keep, changeset, patcheNames);
	}

	private static void doDelete(HgRoot root, boolean keep, ChangeSet changeset,
			List<String> patches) throws HgException {
		QDeleteCommand command = QDeleteCommandFlags.on(root.getRepository());

		Assert.isNotNull(patches);

		if (keep) {
			command.keep();
		}
		if (changeset != null) {
			command.rev(changeset.getNode());
			patches = Collections.EMPTY_LIST;
		}

		try {
			command.execute(patches.toArray(new String[patches.size()]));
		} catch (ExecutionException ex) {
			throw new HgException(ex.getLocalizedMessage(), ex);
		}
	}
}
