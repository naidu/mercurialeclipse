/*******************************************************************************
 * Copyright (c) 2010 Andrei Loskutov (Intland).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov (Intland) - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.ui.TeamUI;

import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

/**
 * @author Andrei
 */
public class RootHistoryHandler extends SingleResourceHandler {

	@Override
	protected void run(IResource resource) throws Exception {
		final HgRoot hgRoot = MercurialTeamProvider.getHgRoot(resource.getProject());
		TeamUI.getHistoryView().showHistoryFor(hgRoot);
	}

}
