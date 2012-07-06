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

import java.util.List;

import org.eclipse.core.runtime.Assert;

import com.aragost.javahg.commands.ExecutionException;
import com.aragost.javahg.ext.mq.Patch;
import com.aragost.javahg.ext.mq.flags.QFinishCommandFlags;
import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;

/**
 * @author bastian
 *
 */
public class HgQFinishClient extends AbstractClient {


	public static void finish(HgRoot root, List<Patch> revs) throws HgException {
		Assert.isTrue(revs.size() > 0);

		String[] patches = new String[revs.size()];
		int i = 0;

		for (Patch p : revs) {
			patches[i++] = p.getName();
		}

		try {
			QFinishCommandFlags.on(root.getRepository()).execute(patches);
		} catch (ExecutionException ex) {
			throw new HgException(ex.getLocalizedMessage(), ex);
		}
	}

	/**
	 * Calls qfinish -a
	 */
	public static void finishAllApplied(HgRoot root) throws HgException {
		try {
			QFinishCommandFlags.on(root.getRepository()).applied().execute();
		} catch (ExecutionException ex) {
			throw new HgException(ex.getLocalizedMessage(), ex);
		}
	}
}
