/*******************************************************************************
 * Copyright (c) 2010 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     John Peberdy - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.ui.TeamUI;

public class HistoryHandler extends SingleResourceHandler {

	/**
	 * @see com.vectrace.MercurialEclipse.menu.SingleResourceHandler#run(org.eclipse.core.resources.IResource)
	 */
	@Override
	protected void run(IResource resource) throws Exception {
		TeamUI.getHistoryView().showHistoryFor(resource);
	}
}
