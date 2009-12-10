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
package com.vectrace.MercurialEclipse.wizards.mq;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.operations.QImportOperation;
import com.vectrace.MercurialEclipse.views.PatchQueueView;
import com.vectrace.MercurialEclipse.wizards.HgWizard;

/**
 * @author bastian
 *
 */
public class QImportWizard extends HgWizard {
	private QImportWizardPage page = null;

	private IResource resource;

	/**
	 * @param windowTitle
	 */
	public QImportWizard(IResource resource) {
		super(Messages.getString("QImportWizard.title")); //$NON-NLS-1$
		this.resource = resource;
		setNeedsProgressMonitor(true);
		page = new QImportWizardPage(
				Messages.getString("QImportWizard.pageName"), //$NON-NLS-1$
				Messages.getString("QImportWizard.page.title"), //$NON-NLS-1$
				Messages.getString("QImportWizard.page.description"), //$NON-NLS-1$
				resource, null);
		initPage(page.getDescription(), page);
		addPage(page);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.vectrace.MercurialEclipse.wizards.HgWizard#performFinish()
	 */
	@Override
	public boolean performFinish() {
		page.setErrorMessage(null);
		ChangeSet[] changesets = page.getRevisions();
		IPath patchFile = null;
		if (changesets == null) {
			if (page.getPatchFile().getText().length()==0) {
				page.setErrorMessage(Messages.getString("QImportWizard.page.error.mustSelectChangesetOrFile")); //$NON-NLS-1$
				return false;
			}

			patchFile = new Path(page.getPatchFile().getText());
			if (!patchFile.toFile().exists()) {
				page.setErrorMessage(Messages.getString("QImportWizard.page.error.patchFileNotExists")); //$NON-NLS-1$
				return false;
			}
		}

		boolean existing = page.isExisting();
		boolean git = page.getGitCheckBox().getSelection();
		boolean force = page.getForceCheckBox().getSelection();

		QImportOperation impOperation = new QImportOperation(getContainer(),
				patchFile, changesets, existing, git, force, resource);
		try {
			getContainer().run(true, false, impOperation);
		} catch (Exception e) {
			MercurialEclipsePlugin.logError(e);
			page.setErrorMessage(e.getLocalizedMessage());
			return false;
		}
		PatchQueueView.getView().populateTable();
		return true;
	}

	/**
	 * @return the resource
	 */
	public IResource getResource() {
		return resource;
	}

	/**
	 * @param resource
	 *            the resource to set
	 */
	public void setResource(IResource resource) {
		this.resource = resource;
	}

}
