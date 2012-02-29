/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	   Bastian	implementation
 *     Andrei Loskutov - bug fixes
 *     Adam Berkes (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.IOException;
import java.io.InputStream;

import com.aragost.javahg.Repository;
import com.aragost.javahg.commands.flags.CatCommandFlags;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgFile;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.cache.CommandServerCache;

public class HgCatClient extends AbstractClient {

	/**
	 * Get the contents of a file at a revision
	 */
	public static InputStream getContent(HgFile hgfile) throws IOException {
		HgRoot hgRoot = hgfile.getHgRoot();
		ChangeSet cs = hgfile.getChangeSet();
		Repository repo = CommandServerCache.getInstance().get(hgRoot, cs.getBundleFile());

		return CatCommandFlags.on(repo).rev(cs.getNode()).decode().execute(hgfile.getIPath().toOSString());
	}
}
