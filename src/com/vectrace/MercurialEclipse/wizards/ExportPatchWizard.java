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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.team.ui.TeamOperation;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgPatchClient;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.ui.LocationChooser.Location;
import com.vectrace.MercurialEclipse.ui.LocationChooser.LocationType;
import com.vectrace.MercurialEclipse.utils.ClipboardUtils;

public class ExportPatchWizard extends HgWizard {

	private final ExportPatchPage sourcePage;

	/**
	 * May be null
	 */
	private final ExportOptionsPage optionsPage;

	private final HgRoot root;

	private Location location;

	// operation result returned from another lambda
	private String result;

	/**
	 * True for exporting uncommitted changes. False for exporting a changeset.
	 */
	private boolean uncommittedMode;

	// constructors

	public ExportPatchWizard(List<IResource> resources, HgRoot root) {
		this(ExportPatchPage.create(resources), new ExportOptionsPage(), root);
		uncommittedMode = true;
	}

	public ExportPatchWizard(ChangeSet cs) {
		this(ExportPatchPage.create(cs), null, cs.getHgRoot());
		uncommittedMode = false;
	}

	private ExportPatchWizard(ExportPatchPage sourcePage, ExportOptionsPage optionsPage, HgRoot root) {
		super(Messages.getString("ExportPatchWizard.WindowTitle")); //$NON-NLS-1$

		setNeedsProgressMonitor(true);
		this.sourcePage = sourcePage;
		addPage(sourcePage);
		initPage(Messages.getString("ExportPatchWizard.pageDescription"), //$NON-NLS-1$
				sourcePage);

		this.optionsPage = optionsPage;
		if (optionsPage != null) {
			addPage(optionsPage);
			initPage(Messages.getString("ExportPatchWizard.optionsPageDescription"), //$NON-NLS-1$
					optionsPage);
		}
		this.root = root;
	}

	// operations

	@Override
	public boolean performFinish() {
		sourcePage.finish(null);
		try {
			location = sourcePage.getLocation();
			if (location.getLocationType() != LocationType.Clipboard && location.getFile().exists()) {
				if (!MessageDialog.openConfirm(getShell(), Messages
						.getString("ExportPatchWizard.OverwriteConfirmTitle"), //$NON-NLS-1$
						Messages.getString("ExportPatchWizard.OverwriteConfirmDescription"))) { //$NON-NLS-1$
					return false;
				}
			}

			TeamOperation operation = (uncommittedMode) ? new ExportUncomittedOperation(
					getContainer()) : new ExportChangeSetOperation(getContainer());

			result = null;
			getContainer().run(true, false, operation);
			if (result != null) {
				((optionsPage != null) ? optionsPage : sourcePage).setErrorMessage(result);

				return false;
			}
		} catch (Exception e) {
			MercurialEclipsePlugin.logError(getWindowTitle(), e);
			MercurialEclipsePlugin.showError(e.getCause());
			return false;
		}
		return true;
	}

	// inner types

	private class ExportUncomittedOperation extends TeamOperation {

		public ExportUncomittedOperation(IRunnableContext context) {
			super(context);
		}

		public void run(IProgressMonitor monitor) throws InvocationTargetException,
				InterruptedException {
			monitor.beginTask(Messages.getString("ExportPatchWizard.pageTitle"), 1); //$NON-NLS-1$
			try {
				doExport();
				if (location.getLocationType() == LocationType.Workspace) {
					location.getWorkspaceFile().refreshLocal(0, null);
				}
			} catch (Exception e) {
				result = e.getLocalizedMessage();
				MercurialEclipsePlugin.logError(Messages.getString("ExportPatchWizard.pageTitle") //$NON-NLS-1$
						+ " failed:", e); //$NON-NLS-1$
			} finally {
				monitor.done();
			}
		}

		protected void doExport() throws Exception {
			List<IResource> resources = Arrays.asList((IResource[]) sourcePage.getSelectedItems());
			List<String> options = optionsPage.getOptions();

			if (location.getLocationType() == LocationType.Clipboard) {
				ClipboardUtils.copyToClipboard(HgPatchClient.exportPatch(root, resources, options));
			} else {
				Set<IPath> paths = new HashSet<IPath>();
				for (IResource resource : resources) {
					paths.add(resource.getLocation());
				}
				HgPatchClient.exportPatch(root, paths, location.getFile(), options);
			}
		}
	}

	private class ExportChangeSetOperation extends ExportUncomittedOperation {

		public ExportChangeSetOperation(IRunnableContext context) {
			super(context);
		}

		/**
		 * @see com.vectrace.MercurialEclipse.wizards.ExportPatchWizard.ExportUncomittedOperation#doExport()
		 */
		@Override
		protected void doExport() throws Exception {
			ChangeSet cs = (ChangeSet) sourcePage.getSelectedItems()[0];

			if (location.getLocationType() == LocationType.Clipboard) {
				ClipboardUtils.copyToClipboard(HgPatchClient.exportPatch(root, cs, null));
			} else {
				HgPatchClient.exportPatch(root, cs, location.getFile(), null);
			}
		}
	}
}
