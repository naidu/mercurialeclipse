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

import java.util.Map;


import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgBisectClient;
import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

/**
 * @author bastian
 *
 */
final class BisectMarkGoodAction extends BisectAbstractAction {
	/**
	 * @param mercurialHistoryPage
	 */
	BisectMarkGoodAction(MercurialHistoryPage mercurialHistoryPage) {
		super("Bisect: Mark selection or working directory as good");
		this.setDescription("Bisect: Marks the selected changeset as good."
				+ "\nIf nothing is selected, stops bisect as the working directory is good");
		this.mercurialHistoryPage = mercurialHistoryPage;
	}

	@Override
	public boolean isEnabled() {
		try {
			final HgRoot root = MercurialTeamProvider.getHgRoot(this.mercurialHistoryPage.resource);
			// no selection or dirty working dir -> disable
			if (HgStatusClient.isDirty(root)) {
				return false;
			}

			// bisect not started -> disable (bisect always start with bad revision)
			Map<String, HgBisectClient.Status> bMap = HgBisectClient.getBisectStatus(root);
			if (bMap.size() == 0) {
				return false;
			}
			return true;
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
		}
		return false;
	}

	@Override
	String callBisect(HgRoot root, ChangeSet cs) throws HgException {
		String result = HgBisectClient.markGood(root, cs);
		return result;
	}
}