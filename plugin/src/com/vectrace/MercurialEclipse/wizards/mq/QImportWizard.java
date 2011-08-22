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

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.operations.QImportOperation;
import com.vectrace.MercurialEclipse.views.PatchQueueView;
import com.vectrace.MercurialEclipse.wizards.HgOperationWizard;

/**
 * @author bastian
 *
 */
public class QImportWizard extends HgOperationWizard {

	private final HgRoot root;

	public QImportWizard(HgRoot root) {
		super(Messages.getString("QImportWizard.title")); //$NON-NLS-1$
		this.root = root;
		setNeedsProgressMonitor(true);
		page = new QImportWizardPage(
				Messages.getString("QImportWizard.pageName"), //$NON-NLS-1$
				Messages.getString("QImportWizard.page.title"), //$NON-NLS-1$
				Messages.getString("QImportWizard.page.description"), //$NON-NLS-1$
				root, null);
		initPage(page.getDescription(), page);
		addPage(page);
	}

	@Override
	protected HgOperation initOperation() {
		final QImportWizardPage importPage = (QImportWizardPage) page;

		ChangeSet[] changesets = importPage.getRevisions();
		boolean existing = importPage.isExisting();
		IPath patchFile = null;

		if (changesets == null) {
			if (importPage.getPatchFile().getText().length()==0) {
				importPage.setErrorMessage(Messages.getString("QImportWizard.page.error.mustSelectChangesetOrFile")); //$NON-NLS-1$
				return null;
			}

			patchFile = new Path(importPage.getPatchFile().getText());
			if (!patchFile.toFile().exists() && !existing) {
				importPage.setErrorMessage(Messages.getString("QImportWizard.page.error.patchFileNotExists")); //$NON-NLS-1$
				return null;
			}
		}

		boolean force = importPage.getForceCheckBox().getSelection();

		return new QImportOperation(getContainer(), patchFile, changesets, existing, force, root);
	}

	/**
	 * @see com.vectrace.MercurialEclipse.wizards.HgOperationWizard#operationFinished()
	 */
	@Override
	protected void operationFinished() {
		super.operationFinished();
		PatchQueueView.getView().populateTable();
	}
}
