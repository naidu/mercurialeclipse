/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.wizards.mq.QDeleteWizard;

/**
 * @author bastian
 *
 */
public class QDeleteHandler extends RootHandler {

	/**
	 * @see com.vectrace.MercurialEclipse.menu.RootHandler#run(com.vectrace.MercurialEclipse.model.HgRoot)
	 */
	@Override
	protected void run(HgRoot root) {
		openWizard(root, getShell(), true);
	}

	public static void openWizard(HgRoot root, Shell shell, boolean bShowRevSelector) {
		QDeleteWizard wizard = new QDeleteWizard(root, bShowRevSelector);
		WizardDialog dialog = new WizardDialog(shell, wizard);
		dialog.setBlockOnOpen(true);
		dialog.open();
	}
}
