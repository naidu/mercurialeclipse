/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch           - implementation
 *     Andrei Loskutov           - bugfixes
 *     Philip Graf               - proxy support
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.util.List;

import com.aragost.javahg.Changeset;
import com.aragost.javahg.commands.OutgoingCommand;
import com.aragost.javahg.commands.flags.OutgoingCommandFlags;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.cache.RemoteData;
import com.vectrace.MercurialEclipse.team.cache.RemoteKey;

public class HgOutgoingClient extends AbstractClient {

	/**
	 * @return Not null
	 */
	public static RemoteData getOutgoing(RemoteKey key) throws HgException {
		HgRoot hgRoot = key.getRoot();
		OutgoingCommand command = OutgoingCommandFlags.on(hgRoot.getRepository());

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

		List<Changeset> changesets = command.execute(location);

		return new RemoteData(key, Direction.OUTGOING, changesets, null);
	}
}
