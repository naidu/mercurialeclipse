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


import com.vectrace.MercurialEclipse.commands.HgBisectClient;
import com.vectrace.MercurialEclipse.commands.HgBisectClient.Status;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;

/**
 * @author bastian
 *
 */
final class BisectMarkBadAction extends BisectAbstractAction {
	/**
	 * @param mercurialHistoryPage
	 */
	BisectMarkBadAction(MercurialHistoryPage mercurialHistoryPage) {
		super(Messages.BisectMarkBadAction_name);
		this.setDescription(Messages.BisectMarkBadAction_description1
				+ Messages.BisectMarkBadAction_description2);
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
}