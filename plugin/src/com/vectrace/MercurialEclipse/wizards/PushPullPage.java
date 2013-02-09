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
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

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
	private boolean timeout;

	private Button timeoutCheckBox;
	private Button svnCheckBox;

	private boolean showForce;
	private boolean showSvn;

	public PushPullPage(HgRoot hgRoot, String pageName, String title,
			ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
		showForce = true;
		setHgRoot(hgRoot);
		try {
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
					if (forceCheckBox.getSelection()) {
						onForceEnabled();
					}
				}
			});
		}

		createExtensionControls();

		initDefaultLocation();
	}

	/**
	 * Called when force is enabled by the user. By default does nothing.
	 */
	protected  void onForceEnabled() {
	}

	protected void createExtensionControls() {
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
		this.timeout = isTimeoutSelected();
		return super.finish(monitor);
	}
}
