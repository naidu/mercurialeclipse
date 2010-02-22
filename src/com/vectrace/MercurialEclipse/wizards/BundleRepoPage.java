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
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgPathsClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocationManager;
import com.vectrace.MercurialEclipse.ui.ChangesetTable;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;

/**
 * @author bastian
 *
 */
public class BundleRepoPage extends PushPullPage {

	private Text bundleFileTextField;
	private Button bundleFileBrowseButton;
	private String bundleFile;
	private ChangesetTable baseRevTable;
	private Button baseRevCheckbox;
	private ChangeSet baseRevision;

	public BundleRepoPage(String pageName, String title, ImageDescriptor titleImage, HgRoot hgRoot) {
		super(hgRoot, pageName, title, titleImage);
		setDescription("Generate a compressed changegroup file collecting changesets \n"
				+ "not known to be in another repository.");
		showRevisionTable = false;
		showCredentials = true;
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		Composite composite = (Composite) getControl();
		Composite c = SWTWidgetHelper.createComposite(composite, 3);
		Group g = SWTWidgetHelper.createGroup(c, "Destination", 3, GridData.FILL_HORIZONTAL);
		SWTWidgetHelper.createLabel(g, "Bundle file to export to");
		this.bundleFileTextField = SWTWidgetHelper.createTextField(g);
		this.bundleFileTextField.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				if (bundleFileTextField.getText() != null
						&& bundleFileTextField.getText().length() > 0) {
					setPageComplete(true);
				}
			}
		});
		this.bundleFileBrowseButton = SWTWidgetHelper.createPushButton(g,
				"Select destination file...", 1);
		this.bundleFileBrowseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog dialog = new FileDialog(getShell());
				dialog
						.setText("Please select file to export to or enter the name of the file to be created");
				String file = dialog.open();
				if (file != null) {
					bundleFileTextField.setText(file);
					setErrorMessage(null);
					setPageComplete(true);
				}
			}
		});

		Group baseGroup = SWTWidgetHelper.createGroup(c, "Base revision", 2,
				GridData.FILL_HORIZONTAL);
		this.baseRevCheckbox = SWTWidgetHelper.createCheckBox(baseGroup,
				"Select a base revision");
		this.baseRevCheckbox.addSelectionListener(new SelectionListener() {

			private String oldRepo;

			public void widgetSelected(SelectionEvent e) {
				baseRevTable.setEnabled(baseRevCheckbox.getSelection());
				if (baseRevCheckbox.getSelection()) {
					this.oldRepo = getUrlText();
					getUrlCombo().setText("");
				} else {
					getUrlCombo().setText(oldRepo);
				}
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});
		this.baseRevTable = new ChangesetTable(baseGroup, hgRoot);
		GridData gd = SWTWidgetHelper.getFillGD(100);
		gd.heightHint = 100;
		this.baseRevTable.setLayoutData(gd);
		this.baseRevTable.setEnabled(false);

		optionGroup.moveBelow(c);
		optionGroup.setVisible(false);
		setControl(composite);
	}

	@Override
	public boolean finish(IProgressMonitor monitor) {
		this.bundleFile = bundleFileTextField.getText();
		this.baseRevision = baseRevCheckbox.getSelection() ? baseRevTable
				.getSelection() : null;
		return super.finish(monitor);
	}

	@Override
	public boolean canFlipToNextPage() {
		try {
			if (getUrlCombo().getText() != null && getUrlCombo().getText() != null) {
				OutgoingPage outgoingPage = (OutgoingPage) getNextPage();
				outgoingPage.setHgRoot(hgRoot);
				IHgRepositoryLocation loc = MercurialEclipsePlugin.getRepoManager()
						.getRepoLocation(urlCombo.getText(), getUserCombo().getText(),
								getPasswordText().getText());
				outgoingPage.setLocation(loc);
				outgoingPage.setSvn(getSvnCheckBox() != null && getSvnCheckBox().getSelection());
				setErrorMessage(null);
				return isPageComplete() && (getWizard().getNextPage(this) != null);
			}
		} catch (HgException e) {
			setErrorMessage(e.getLocalizedMessage());
		}
		return false;
	}

	@Override
	public boolean isPageComplete() {
		boolean isComplete = true;
		setErrorMessage(null);

		if (bundleFileTextField == null) {
			return false;
		}

		String bf = bundleFileTextField.getText();
		if (bf == null || bf.length() == 0) {
			isComplete = false;
			setErrorMessage("No destination file selected. Please provide a destination file.");
		}
		return isComplete && super.isPageComplete();
	}

	@Override
	protected IHgRepositoryLocation getRepoFromRoot() {
		HgRepositoryLocationManager mgr = MercurialEclipsePlugin.getRepoManager();
		IHgRepositoryLocation defaultLocation = mgr.getDefaultRepoLocation(getHgRoot());
		Set<IHgRepositoryLocation> repos = mgr.getAllRepoLocations(getHgRoot());
		if (defaultLocation == null) {
			for (IHgRepositoryLocation repo : repos) {
				if (HgPathsClient.DEFAULT_PUSH.equals(repo.getLogicalName())
						|| HgPathsClient.DEFAULT.equals(repo.getLogicalName())) {
					defaultLocation = repo;
					break;
				}
			}
		}
		return defaultLocation;
	}

	public String getBundleFile() {
		return bundleFile;
	}

	/**
	 * @return the baseRevision
	 */
	public ChangeSet getBaseRevision() {
		return baseRevision;
	}
}
