/*******************************************************************************
 * Copyright (c) 2003, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 *     Bastian Doetsch              - adaptation
 ******************************************************************************/
package com.vectrace.MercurialEclipse.repository.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IWorkbenchPart;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;

/**
 * Helper class for actions that are displayed in a toolbar
 */
public abstract class ReplaceableIconAction extends TeamAction {

	private IAction action = null;
	private boolean isInitialized = false;

	/**
	 * Returns the id of the image for this menu entry
	 *
	 * @return the id of the image for this menu entry
	 */
	protected String getImageId() {
		return null;
	}

	/*
	 * @see org.eclipse.ui.actions.ActionDelegate#init(org.eclipse.jface.action.IAction)
	 */
	@Override
	public void init(IAction act) {
		super.init(action);

		this.action = act;

		setIcon();
	}

	/*
	 * @see org.tigris.subversion.subclipse.ui.internal.TeamAction#setActivePart(org.eclipse.jface.action.IAction,
	 *      org.eclipse.ui.IWorkbenchPart)
	 */
	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		super.setActivePart(action, targetPart);

		if (!isInitialized) {
			setIcon();
			isInitialized = true;
		}
	}

	protected void setIcon() {
		String iconName = getImageId();

		if (iconName != null) {
			ImageDescriptor descriptor = MercurialEclipsePlugin
					.getImageDescriptor(iconName);
			action.setImageDescriptor(descriptor);
		}
	}

	/*
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#dispose()
	 */
	@Override
	public void dispose() {
		super.dispose();
		action = null;
	}

	protected IAction getAction() {
		return action;
	}
}
