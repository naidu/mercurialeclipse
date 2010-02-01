/*******************************************************************************
 * Copyright (c) 2005-2009 Bastian Doetsch and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.ui;

import static com.vectrace.MercurialEclipse.ui.SWTWidgetHelper.*;

import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocationManager;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

/**
 * @author bastian
 */
public class HgProjectPropertyPage extends PropertyPage {
	private IProject project;
	private Group reposGroup;
	private Text defTextField;
	private HgRoot hgRoot;

	public HgProjectPropertyPage() {
		super();
	}

	@Override
	protected Control createContents(Composite parent) {
		this.project = (IProject) super.getElement();

		// create gui elements
		Composite comp = createComposite(parent, 1);

		if (!MercurialUtilities.hgIsTeamProviderFor(project, false)) {
			setMessage("This project doesn't use MercurialEclipse as Team provider.");
			return comp;
		}
		try {
			hgRoot = MercurialTeamProvider.getHgRoot(project);
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
			setMessage("Failed to find hg root for project.");
			return comp;
		}

		reposGroup = SWTWidgetHelper.createGroup(comp, "Repositories:", 1,
				GridData.FILL_HORIZONTAL);

		// each repository gets a label with its logical name and a combo for
		// setting it within MercurialEclipse
		final HgRepositoryLocationManager mgr = MercurialEclipsePlugin.getRepoManager();

		final Set<HgRepositoryLocation> allRepos = mgr.getAllRepoLocations();
		HgRepositoryLocation defLoc = mgr.getDefaultRepoLocation(hgRoot);

		Composite repoComposite = createComposite(reposGroup, 3);
		createLabel(repoComposite, "Default:");
		defTextField = createTextField(repoComposite);
		defTextField.setEditable(false);
		if(defLoc != null){
			defTextField.setText(defLoc.getLocation());
		} else {
			defTextField.setText("");
		}

		repoComposite = createComposite(reposGroup, 3);
		createLabel(repoComposite, "");
		final Combo combo = createCombo(repoComposite);
		Button defaultButton = createPushButton(repoComposite, "Set as Default", 1);
		defaultButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				for (HgRepositoryLocation repo : allRepos) {
					if(repo.getLocation().equals(combo.getText())){
						defTextField.setData(repo);
						defTextField.setText(repo.getLocation());
					}
				}
			}
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});

		int idx = -1;
		int defIndex = idx;
		for (final HgRepositoryLocation repo : allRepos) {
			idx ++;
			combo.add(repo.getLocation());
			if(defLoc != null && defLoc.equals(repo)){
				defIndex = idx;
			}
		}
		if(defIndex >= 0) {
			combo.select(defIndex);
		} else if(idx >= 0){
			combo.select(idx);
		}
		return comp;
	}

	@Override
	public boolean performOk() {
		if (hgRoot == null) {
			return super.performOk();
		}
		final HgRepositoryLocationManager mgr = MercurialEclipsePlugin.getRepoManager();
		HgRepositoryLocation defLoc = mgr.getDefaultRepoLocation(hgRoot);
		HgRepositoryLocation data = (HgRepositoryLocation) defTextField.getData();
		if (data != null
				&& (defLoc == null || !defTextField.getText().equals(defLoc.getLocation()))) {
			mgr.setDefaultRepository(hgRoot, data);
		}
		return super.performOk();
	}

	@Override
	protected void performApply() {
		this.performOk();
	}
}
