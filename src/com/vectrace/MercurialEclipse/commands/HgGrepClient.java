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

import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.search.MercurialTextSearchMatchAccess;
import com.vectrace.MercurialEclipse.utils.StringUtils;

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
	 * @param all
	 * @param res
	 * @return
	 * @throws HgException
	 */
	public static List<MercurialTextSearchMatchAccess> grep(HgRoot root, String pattern, List<IResource> files, boolean all)
			throws HgException {
		AbstractShellCommand cmd = new HgCommand("grep", root, true);
		cmd.addOptions("-nuf");
		if (all) {
			cmd.addOptions("--all");
		}
		if (StringUtils.isEmpty(pattern)) {
			return new ArrayList<MercurialTextSearchMatchAccess>();
		}

		cmd.addOptions(pattern);
		cmd.addFilesWithoutFolders(files);
		String result = cmd.executeToString(false);
		List<MercurialTextSearchMatchAccess> list = getSearchResults(root, result, all);
		return list;
	}

	/**
	 * @param root
	 * @param result
	 * @return
	 */
	private static List<MercurialTextSearchMatchAccess> getSearchResults(HgRoot root,
			String result, boolean all) {
		String[] lines = result.split("\n");
		List<MercurialTextSearchMatchAccess> list = new ArrayList<MercurialTextSearchMatchAccess>();
		for (String line : lines) {
			try {
				list.add(new MercurialTextSearchMatchAccess(root, line, all));
			} catch (HgException e) {
				// skip parsing errors, add only successful matches
			}
		}
		return list;
	}

}
