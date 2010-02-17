/*******************************************************************************
 * Copyright (c) 2010 Bastian Doetsch and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch - Implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.history;


import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgBisectClient;
import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.commands.HgBisectClient.Status;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

/**
 * @author bastian
 *
 */
final class BisectMarkBadAction extends BisectAbstractAction {
	/**
	 * @param mercurialHistoryPage
	 */
	BisectMarkBadAction(MercurialHistoryPage mercurialHistoryPage) {
		super("Bisect: Mark selection or working directory as bad");
		this.setDescription("Bisect:Marks the selected changeset as erroneous."
				+ "\nIf bisect hadn't been started before, starts bisect.");
		super.mercurialHistoryPage = mercurialHistoryPage;
	}

	@Override
	protected void updateHistory(MercurialRevision rev, HgRoot root) {
		super.updateHistory(rev, root);
		rev.setBisectStatus(Status.BAD);
	}

	/**
	 * @param root
	 * @param changeSet
	 * @return
	 * @throws HgException
	 */
	@Override
	String callBisect(final HgRoot root, final ChangeSet changeSet) throws HgException {
		final String result = HgBisectClient.markBad(root, changeSet);
		return result;
	}

	@Override
	public boolean isEnabled() {
		try {
			final HgRoot root = MercurialTeamProvider.getHgRoot(mercurialHistoryPage.resource);
			// no selection or dirty working dir -> disable
			if (HgStatusClient.isDirty(root)) {
				return false;
			}
			return true;
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
		}
		return false;
	}
}