/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.wizards.StripWizard;

public class StripHandler extends RootHandler {

	@Override
	protected void run(HgRoot hgRoot) {
		openWizard(hgRoot, getShell(), null);
	}

	public static void openWizard(HgRoot hgRoot, Shell shell, ChangeSet changeset) {
		StripWizard stripWizard = new StripWizard(hgRoot, changeset);
		WizardDialog dialog = new WizardDialog(shell, stripWizard);
		dialog.setBlockOnOpen(true);
		dialog.open();
	}

}
