/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others. All rights
 * reserved. This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Bastian Doetsch - implementation
 *     Zsolt Koppany (Intland)   - bug fixes
 *     Andrei Loskutov           - bug fixes
 *     Philip Graf               - proxy support
 ******************************************************************************/

package com.vectrace.MercurialEclipse.commands;

import java.io.File;

import com.aragost.javahg.Bundle;
import com.aragost.javahg.commands.IncomingCommand;
import com.aragost.javahg.commands.flags.IncomingCommandFlags;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.cache.RemoteData;
import com.vectrace.MercurialEclipse.team.cache.RemoteKey;

public class HgIncomingClient extends AbstractClient {

	/**
	 * Gets all File Revisions that are incoming and saves them in a bundle
	 * file. There can be more than one revision per file as this method obtains
	 * all new changesets.
	 *
	 * @return Never return null. Map containing all revisions of the IResources contained in the
	 *         Changesets. The sorting is ascending by date.
	 * @throws HgException
	 */
	public static RemoteData getHgIncoming(RemoteKey key) throws HgException {
		HgRoot hgRoot = key.getRoot();
		IncomingCommand command = IncomingCommandFlags.on(hgRoot.getRepository());

		if (key.getBranch() != null) {
			command.branch(key.getBranch());
		}

		if (isInsecure()) {
			command.insecure();
		}

		if (key.isAllowUnrelated()) {
			command.force();
		}

		String location = setupForRemote(key.getRepo(), command);

		Bundle bundle = command.execute(location);

		File file = bundle.getFile();
		bundle.setManageFile(false);
		file.deleteOnExit();

		return new RemoteData(key, Direction.INCOMING, bundle.getChangesets(), file);
	}
}
