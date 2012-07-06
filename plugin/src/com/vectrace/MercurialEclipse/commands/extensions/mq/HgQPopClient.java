/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		bastian	implementation
 * 		Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands.extensions.mq;

import com.aragost.javahg.commands.ExecutionException;
import com.aragost.javahg.ext.mq.QPopCommand;
import com.aragost.javahg.ext.mq.flags.QPopCommandFlags;
import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;

/**
 * @author bastian
 */
public class HgQPopClient extends AbstractClient {
	public static void popAll(HgRoot root, boolean force) throws HgException {

		QPopCommand command = QPopCommandFlags.on(root.getRepository());

		command.all();

		if (force) {
			command.force();
		}

		try {
			command.execute();
		} catch (ExecutionException ee) {
			throw new HgException(ee.getLocalizedMessage(), ee);
		}
	}

	public static void pop(HgRoot root, boolean force, String patchName) throws HgException {
		QPopCommand command = QPopCommandFlags.on(root.getRepository());

		if (force) {
			command.force();
		}

		try {
			if (!"".equals(patchName)) { //$NON-NLS-1$
				command.execute(patchName);
			} else {
				command.execute();
			}
		} catch (ExecutionException ee) {
			throw new HgException(ee.getLocalizedMessage(), ee);
		}
	}
}
