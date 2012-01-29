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
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.team.ResourceProperties;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;

/**
 * @author bastian
 *
 */
public abstract class PushPullPage extends ConfigurationWizardMainPage {

	protected Button forceCheckBox;
	protected Group optionGroup;
	private boolean force;
	private boolean timeout;

	private Button timeoutCheckBox;
	private Button forestCheckBox;
	private Combo snapFileCombo;
	private Button snapFileButton;
	private Button svnCheckBox;

	private boolean showForce;
	private boolean showForest;
	private boolean showSnapFile;
	private boolean showSvn;

	public PushPullPage(HgRoot hgRoot, String pageName, String title,
			ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
		showSnapFile = true;
		showForce = true;
		setHgRoot(hgRoot);
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
		timeoutCheckBox = SWTWidgetHelper.createCheckBox(optionGroup,
				getTimeoutCheckBoxLabel());

		if (showForce) {
			forceCheckBox = SWTWidgetHelper.createCheckBox(optionGroup,
					getForceCheckBoxLabel());
			forceCheckBox.addSelectionListener(new SelectionListener() {
				public void widgetDefaultSelected(SelectionEvent e) {
				}
				public void widgetSelected(SelectionEvent e) {
					optionChanged();
				}
			});
		}

		createExtensionControls();

		initDefaultLocation();
	}

	protected void createExtensionControls() {
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
			svnCheckBox = SWTWidgetHelper.createCheckBox(optionGroup,
					Messages.getString("PushPullPage.option.svn"));             //$NON-NLS-1$
		}
	}

	@SuppressWarnings("static-method")
	protected String getForceCheckBoxLabel() {
		return Messages.getString("PushRepoPage.forceCheckBox.text"); //$NON-NLS-1$
	}

	@SuppressWarnings("static-method")
	protected String getTimeoutCheckBoxLabel() {
		return Messages.getString("PushRepoPage.timeoutCheckBox.text"); //$NON-NLS-1$
	}

	public boolean isForce() {
		return force;
	}

	public boolean isTimeout() {
		return timeout;
	}

	public boolean isForceSelected() {
		return forceCheckBox != null && forceCheckBox.getSelection();
	}

	public boolean isTimeoutSelected() {
		return timeoutCheckBox != null && timeoutCheckBox.getSelection();
	}

	public void setShowForce(boolean showForce) {
		this.showForce = showForce;
	}

	public boolean isShowForest() {
		return showForest;
	}

	protected void setShowForest(boolean showForest) throws HgException {
		this.showForest = showForest
				&& MercurialUtilities.isCommandAvailable("fpull", //$NON-NLS-1$
						ResourceProperties.EXT_FOREST_AVAILABLE, null);
	}

	public String getSnapFileText() {
		return snapFileCombo != null? snapFileCombo.getText() : null;
	}

	public boolean isShowSnapFile() {
		return showSnapFile;
	}

	public void setShowSnapFile(boolean showSnapFile) {
		this.showSnapFile = showSnapFile;
	}

	public boolean isForestSelected() {
		return forestCheckBox.getSelection();
	}

	public boolean isShowSvn() {
		return showSvn;
	}

	protected void setShowSvn(boolean showSvn) throws HgException {
		this.showSvn = showSvn
				&& MercurialUtilities.isCommandAvailable("svn", //$NON-NLS-1$
						ResourceProperties.EXT_HGSUBVERSION_AVAILABLE, null);
	}

	public boolean isSvnSelected() {
		return isShowSvn() && svnCheckBox != null && svnCheckBox.getSelection();
	}

	@Override
	public boolean finish(IProgressMonitor monitor) {
		this.force = isForceSelected();
		this.timeout = isTimeoutSelected();
		return super.finish(monitor);
	}
}
