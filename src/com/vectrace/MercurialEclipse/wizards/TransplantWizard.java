/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian Doetsch	implementation
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import java.net.URISyntaxException;
import java.util.Properties;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgClients;
import com.vectrace.MercurialEclipse.commands.extensions.HgTransplantClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.Branch;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

/**
 * @author bastian
 *
 */
public class TransplantWizard extends HgWizard {

	private final HgRoot hgRoot;

	public TransplantWizard(HgRoot hgRoot) {
		super(Messages.getString("TransplantWizard.title")); //$NON-NLS-1$
		setNeedsProgressMonitor(true);
		this.hgRoot = hgRoot;
	}

	@Override
	public void addPages() {
		super.addPages();
		TransplantPage transplantPage = new TransplantPage(Messages.getString("TransplantWizard.transplantPage.name"), //$NON-NLS-1$
				Messages.getString("TransplantWizard.transplantPage.title"), null, hgRoot); //$NON-NLS-1$
		initPage(Messages.getString("TransplantWizard.transplantPage.description"), //$NON-NLS-1$
				transplantPage);
		transplantPage.setShowCredentials(true);
		page = transplantPage;
		addPage(page);

		TransplantOptionsPage optionsPage = new TransplantOptionsPage(
				Messages.getString("TransplantWizard.optionsPage.name"),
				Messages.getString("TransplantWizard.optionsPage.title"), null, hgRoot); //$NON-NLS-1$
		initPage(Messages.getString("TransplantWizard.optionsPage.description"), optionsPage); //$NON-NLS-1$
		addPage(optionsPage);
	}

	@Override
	public boolean performFinish() {
		try {
			page.finish(new NullProgressMonitor());
			Properties props = page.getProperties();
			HgRepositoryLocation repo = MercurialEclipsePlugin.getRepoManager()
					.fromProperties(hgRoot, props);

			TransplantPage transplantPage = (TransplantPage) page;
			TransplantOptionsPage optionsPage = (TransplantOptionsPage) page
					.getNextPage();
			boolean isBranch = transplantPage.isBranch();
			String branchName = transplantPage.getBranchName();
			if (isBranch && Branch.isDefault(branchName)) {
				// branch name, as command parameter is default if empty
				branchName = Branch.DEFAULT;
			}
			String result = HgTransplantClient.transplant(hgRoot,
					transplantPage.getNodeIds(), repo, isBranch, branchName,
					transplantPage.isAll(), optionsPage.isMerge(), optionsPage
							.getMergeNodeId(), optionsPage.isPrune(),
					optionsPage.getPruneNodeId(), optionsPage
							.isContinueLastTransplant(), optionsPage
							.isFilterChangesets(), optionsPage.getFilter());

			if (result.length() != 0) {
				HgClients.getConsole().printMessage(result, null);
			}

			// It appears good. Stash the repo location.
			MercurialEclipsePlugin.getRepoManager().addRepoLocation(hgRoot,	repo);
		} catch (HgException e) {
			if(!(e.getCause() instanceof URISyntaxException)) {
				MercurialEclipsePlugin.logError(e);
			}
			MessageDialog.openError(Display.getCurrent().getActiveShell(), e
					.getMessage(), e.getMessage());
			// return normally (as success) to refresh project because exception means
			// not totally successful transplant but user has to see what has already
			// happened.
		}
		return true;
	}

}
