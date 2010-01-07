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
public class BackoutWizard extends HgWizard {
	private HgRoot hgRoot;

	private BackoutWizard() {
		super(Messages.getString("BackoutWizard.title")); //$NON-NLS-1$
		setNeedsProgressMonitor(true);
	}

	public BackoutWizard(HgRoot hgRoot) {
		this();
		this.hgRoot = hgRoot;
	}

	@Override
	public void addPages() {
		super.addPages();
		page = createPage(Messages.getString("BackoutWizard.pageName"), Messages.getString("BackoutWizard.pageTitle"),null, //$NON-NLS-1$ //$NON-NLS-2$
				Messages.getString("BackoutWizard.pageDescription") ); //$NON-NLS-1$
		addPage(page);
	}

	/**
	 * Creates a ConfigurationWizardPage.
	 */
	protected HgWizardPage createPage(String pageName, String pageTitle,
			String iconPath, String description) {
		this.page = new BackoutWizardPage(pageName, pageTitle,
				MercurialEclipsePlugin.getImageDescriptor(iconPath), hgRoot);
		initPage(description, page);
		return page;
	}

	@Override
	public boolean performFinish() {
		return super.performFinish();
	}

}
