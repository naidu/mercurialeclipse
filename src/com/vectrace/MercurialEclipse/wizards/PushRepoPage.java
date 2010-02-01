/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;


import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Composite;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgPathsClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

/**
 * @author bastian
 *
 */
public class PushRepoPage extends PushPullPage {

	public PushRepoPage(String pageName, String title,
			ImageDescriptor titleImage, HgRoot hgRoot) {
		super(hgRoot, pageName, title, titleImage);
		showRevisionTable = false;
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
	}


	@Override
	public boolean finish(IProgressMonitor monitor) {
		this.force = forceCheckBox.getSelection();
		this.timeout = timeoutCheckBox.getSelection();
		return super.finish(monitor);
	}

	@Override
	public boolean canFlipToNextPage() {
		try {
			if (getUrlCombo().getText() != null
					&& getUrlCombo().getText() != null) {
				OutgoingPage outgoingPage = (OutgoingPage) getNextPage();
				outgoingPage.setHgRoot(hgRoot);
				HgRepositoryLocation loc = MercurialEclipsePlugin
						.getRepoManager().getRepoLocation(urlCombo.getText(),
								getUserCombo().getText(),
								getPasswordText()
								.getText());
				outgoingPage.setLocation(loc);
				outgoingPage.setSvn(getSvnCheckBox() != null
						&& getSvnCheckBox().getSelection());
				setErrorMessage(null);
				return isPageComplete()
						&& (getWizard().getNextPage(this) != null);
			}
		} catch (HgException e) {
			setErrorMessage(e.getLocalizedMessage());
		}
		return false;
	}

	@Override
	protected Set<HgRepositoryLocation> setDefaultLocation() {
		Set<HgRepositoryLocation> repos = super.setDefaultLocation();
		if (repos == null) {
			return null;
		}
		HgRepositoryLocation defaultLocation = MercurialEclipsePlugin.getRepoManager()
				.getDefaultRepoLocation(hgRoot);
		if (defaultLocation == null) {
			for (HgRepositoryLocation repo : repos) {
				if (HgPathsClient.DEFAULT_PUSH.equals(repo.getLogicalName())
						|| HgPathsClient.DEFAULT.equals(repo.getLogicalName())) {
					defaultLocation = repo;
					break;
				}
			}
		}

		if (defaultLocation != null) {
			getUrlCombo().setText(defaultLocation.getLocation());

			String user = defaultLocation.getUser();
			if (user != null && user.length() != 0) {
				getUserCombo().setText(user);
			}
			String password = defaultLocation.getPassword();
			if (password != null && password.length() != 0) {
				getPasswordText().setText(password);
			}
		}
		return repos;
	}

}
