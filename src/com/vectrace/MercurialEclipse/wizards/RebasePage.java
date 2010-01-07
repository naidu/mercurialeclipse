/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 * Andrei Loskutov (Intland) - bugfixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.team.ResourceProperties;
import com.vectrace.MercurialEclipse.ui.ChangesetTable;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;

/**
 * @author bastian
 */
public class RebasePage extends HgWizardPage {

	private final HgRoot hgRoot;
	private ChangesetTable srcTable;
	private Button sourceRevCheckBox;
	private Button baseRevCheckBox;
	private Button destRevCheckBox;
	private Button collapseRevCheckBox;
	private Button continueRevCheckBox;
	private Button abortRevCheckBox;
	private ChangesetTable destTable;

	public RebasePage(String pageName, String title,
			ImageDescriptor titleImage, String description, HgRoot hgRoot) {
		super(pageName, title, titleImage, description);
		this.hgRoot = hgRoot;
	}

	public void createControl(Composite parent) {
		Composite comp = SWTWidgetHelper.createComposite(parent, 2);

		createSrcWidgets(comp);
		createDestWidgets(comp);
		createOptionsWidgets(comp);

		setControl(comp);
		try {
			if (!MercurialUtilities.isCommandAvailable("rebase", //$NON-NLS-1$
					ResourceProperties.REBASE_AVAILABLE, "hgext.rebase=")) { //$NON-NLS-1$
				setErrorMessage(Messages.getString("RebasePage.error.notAvailable")); //$NON-NLS-1$
			}
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
			setErrorMessage(e.getLocalizedMessage());
		}
	}

	private void createOptionsWidgets(Composite comp) {
		Group optionGroup = SWTWidgetHelper.createGroup(comp, Messages.getString("RebasePage.optionGroup.label"), 2, //$NON-NLS-1$
				GridData.FILL_BOTH);

		this.collapseRevCheckBox = SWTWidgetHelper.createCheckBox(optionGroup,
				Messages.getString("RebasePage.option.collapse")); //$NON-NLS-1$
		this.abortRevCheckBox = SWTWidgetHelper.createCheckBox(optionGroup,
				Messages.getString("RebasePage.option.abort")); //$NON-NLS-1$

		SelectionListener abortSl = new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			public void widgetSelected(SelectionEvent e) {
				boolean selection = abortRevCheckBox.getSelection();
				sourceRevCheckBox.setEnabled(!selection);
				baseRevCheckBox.setEnabled(!selection);
				destRevCheckBox.setEnabled(!selection);

				if (selection) {
					sourceRevCheckBox.setSelection(false);
					baseRevCheckBox.setSelection(false);
					destRevCheckBox.setSelection(false);
					collapseRevCheckBox.setSelection(false);
					continueRevCheckBox.setSelection(false);
					srcTable.setEnabled(false);
					destTable.setEnabled(false);
				}
			}
		};

		abortRevCheckBox.addSelectionListener(abortSl);

		this.continueRevCheckBox = SWTWidgetHelper.createCheckBox(optionGroup,
				Messages.getString("RebasePage.option.continue")); //$NON-NLS-1$

		SelectionListener contSl = new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			public void widgetSelected(SelectionEvent e) {
				boolean selection = continueRevCheckBox.getSelection();
				sourceRevCheckBox.setEnabled(!selection);
				baseRevCheckBox.setEnabled(!selection);
				destRevCheckBox.setEnabled(!selection);

				if (selection) {
					sourceRevCheckBox.setSelection(false);
					baseRevCheckBox.setSelection(false);
					destRevCheckBox.setSelection(false);
					collapseRevCheckBox.setSelection(false);
					abortRevCheckBox.setSelection(false);
					srcTable.setEnabled(false);
					destTable.setEnabled(false);
				}
			}
		};
		continueRevCheckBox.addSelectionListener(contSl);
	}

	private void createDestWidgets(Composite comp) {
		Group destGroup = SWTWidgetHelper.createGroup(comp,
				Messages.getString("RebasePage.destinationGroup.label"), 2, GridData.FILL_BOTH); //$NON-NLS-1$
		this.destRevCheckBox = SWTWidgetHelper.createCheckBox(destGroup,
				Messages.getString("RebasePage.destinationCheckbox.label")); //$NON-NLS-1$

		SelectionListener sl = new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
			public void widgetSelected(SelectionEvent e) {
				destTable.setEnabled(destRevCheckBox.getSelection());
			}
		};
		destRevCheckBox.addSelectionListener(sl);

		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.heightHint = 150;
		gridData.minimumHeight = 50;
		destTable = new ChangesetTable(destGroup, hgRoot);
		destTable.setLayoutData(gridData);
		destTable.setEnabled(false);
	}

	private void createSrcWidgets(Composite comp) {
		Group srcGroup = SWTWidgetHelper.createGroup(comp,
				Messages.getString("RebasePage.sourceGroup.label"), 2, GridData.FILL_BOTH); //$NON-NLS-1$
		this.sourceRevCheckBox = SWTWidgetHelper.createCheckBox(srcGroup,
				Messages.getString("RebasePage.source.label")); //$NON-NLS-1$
		this.baseRevCheckBox = SWTWidgetHelper.createCheckBox(srcGroup,
				Messages.getString("RebasePage.base.label")); //$NON-NLS-1$

		SelectionListener srcSl = new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			public void widgetSelected(SelectionEvent e) {
				srcTable.setEnabled(sourceRevCheckBox.getSelection()
						|| baseRevCheckBox.getSelection());
				if (sourceRevCheckBox.getSelection()) {
					baseRevCheckBox.setSelection(false);
				}
			}
		};

		SelectionListener baseSl = new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			public void widgetSelected(SelectionEvent e) {
				srcTable.setEnabled(sourceRevCheckBox.getSelection()
						|| baseRevCheckBox.getSelection());
				if (baseRevCheckBox.getSelection()) {
					sourceRevCheckBox.setSelection(false);
				}
			}
		};

		this.sourceRevCheckBox.addSelectionListener(srcSl);
		this.baseRevCheckBox.addSelectionListener(baseSl);
		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.heightHint = 150;
		gridData.minimumHeight = 50;
		srcTable = new ChangesetTable(srcGroup, hgRoot);
		srcTable.setLayoutData(gridData);
		srcTable.setEnabled(false);
	}

	public ChangesetTable getSrcTable() {
		return srcTable;
	}

	public void setSrcTable(ChangesetTable changesetTable) {
		this.srcTable = changesetTable;
	}

	public Button getSourceRevCheckBox() {
		return sourceRevCheckBox;
	}

	public void setSourceRevCheckBox(Button sourceRevCheckBox) {
		this.sourceRevCheckBox = sourceRevCheckBox;
	}

	public Button getBaseRevCheckBox() {
		return baseRevCheckBox;
	}

	public void setBaseRevCheckBox(Button baseRevCheckBox) {
		this.baseRevCheckBox = baseRevCheckBox;
	}

	public Button getDestRevCheckBox() {
		return destRevCheckBox;
	}

	public void setDestRevCheckBox(Button destRevCheckBox) {
		this.destRevCheckBox = destRevCheckBox;
	}

	public Button getCollapseRevCheckBox() {
		return collapseRevCheckBox;
	}

	public void setCollapseRevCheckBox(Button collapseRevCheckBox) {
		this.collapseRevCheckBox = collapseRevCheckBox;
	}

	public Button getContinueRevCheckBox() {
		return continueRevCheckBox;
	}

	public void setContinueRevCheckBox(Button continueRevCheckBox) {
		this.continueRevCheckBox = continueRevCheckBox;
	}

	public Button getAbortRevCheckBox() {
		return abortRevCheckBox;
	}

	public void setAbortRevCheckBox(Button abortRevCheckBox) {
		this.abortRevCheckBox = abortRevCheckBox;
	}

	public ChangesetTable getDestTable() {
		return destTable;
	}

	public void setDestTable(ChangesetTable destTable) {
		this.destTable = destTable;
	}

}
