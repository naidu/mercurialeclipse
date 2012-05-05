/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch           - implementation
 *     Andrei Loskutov           - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.IOException;

import com.aragost.javahg.commands.MergeCommand;
import com.aragost.javahg.commands.flags.MergeCommandFlags;
import com.aragost.javahg.merge.MergeContext;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;

public class HgMergeClient extends AbstractClient {

	public static MergeContext merge(HgRoot hgRoot, String revision, boolean forced)
			throws HgException {
		MergeCommand command = MergeCommandFlags.on(hgRoot.getRepository());

		if (revision != null) {
			command.rev(revision);
		}

		if (forced) {
			command.force();
		}

		try {
			return command.execute();
		} catch (IOException e) {
			throw new HgException(e.getLocalizedMessage(), e);
		}
	}
}
