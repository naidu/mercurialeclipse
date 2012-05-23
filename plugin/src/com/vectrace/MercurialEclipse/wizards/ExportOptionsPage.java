/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     steeven                    - implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import com.aragost.javahg.commands.DiffCommand;
import com.aragost.javahg.commands.flags.DiffCommandFlags;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;

/**
 * Last page of import wizard for import options.
 */
public class ExportOptionsPage extends HgWizardPage implements Listener {

	private Button chkBase;
	private Button chkFunction;
	private Text txtUnified;
	private Button chkUnified;
	private Text txtBase;
	private Button chkNoDate;
	private Button chkIgnoreAllSpace;
	private Button chkIgnoreSpaceChange;
	private Button chkIgnoreBlankLines;

	public ExportOptionsPage() {
		super(Messages.getString("ImportPatchWizard.optionsPageName"), Messages //$NON-NLS-1$
				.getString("ImportPatchWizard.optionsPageTitle"), null); //$NON-NLS-1$
	}

	public void createControl(Composite parent) {
		Composite composite = SWTWidgetHelper.createComposite(parent, 2);

		chkBase = createLabelCheckBox(composite, Messages
				.getString("ExportOptionsPage.rev")); //$NON-NLS-1$
		chkBase.addListener(SWT.Selection, this);
		txtBase = SWTWidgetHelper.createTextField(composite);

		chkFunction = SWTWidgetHelper.createCheckBox(composite, Messages
				.getString("ExportOptionsPage.function")); //$NON-NLS-1$

		chkNoDate = SWTWidgetHelper.createCheckBox(composite, Messages
				.getString("ExportOptionsPage.noDate")); //$NON-NLS-1$

		chkIgnoreAllSpace = SWTWidgetHelper.createCheckBox(composite, Messages
				.getString("ExportOptionsPage.ignoreAllSpace")); //$NON-NLS-1$
		chkIgnoreSpaceChange = SWTWidgetHelper.createCheckBox(composite,
				Messages.getString("ExportOptionsPage.ignoreSpaceChange")); //$NON-NLS-1$
		chkIgnoreBlankLines = SWTWidgetHelper.createCheckBox(composite,
				Messages.getString("ExportOptionsPage.ignoreBlankLines")); //$NON-NLS-1$

		chkUnified = createLabelCheckBox(composite, Messages
				.getString("ExportOptionsPage.context")); //$NON-NLS-1$
		chkUnified.addListener(SWT.Selection, this);
		txtUnified = SWTWidgetHelper.createTextField(composite);

		setControl(composite);
		validate();
	}

	public void handleEvent(Event event) {
		validate();
	}

	private void validate() {
		txtUnified.setEnabled(chkUnified.getSelection());
		txtBase.setEnabled(chkBase.getSelection());
		setErrorMessage(null);
		setPageComplete(true);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.vectrace.MercurialEclipse.wizards.HgWizardPage#finish(org.eclipse
	 * .core.runtime.IProgressMonitor)
	 */
	@Override
	public boolean finish(IProgressMonitor monitor) {
		// getDialogSettings(); save setttings.
		return super.finish(monitor);
	}

	DiffCommand getOptions(HgRoot root) {
		DiffCommand command = DiffCommandFlags.on(root.getRepository());

		if (chkBase.getSelection()) {
			command.rev(txtBase.getText());
		}
		if (chkFunction.getSelection()) {
			command.showFunction();
		}
		if (chkNoDate.getSelection()) {
			command.nodates();
		}
		if (chkIgnoreAllSpace.getSelection()) {
			command.ignoreAllSpace();
		}
		if (chkIgnoreSpaceChange.getSelection()) {
			command.ignoreSpaceChange();
		}
		if (chkIgnoreBlankLines.getSelection()) {
			command.ignoreBlankLines();
		}
		if (chkUnified.getSelection()) {
			command.unified(Integer.parseInt(txtUnified.getText()));
		}

		return command;
	}

	public static Button createLabelCheckBox(Composite group, String label) {
		Button button = new Button(group, SWT.CHECK | SWT.LEFT);
		button.setText(label);
		return button;
	}
}
