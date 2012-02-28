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

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgFile;
import com.vectrace.MercurialEclipse.model.HgRoot;

public class HgCatClient extends AbstractClient {

	/**
	 * TODO: use javahg
	 */
	public static byte[] getContent(HgFile hgfile) throws HgException, IOException {
		HgRoot hgRoot = hgfile.getHgRoot();
		ChangeSet cs = hgfile.getChangeSet();
		HgCommand command = new HgCommand("cat", "Retrieving file contents", hgRoot, true);

		command.setBundleOverlay(cs.getBundleFile());
		command.addOptions("-r", cs.getNode()); //$NON-NLS-1$
		command.addOptions("--decode"); //$NON-NLS-1$
		command.addOptions(hgfile.getIPath().toOSString());

		return command.executeToBytes();
	}
}
