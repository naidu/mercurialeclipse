/*******************************************************************************
 * Copyright (c) 2006-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;

import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.menu.RebaseHandler;
import com.vectrace.MercurialEclipse.model.HgRoot;

public class ActionRebase extends ActionDelegate {

	/**
	 * The action has been activated. The argument of the method represents the
	 * 'real' action sitting in the workbench UI.
	 * @throws HgException
	 *
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	@Override
	public void run(IAction action) {
		try {
			final List<HgRoot> hgRoots = getSelectedHgRoots();

			if (hgRoots.size() > 0) {
				new RebaseHandler().run(hgRoots.get(0));
			}
		} catch (Exception e) {
			MercurialEclipsePlugin.logError(e);
			MercurialEclipsePlugin.showError(e);
		}
	}
}
