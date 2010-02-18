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
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

/**
 * @author bastian
 *
 */
final class BisectResetAction extends BisectAbstractAction {
	/**
	 * @param mercurialHistoryPage
	 */
	BisectResetAction(MercurialHistoryPage mercurialHistoryPage) {
		super(Messages.BisectResetAction_name);
		this.setDescription(Messages.BisectResetAction_description
				+ Messages.BisectResetAction_description2);
		this.mercurialHistoryPage = mercurialHistoryPage;
	}

	@Override
	public boolean isEnabled() {
		try {
			final HgRoot root = MercurialTeamProvider.getHgRoot(this.mercurialHistoryPage.resource);
			// no selection or dirty working dir -> disable
			if (root != null && HgBisectClient.isBisecting(root)) {
				return true;
			}
			return false;
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
		}
		return false;
	}

	@Override
	String callBisect(HgRoot root, ChangeSet cs) throws HgException {
		String result = HgBisectClient.reset(root);
		return result;
	}
}