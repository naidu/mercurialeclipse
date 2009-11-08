/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.team.ResourceProperties;
import com.vectrace.MercurialEclipse.ui.ChangesetTable;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;

/**
 * @author bastian
 *
 */
public class RebasePage extends HgWizardPage {

	private IResource resource;
	private ChangesetTable srcTable;
	private Button sourceRevCheckBox;
	private Button baseRevCheckBox;
	private Button destRevCheckBox;
	private Button collapseRevCheckBox;
	private Button continueRevCheckBox;
	private Button abortRevCheckBox;
	private ChangesetTable destTable;

	/**
	 * @param pageName
	 */
	public RebasePage(String pageName) {
		super(pageName);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param pageName
	 * @param title
	 * @param titleImage
	 * @param description
	 */
	public RebasePage(String pageName, String title,
			ImageDescriptor titleImage, String description, IResource res) {
		super(pageName, title, titleImage, description);
		this.resource = res;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets
	 * .Composite)
	 */
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

	/**
	 * @param comp
	 */
	private void createOptionsWidgets(Composite comp) {
		Group optionGroup = SWTWidgetHelper.createGroup(comp, Messages.getString("RebasePage.optionGroup.label"), 2, //$NON-NLS-1$
				GridData.FILL_BOTH);

		this.collapseRevCheckBox = SWTWidgetHelper.createCheckBox(optionGroup,
				Messages.getString("RebasePage.option.collapse")); //$NON-NLS-1$
		this.abortRevCheckBox = SWTWidgetHelper.createCheckBox(optionGroup,
				Messages.getString("RebasePage.option.abort")); //$NON-NLS-1$

		SelectionListener abortSl = new SelectionListener() {
			/*
			 * (non-Javadoc)
			 *
			 * @see
			 * org.eclipse.swt.events.SelectionListener#widgetDefaultSelected
			 * (org.eclipse.swt.events.SelectionEvent)
			 */
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			/*
			 * (non-Javadoc)
			 *
			 * @see
			 * org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse
			 * .swt.events.SelectionEvent)
			 */
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
			/*
			 * (non-Javadoc)
			 *
			 * @see
			 * org.eclipse.swt.events.SelectionListener#widgetDefaultSelected
			 * (org.eclipse.swt.events.SelectionEvent)
			 */
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			/*
			 * (non-Javadoc)
			 *
			 * @see
			 * org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse
			 * .swt.events.SelectionEvent)
			 */
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

	/**
	 * @param comp
	 */
	private void createDestWidgets(Composite comp) {
		Group destGroup = SWTWidgetHelper.createGroup(comp,
				Messages.getString("RebasePage.destinationGroup.label"), 2, GridData.FILL_BOTH); //$NON-NLS-1$
		this.destRevCheckBox = SWTWidgetHelper.createCheckBox(destGroup,
				Messages.getString("RebasePage.destinationCheckbox.label")); //$NON-NLS-1$

		SelectionListener sl = new SelectionListener() {
			/*
			 * (non-Javadoc)
			 *
			 * @see
			 * org.eclipse.swt.events.SelectionListener#widgetDefaultSelected
			 * (org.eclipse.swt.events.SelectionEvent)
			 */
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			/*
			 * (non-Javadoc)
			 *
			 * @see
			 * org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse
			 * .swt.events.SelectionEvent)
			 */
			public void widgetSelected(SelectionEvent e) {
				destTable.setEnabled(destRevCheckBox.getSelection());
			}
		};
		destRevCheckBox.addSelectionListener(sl);

		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.heightHint = 150;
		gridData.minimumHeight = 50;
		destTable = new ChangesetTable(destGroup, resource, true);
		destTable.setLayoutData(gridData);
		destTable.setEnabled(false);
	}

	/**
	 * @param comp
	 */
	private void createSrcWidgets(Composite comp) {
		Group srcGroup = SWTWidgetHelper.createGroup(comp,
				Messages.getString("RebasePage.sourceGroup.label"), 2, GridData.FILL_BOTH); //$NON-NLS-1$
		this.sourceRevCheckBox = SWTWidgetHelper.createCheckBox(srcGroup,
				Messages.getString("RebasePage.source.label")); //$NON-NLS-1$
		this.baseRevCheckBox = SWTWidgetHelper.createCheckBox(srcGroup,
				Messages.getString("RebasePage.base.label")); //$NON-NLS-1$

		SelectionListener srcSl = new SelectionListener() {
			/*
			 * (non-Javadoc)
			 *
			 * @see
			 * org.eclipse.swt.events.SelectionListener#widgetDefaultSelected
			 * (org.eclipse.swt.events.SelectionEvent)
			 */
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			/*
			 * (non-Javadoc)
			 *
			 * @see
			 * org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse
			 * .swt.events.SelectionEvent)
			 */
			public void widgetSelected(SelectionEvent e) {
				srcTable.setEnabled(sourceRevCheckBox.getSelection()
						|| baseRevCheckBox.getSelection());
				if (sourceRevCheckBox.getSelection()) {
					baseRevCheckBox.setSelection(false);
				}
			}
		};

		SelectionListener baseSl = new SelectionListener() {
			/*
			 * (non-Javadoc)
			 *
			 * @see
			 * org.eclipse.swt.events.SelectionListener#widgetDefaultSelected
			 * (org.eclipse.swt.events.SelectionEvent)
			 */
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			/*
			 * (non-Javadoc)
			 *
			 * @see
			 * org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse
			 * .swt.events.SelectionEvent)
			 */
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
		srcTable = new ChangesetTable(srcGroup, resource, true);
		srcTable.setLayoutData(gridData);
		srcTable.setEnabled(false);
	}

	/**
	 * @return the changesetTable
	 */
	public ChangesetTable getSrcTable() {
		return srcTable;
	}

	/**
	 * @param changesetTable
	 *            the changesetTable to set
	 */
	public void setSrcTable(ChangesetTable changesetTable) {
		this.srcTable = changesetTable;
	}

	/**
	 * @return the sourceRevCheckBox
	 */
	public Button getSourceRevCheckBox() {
		return sourceRevCheckBox;
	}

	/**
	 * @param sourceRevCheckBox
	 *            the sourceRevCheckBox to set
	 */
	public void setSourceRevCheckBox(Button sourceRevCheckBox) {
		this.sourceRevCheckBox = sourceRevCheckBox;
	}

	/**
	 * @return the baseRevCheckBox
	 */
	public Button getBaseRevCheckBox() {
		return baseRevCheckBox;
	}

	/**
	 * @param baseRevCheckBox
	 *            the baseRevCheckBox to set
	 */
	public void setBaseRevCheckBox(Button baseRevCheckBox) {
		this.baseRevCheckBox = baseRevCheckBox;
	}

	/**
	 * @return the destRevCheckBox
	 */
	public Button getDestRevCheckBox() {
		return destRevCheckBox;
	}

	/**
	 * @param destRevCheckBox
	 *            the destRevCheckBox to set
	 */
	public void setDestRevCheckBox(Button destRevCheckBox) {
		this.destRevCheckBox = destRevCheckBox;
	}

	/**
	 * @return the collapseRevCheckBox
	 */
	public Button getCollapseRevCheckBox() {
		return collapseRevCheckBox;
	}

	/**
	 * @param collapseRevCheckBox
	 *            the collapseRevCheckBox to set
	 */
	public void setCollapseRevCheckBox(Button collapseRevCheckBox) {
		this.collapseRevCheckBox = collapseRevCheckBox;
	}

	/**
	 * @return the continueRevCheckBox
	 */
	public Button getContinueRevCheckBox() {
		return continueRevCheckBox;
	}

	/**
	 * @param continueRevCheckBox
	 *            the continueRevCheckBox to set
	 */
	public void setContinueRevCheckBox(Button continueRevCheckBox) {
		this.continueRevCheckBox = continueRevCheckBox;
	}

	/**
	 * @return the abortRevCheckBox
	 */
	public Button getAbortRevCheckBox() {
		return abortRevCheckBox;
	}

	/**
	 * @param abortRevCheckBox
	 *            the abortRevCheckBox to set
	 */
	public void setAbortRevCheckBox(Button abortRevCheckBox) {
		this.abortRevCheckBox = abortRevCheckBox;
	}

	/**
	 * @return the destTable
	 */
	public ChangesetTable getDestTable() {
		return destTable;
	}

	/**
	 * @param destTable
	 *            the destTable to set
	 */
	public void setDestTable(ChangesetTable destTable) {
		this.destTable = destTable;
	}

}
