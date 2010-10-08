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

import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

import org.eclipse.core.runtime.NullProgressMonitor;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.extensions.HgTransplantClient;
import com.vectrace.MercurialEclipse.commands.extensions.HgTransplantClient.TransplantOptions;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.Branch;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.operations.TransplantOperation;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocationManager;

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
			HgRepositoryLocationManager repoManager = MercurialEclipsePlugin.getRepoManager();
			TransplantOptions options = createOptions();

			IHgRepositoryLocation repo;
			if (options.branch) {
				repo = null;
			} else {
				repo = repoManager.fromProperties(hgRoot, page.getProperties());
			}

			TransplantOperation runnable = new TransplantOperation(getContainer(), hgRoot,
					options, repo);

			getContainer().run(true, true, runnable);

			// It appears good. Stash the repo location.
			if(repo != null) {
				repoManager.addRepoLocation(hgRoot,	repo);
			}
		} catch (HgException e) {
			if(!(e.getCause() instanceof URISyntaxException)) {
				MercurialEclipsePlugin.logError(e);
			}
			MercurialEclipsePlugin.showError(e);
			return false;
		} catch (InvocationTargetException e) {
			MercurialEclipsePlugin.showError(e.getTargetException());
			return false;
		} catch (InterruptedException e) {
			MercurialEclipsePlugin.logError(e);
			return false;
		}
		return true;
	}

	private HgTransplantClient.TransplantOptions createOptions() {
		HgTransplantClient.TransplantOptions options = new HgTransplantClient.TransplantOptions();
		TransplantPage transplantPage = (TransplantPage) page;
		TransplantOptionsPage optionsPage = (TransplantOptionsPage) page.getNextPage();

		options.all = transplantPage.isAll();
		options.branch = transplantPage.isBranch();
		if (options.branch && Branch.isDefault(transplantPage.getBranchName())) {
			// branch name, as command parameter is default if empty
			options.branchName = Branch.DEFAULT;
		} else {
			options.branchName = transplantPage.getBranchName();
		}
		options.filter = optionsPage.getFilter();
		options.filterChangesets = optionsPage.isFilterChangesets();
		options.merge = optionsPage.isMerge();
		options.mergeNodeId = optionsPage.getMergeNodeId();
		options.nodes = transplantPage.getSelectedChangesets();
		options.prune = optionsPage.isPrune();
		options.pruneNodeId = optionsPage.getPruneNodeId();
		return options;
	}
}
