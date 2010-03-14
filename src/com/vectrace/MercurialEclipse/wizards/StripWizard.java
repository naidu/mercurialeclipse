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

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.HgRoot;

/**
 * @author bastian
 */
public class StripWizard extends HgWizard {
	private HgRoot hgRoot;

	private StripWizard() {
		super(Messages.getString("StripWizard.title"));  //$NON-NLS-1$
		setNeedsProgressMonitor(true);
	}

	public StripWizard(HgRoot hgRoot) {
		this();
		this.hgRoot = hgRoot;
	}

	@Override
	public void addPages() {
		super.addPages();
		page = createPage(
				Messages.getString("StripWizard.page.name"), //$NON-NLS-1$
				Messages.getString("StripWizard.pageTitle"), //$NON-NLS-1$
				null,
				Messages.getString("StripWizard.page.description.1") //$NON-NLS-1$
						+ Messages.getString("StripWizard.page.description.2")); //$NON-NLS-1$
		addPage(page);
	}

	/**
	 * Creates a ConfigurationWizardPage.
	 */
	protected HgWizardPage createPage(String pageName, String pageTitle,
			String iconPath, String description) {
		this.page = new StripWizardPage(pageName, pageTitle,
				MercurialEclipsePlugin.getImageDescriptor(iconPath), hgRoot);
		initPage(description, page);
		return page;
	}

	@Override
	public boolean performFinish() {
		return super.performFinish();
	}

}
