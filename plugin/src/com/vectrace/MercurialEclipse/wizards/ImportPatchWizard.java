/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Stefan Groschupf          - logError
 *     Stefan C                  - Code cleanup
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.dialogs.MessageDialog;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.operations.ImportPatchOperation;

public class ImportPatchWizard extends HgWizard {

	private final ImportPatchPage sourcePage;
	private final ImportOptionsPage optionsPage;

	final HgRoot hgRoot;

	public ImportPatchWizard(HgRoot hgRoot) {
		super(Messages.getString("ImportPatchWizard.WizardTitle"));
		setNeedsProgressMonitor(true);
		this.hgRoot = hgRoot;

		sourcePage = new ImportPatchPage(hgRoot);
		addPage(sourcePage);
		initPage(Messages.getString("ImportPatchWizard.pageDescription"), sourcePage);

		optionsPage = new ImportOptionsPage();
		addPage(optionsPage);
		initPage(Messages.getString("ImportPatchWizard.optionsPageDescription"), optionsPage);
	}

	@Override
	public boolean performFinish() {
		sourcePage.finish(null);
		try {
			ImportPatchOperation operation = new ImportPatchOperation(getContainer(), hgRoot,
					sourcePage.getLocation(), optionsPage.getOptions());

			getContainer().run(true, false, operation);

			if (operation.isConflict()) {
				MessageDialog.openInformation(getShell(),
						Messages.getString("ImportPatchWizard.WizardTitle"),
						Messages.getString("ImportPatchWizard.conflict") + "\n" +  operation.getResult());
			}

			return true;
		} catch (InvocationTargetException e) {
			handleError(e.getTargetException());
		} catch (InterruptedException e) {
			handleError(e);
		}

		return false;
	}

	private void handleError(Throwable e) {
		MercurialEclipsePlugin.logError(e);

		// The user might be on either page
		optionsPage.setErrorMessage(e.getLocalizedMessage());
		sourcePage.setErrorMessage(e.getLocalizedMessage());
	}
}
