/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardDialog;

import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.commands.extensions.HgTransplantClient;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.wizards.TransplantWizard;

public class TransplantHandler extends RootHandler {

	@Override
	protected void run(final HgRoot hgRoot) throws CoreException {
		if (HgStatusClient.isDirty(hgRoot)) {
			MessageDialog dialog = new MessageDialog(null, Messages
					.getString("TransplantHandler.dirtyTitle"), null, Messages
					.getString("TransplantHandler.dirtyMessage"), MessageDialog.QUESTION, new String[] {
					Messages.getString("TransplantHandler.cancel"),
					Messages.getString("TransplantHandler.continue") }, 0);

			if (dialog.open() == 1) {
				HgTransplantClient.continueTransplant(hgRoot);
			}
		} else {
			TransplantWizard transplantWizard = new TransplantWizard(hgRoot);
			WizardDialog transplantWizardDialog = new WizardDialog(getShell(), transplantWizard);
			transplantWizardDialog.open();
		}
	}

}
