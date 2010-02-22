/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.util.ArrayList;
import java.util.List;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.search.MercurialTextSearchMatchAccess;

/**
 * @author Bastian
 *
 */
public class HgGrepClient extends AbstractClient {

	/**
	 * Greps given Hg repo with params -lnu -all for given pattern
	 *
	 * @param root
	 * @param pattern
	 * @return
	 * @throws HgException
	 */
	public static List<MercurialTextSearchMatchAccess> grep(HgRoot root, String pattern)
			throws HgException {
		AbstractShellCommand cmd = new HgCommand("grep", root, true);
		cmd.setUsePreferenceTimeout(MercurialPreferenceConstants.PULL_TIMEOUT);

		cmd.addOptions("-nuf", "--all", pattern);
		String result = cmd.executeToString();
		List<MercurialTextSearchMatchAccess> list = getSearchResults(root, result);
		return list;
	}

	/**
	 * @param root
	 * @param result
	 * @return
	 */
	private static List<MercurialTextSearchMatchAccess> getSearchResults(HgRoot root,
			String result) {
		String[] lines = result.split("\n");
		List<MercurialTextSearchMatchAccess> list = new ArrayList<MercurialTextSearchMatchAccess>();
		for (String line : lines) {
			list.add(new MercurialTextSearchMatchAccess(root, line));
		}
		return list;
	}

}
