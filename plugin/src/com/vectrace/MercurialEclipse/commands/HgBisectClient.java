/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Stefan Chyssler	implementation
 * Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.aragost.javahg.commands.BisectCommand;
import com.aragost.javahg.commands.BisectResult;
import com.aragost.javahg.commands.flags.BisectCommandFlags;
import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;

/**
 * @author Stefan Chyssler
 * TODO use JavaHg
 */
public final class HgBisectClient extends AbstractClient {

	public enum Status { GOOD, BAD }

	/**
	 * Marks the specified changeset as a good revision. If no changeset is specified
	 * bisect will use the "current" changeset.
	 *
	 * @param repository the repository to bisect
	 * @param good the changeset to mark as good, or null for current
	 * @return a message from the command
	 * @throws HgException
	 */
	public static BisectResult markGood(HgRoot repository, ChangeSet good)
			throws HgException {
		return mark(BisectCommandFlags.on(repository.getRepository()).good(), good);
	}

	/**
	 * Marks the specified changeset as a bad revision. If no changeset is specified
	 * bisect will use the "current" changeset.
	 *
	 * @param repository the repository to bisect
	 * @param bad the changeset to mark as bad, or null for current
	 * @return a message from the command
	 * @throws HgException
	 */
	public static BisectResult markBad(HgRoot repository, ChangeSet bad)
			throws HgException {
		return mark(BisectCommandFlags.on(repository.getRepository()).bad(), bad);
	}

	private static BisectResult mark(BisectCommand command, ChangeSet cs) {
		if (cs != null) {
			return command.execute(cs.getNode());
		}

		return command.execute();
	}

	/**
	 * Resets the bisect status for this repository. This command will not update the repository
	 * to the head.
	 * @param repository the repository to reset bisect status for
	 * @throws HgException
	 */
	public static BisectResult reset(HgRoot repository) throws HgException {
		return BisectCommandFlags.on(repository.getRepository()).reset().execute();
	}

	/**
	 * Checks if the repository is currently being bisected
	 */
	public static boolean isBisecting(HgRoot hgRoot) {
		return getStatusFile(hgRoot).exists();
	}

	/**
	 * Gets a Status by Changeset map containing marked bisect statuses.
	 * @param hgRoot
	 * @return
	 * @throws HgException
	 */
	public static Map<String, Status> getBisectStatus(HgRoot hgRoot) throws HgException {
		HashMap<String, Status> statusByRevision = new HashMap<String, Status>();
		if(!isBisecting(hgRoot)) {
			return statusByRevision;
		}
		BufferedReader reader = null;
		try {
			File file = getStatusFile(hgRoot);
			reader = new BufferedReader(new FileReader(file));
			String line = null;
			while(null != (line = reader.readLine())) {
				String[] statusChangeset = line.split("\\s"); //$NON-NLS-1$
				if(statusChangeset[0].equalsIgnoreCase("bad")) { //$NON-NLS-1$
					statusByRevision.put(statusChangeset[1].trim(), Status.BAD);
				} else {
					statusByRevision.put(statusChangeset[1].trim(), Status.GOOD);
				}
			}

		} catch (IOException e) {
			throw new HgException(e.getLocalizedMessage(), e);
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e) {
				MercurialEclipsePlugin.logError(e);
			}
		}
		return statusByRevision;
	}

	private static File getStatusFile(HgRoot hgRoot) {
		return new File(hgRoot, ".hg" + File.separator + "bisect.state");  //$NON-NLS-1$ //$NON-NLS-2$
	}
}
