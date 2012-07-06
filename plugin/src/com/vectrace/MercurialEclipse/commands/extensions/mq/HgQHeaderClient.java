/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Philip Graf    implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands.extensions.mq;

import com.aragost.javahg.commands.ExecutionException;
import com.aragost.javahg.ext.mq.flags.QHeaderCommandFlags;
import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;

/**
 * Client for {@code qheader}.
 *
 * @author Philip Graf
 */
public class HgQHeaderClient extends AbstractClient {

	/**
	 * Returns the header of the topmost patch. This method calls {@code hg qheader} without a
	 * specified patch and returns the result.
	 *
	 * @param resource
	 * @return The header of the topmost patch. Never returns {@code null}.
	 * @throws HgException
	 *             Thrown when the Hg command cannot be executed.
	 */
	public static String getHeader(HgRoot root) throws HgException {
		try {
			return QHeaderCommandFlags.on(root.getRepository()).execute().trim();
		} catch (ExecutionException ee) {
			throw new HgException(ee.getLocalizedMessage(), ee);
		}
	}

}
