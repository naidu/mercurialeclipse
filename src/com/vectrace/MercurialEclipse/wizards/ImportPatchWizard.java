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

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.team.ui.TeamOperation;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgPatchClient;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.ui.LocationChooser.Location;
import com.vectrace.MercurialEclipse.ui.LocationChooser.LocationType;
import com.vectrace.MercurialEclipse.utils.ClipboardUtils;

public class ImportPatchWizard extends HgWizard {

	private final ImportPatchPage sourcePage;
	private final ImportOptionsPage optionsPage;
	private Location location;
	private final HgRoot hgRoot;
	private String result;
	private ArrayList<String> options;

	public ImportPatchWizard(HgRoot hgRoot) {
		super(Messages.getString("ImportPatchWizard.WizardTitle")); //$NON-NLS-1$
		setNeedsProgressMonitor(true);
		this.hgRoot = hgRoot;

		sourcePage = new ImportPatchPage(hgRoot);
		addPage(sourcePage);
		initPage(Messages.getString("ImportPatchWizard.pageDescription"), //$NON-NLS-1$
				sourcePage);

		optionsPage = new ImportOptionsPage();
		addPage(optionsPage);
		initPage(Messages.getString("ImportPatchWizard.optionsPageDescription"), optionsPage); //$NON-NLS-1$
	}

	@Override
	public boolean performFinish() {
		sourcePage.finish(null);
		try {
			location = sourcePage.getLocation();
			options = optionsPage.getOptions();
			result = null;
			ImportOperation operation = new ImportOperation(getContainer());
			getContainer().run(true, false, operation);
			if (result != null) {
				optionsPage.setErrorMessage(result);
				return false;
			}
		} catch (Exception e) {
			MercurialEclipsePlugin.logError(getWindowTitle(), e);
			MercurialEclipsePlugin.showError(e.getCause());
			return false;
		}
		return true;
	}

	class ImportOperation extends TeamOperation {

		public ImportOperation(IRunnableContext context) {
			super(context);
		}

		public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
			monitor.beginTask(Messages.getString("ExportPatchWizard.pageTitle"), 1); //$NON-NLS-1$
			try {
				performOperation();
			} catch (Exception e) {
				result = e.getLocalizedMessage();
				MercurialEclipsePlugin.logError(Messages
						.getString("ExportPatchWizard.pageTitle") //$NON-NLS-1$
						+ " failed:", e); //$NON-NLS-1$
			} finally {
				monitor.done();
			}
		}

	}

	public void performOperation() throws Exception {
		if (location.getLocationType() == LocationType.Clipboard) {
			File file = null;
			try {
				file = ClipboardUtils.clipboardToTempFile("mercurial_", //$NON-NLS-1$
						".patch"); //$NON-NLS-1$
				if (file != null) {
					HgPatchClient.importPatch(hgRoot, file, options);
				}
			} finally {
				if (file != null && file.exists()) {
					boolean deleted = file.delete();
					if(!deleted){
						MercurialEclipsePlugin.logError("Failed to delete clipboard content file: " + file, null);
					}
				}
			}

		} else {
			HgPatchClient.importPatch(hgRoot, location.getFile(), options);
		}
	}
}
