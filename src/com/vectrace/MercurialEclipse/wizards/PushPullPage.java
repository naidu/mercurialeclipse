/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *     Adam Berkes (Intland) - repository location handling
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.team.ResourceProperties;
import com.vectrace.MercurialEclipse.ui.ChangesetTable;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;

/**
 * @author bastian
 *
 */
public class PushPullPage extends ConfigurationWizardMainPage {

	protected HgRoot hgRoot;
	protected Button forceCheckBox;
	protected boolean force;
	protected ChangesetTable changesetTable;
	protected String revision;
	protected Button revCheckBox;
	protected Button timeoutCheckBox;
	protected boolean timeout;
	protected Group optionGroup;
	protected boolean showRevisionTable = true;
	protected boolean showForce = true;
	protected Button forestCheckBox;
	protected boolean showForest = false;
	protected Combo snapFileCombo;
	protected Button snapFileButton;
	protected boolean showSnapFile = true;
	protected boolean showSvn = false;
	protected Button svnCheckBox;

	public PushPullPage(HgRoot hgRoot, String pageName, String title,
			ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
		this.hgRoot = hgRoot;
		try {
			setShowForest(true);
			setShowSvn(true);
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
			setErrorMessage(e.getMessage());
		}
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		Composite composite = (Composite) getControl();

		// now the options
		optionGroup = SWTWidgetHelper.createGroup(composite, Messages
				.getString("PushRepoPage.optionGroup.title")); //$NON-NLS-1$
		this.timeoutCheckBox = SWTWidgetHelper.createCheckBox(optionGroup,
				getTimeoutCheckBoxLabel());

		if (showForce) {
			this.forceCheckBox = SWTWidgetHelper.createCheckBox(optionGroup,
					getForceCheckBoxLabel());
		}
		if (showRevisionTable) {
			createRevisionTable(composite);
		}

		createExtensionControls();

		setDefaultLocation();
	}

	private void createExtensionControls() {
		if (showForest) {
			this.forestCheckBox = SWTWidgetHelper.createCheckBox(optionGroup,
					Messages.getString("PushPullPage.option.forest")); //$NON-NLS-1$

			if (showSnapFile) {
				Composite c = SWTWidgetHelper.createComposite(optionGroup, 3);
				final Label forestLabel = SWTWidgetHelper.createLabel(c,
						Messages.getString("PushPullPage.snapfile.label")); //$NON-NLS-1$
				forestLabel.setEnabled(false);
				this.snapFileCombo = createEditableCombo(c);
				snapFileCombo.setEnabled(false);
				this.snapFileButton = SWTWidgetHelper.createPushButton(c,
						Messages.getString("PushPullPage.snapfile.browse"), 1); //$NON-NLS-1$
				snapFileButton.setEnabled(false);
				this.snapFileButton
						.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent e) {
								FileDialog dialog = new FileDialog(getShell());
								dialog.setText(Messages.getString("PushPullPage.snapfile.select")); //$NON-NLS-1$
								String file = dialog.open();
								if (file != null) {
									snapFileCombo.setText(file);
								}
							}
						});

				SelectionListener forestCheckBoxListener = new SelectionListener() {
					public void widgetSelected(SelectionEvent e) {
						forestLabel.setEnabled(forestCheckBox.getSelection());
						snapFileButton
								.setEnabled(forestCheckBox.getSelection());
						snapFileCombo.setEnabled(forestCheckBox.getSelection());
					}

					public void widgetDefaultSelected(SelectionEvent e) {
						widgetSelected(e);
					}
				};
				forestCheckBox.addSelectionListener(forestCheckBoxListener);
			}
		}

		if (showSvn) {
			this.svnCheckBox = SWTWidgetHelper.createCheckBox(optionGroup,
					Messages.getString("PushPullPage.option.svn"));             //$NON-NLS-1$
		}
	}

	private void createRevisionTable(Composite composite) {
		this.revCheckBox = SWTWidgetHelper.createCheckBox(optionGroup,
				getRevCheckBoxLabel());

		Listener revCheckBoxListener = new Listener() {
			public void handleEvent(Event event) {
				// en-/disable list view
				changesetTable.setEnabled(revCheckBox.getSelection());
			}
		};

		this.revCheckBox.addListener(SWT.Selection, revCheckBoxListener);

		Group revGroup = SWTWidgetHelper.createGroup(composite,
				getRevGroupLabel(), GridData.FILL_BOTH);

		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.heightHint = 150;
		gridData.minimumHeight = 50;
		this.changesetTable = new ChangesetTable(revGroup, hgRoot);
		this.changesetTable.setLayoutData(gridData);
		this.changesetTable.setEnabled(false);

		SelectionListener listener = new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				setPageComplete(true);
				revision = changesetTable.getSelection().toString();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		};

		this.changesetTable.addSelectionListener(listener);
	}

	protected String getRevGroupLabel() {
		return Messages.getString("PushRepoPage.revGroup.title"); //$NON-NLS-1$
	}

	protected String getRevCheckBoxLabel() {
		return Messages.getString("PushRepoPage.revCheckBox.text");//$NON-NLS-1$
	}

	protected String getForceCheckBoxLabel() {
		return Messages.getString("PushRepoPage.forceCheckBox.text");//$NON-NLS-1$
	}

	protected String getTimeoutCheckBoxLabel() {
		return Messages.getString("PushRepoPage.timeoutCheckBox.text");//$NON-NLS-1$
	}

	public boolean isForce() {
		return force;
	}

	public String getRevision() {
		return revision;
	}

	public boolean isTimeout() {
		return timeout;
	}

	public Button getForceCheckBox() {
		return forceCheckBox;
	}

	public ChangesetTable getChangesetTable() {
		return changesetTable;
	}

	public Button getRevCheckBox() {
		return revCheckBox;
	}

	public Button getTimeoutCheckBox() {
		return timeoutCheckBox;
	}

	public boolean isShowRevisionTable() {
		return showRevisionTable;
	}

	public void setShowRevisionTable(boolean showRevisionTable) {
		this.showRevisionTable = showRevisionTable;
	}

	public boolean isShowForce() {
		return showForce;
	}

	public void setShowForce(boolean showForce) {
		this.showForce = showForce;
	}

	public boolean isShowForest() {
		return showForest;
	}

	public void setShowForest(boolean showForest) throws HgException {
		this.showForest = showForest
				&& MercurialUtilities.isCommandAvailable("fpull", //$NON-NLS-1$
						ResourceProperties.EXT_FOREST_AVAILABLE, null);
	}

	public String getSnapFileText() {
		return snapFileCombo != null? snapFileCombo.getText() : null;
	}

	public void setSnapFileCombo(Combo snapFileCombo) {
		this.snapFileCombo = snapFileCombo;
	}

	public boolean isShowSnapFile() {
		return showSnapFile;
	}

	public void setShowSnapFile(boolean showSnapFile) {
		this.showSnapFile = showSnapFile;
	}

	public Button getForestCheckBox() {
		return forestCheckBox;
	}

	public void setForestCheckBox(Button forestCheckBox) {
		this.forestCheckBox = forestCheckBox;
	}

	public boolean isShowSvn() {
		return showSvn;
	}

	public void setShowSvn(boolean showSvn) throws HgException {
		this.showSvn = showSvn
				&& MercurialUtilities.isCommandAvailable("svn", //$NON-NLS-1$
						ResourceProperties.EXT_HGSUBVERSION_AVAILABLE, null);
	}

	public Button getSvnCheckBox() {
		return svnCheckBox;
	}

	public void setSvnCheckBox(Button svnCheckBox) {
		this.svnCheckBox = svnCheckBox;
	}

	@Override
	protected HgRoot getHgRoot() {
		return hgRoot;
	}

}
