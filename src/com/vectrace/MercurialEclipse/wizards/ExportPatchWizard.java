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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.team.ui.TeamOperation;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgPatchClient;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.ui.LocationChooser.Location;
import com.vectrace.MercurialEclipse.ui.LocationChooser.LocationType;
import com.vectrace.MercurialEclipse.utils.ClipboardUtils;

public class ExportPatchWizard extends HgWizard {

	private final ExportPatchPage sourcePage;
	private List<IResource> resources;
	private Location location;
	private final HgRoot root;
	// operation result returned from another thread
	private String result;
	private ArrayList<String> options;
	private final ExportOptionsPage optionsPage;

	public ExportPatchWizard(List<IResource> resources, HgRoot root) {
		super(Messages.getString("ExportPatchWizard.WindowTitle")); //$NON-NLS-1$
		setNeedsProgressMonitor(true);
		sourcePage = new ExportPatchPage(resources);
		addPage(sourcePage);
		initPage(Messages.getString("ExportPatchWizard.pageDescription"), //$NON-NLS-1$
				sourcePage);
		optionsPage = new ExportOptionsPage();
		addPage(optionsPage);
		initPage(Messages.getString("ExportPatchWizard.optionsPageDescription"), //$NON-NLS-1$
				optionsPage);
		this.root = root;
	}

	@Override
	public boolean performFinish() {
		sourcePage.finish(null);
		try {
			resources = sourcePage.getCheckedResources();
			options = optionsPage.getOptions();
			location = sourcePage.getLocation();
			if (location.getLocationType() != LocationType.Clipboard
					&& location.getFile().exists()) {
				if (!MessageDialog
						.openConfirm(
								getShell(),
								Messages
										.getString("ExportPatchWizard.OverwriteConfirmTitle"), //$NON-NLS-1$
								Messages
										.getString("ExportPatchWizard.OverwriteConfirmDescription"))) { //$NON-NLS-1$
					return false;
				}
			}
			ExportOperation operation = new ExportOperation(getContainer());
			result = null;
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

	class ExportOperation extends TeamOperation {

		public ExportOperation(IRunnableContext context) {
			super(context);
		}

		public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
			monitor.beginTask(Messages.getString("ExportPatchWizard.pageTitle"), 1); //$NON-NLS-1$
			try {
				doExport();
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

	public void doExport() throws Exception {
		if (location.getLocationType() == LocationType.Clipboard) {
			ClipboardUtils.copyToClipboard(HgPatchClient.exportPatch(root,
					resources, options));
		} else {
			HgPatchClient.exportPatch(root, resources, location.getFile(),
					options);
		}
		if (location.getLocationType() == LocationType.Workspace) {
			location.getWorkspaceFile().refreshLocal(0, null);
		}
	}
}
