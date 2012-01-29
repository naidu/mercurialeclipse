/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * John Peberdy	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.views;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.part.ViewPart;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;

/**
 *
 */
public abstract class AbstractRootView extends ViewPart implements ISelectionListener {

	protected HgRoot hgRoot;

	@Override
	public void createPartControl(Composite parent) {
		setContentDescription(getDescription());

		parent.setLayout(new GridLayout(1, false));

		createTable(parent);
		createActions();
		createToolBar(getViewSite().getActionBars().getToolBarManager());
		createMenus(getViewSite().getActionBars().getMenuManager());
		getSite().getPage().addSelectionListener(this);
	}

	protected abstract void createTable(Composite parent);

	protected abstract void createActions();

	protected abstract void createMenus(IMenuManager mgr);

	protected abstract void createToolBar(IToolBarManager mgr);

	protected static IContributionItem makeActionContribution(IAction a)
	{
		ActionContributionItem c = new ActionContributionItem(a);
		c.setMode(c.getMode() | ActionContributionItem.MODE_FORCE_TEXT);
		return c;
	}

	protected void handleError(CoreException e) {
		MercurialEclipsePlugin.logError(e);

		String sMessage;

		if (e instanceof HgException) {
			sMessage = ((HgException) e).getConciseMessage();
		} else {
			sMessage = e.getLocalizedMessage();
		}

		showWarning(sMessage);
	}

	protected void showWarning(String sMessage) {
		MessageDialog.openError(getSite().getShell(), "Error", fixCase(sMessage));
	}

	private static String fixCase(String sMessage) {
		if (sMessage != null) {
			sMessage = sMessage.trim();

			if (sMessage.length() > 0) {
				sMessage = Character.toUpperCase(sMessage.charAt(0)) + sMessage.substring(1);
			}
		}
		return sMessage;
	}

	@Override
	public void dispose() {
		getSite().getPage().removeSelectionListener(this);
		super.dispose();
	}

	public final void refresh(HgRoot newRoot) {
		hgRoot = null;

		if (newRoot != null && canChangeRoot(newRoot, false)) {
			hgRoot = newRoot;
		}

		handleRootChanged();
	}

	protected final void rootSelected(HgRoot newRoot) {
		if (newRoot != null && canChangeRoot(newRoot, true)) {
			hgRoot = newRoot;
			handleRootChanged();
		}
	}

	private void handleRootChanged() {
		onRootChanged();
		setContentDescription(getDescription());
	}

	protected abstract String getDescription();

	/**
	 * Template method to customize root changing behavior
	 *
	 * @return True if the selection can be changed
	 */
	protected boolean canChangeRoot(HgRoot newRoot, boolean fromSelection) {
		return !newRoot.equals(hgRoot);
	}

	/**
	 * Called when the selected root changes
	 */
	protected abstract void onRootChanged();
}
