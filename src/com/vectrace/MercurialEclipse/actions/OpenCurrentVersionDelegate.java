/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Ilya Ivanov	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.actions;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.FileFromChangeSet;

/**
 * This delegate is used for object contribution to context menus.
 * Object class must be FileFromChangeSet or be adaptable to it.
 */
public class OpenCurrentVersionDelegate implements IObjectActionDelegate {

	private FileFromChangeSet fileFromChangeSet;
	private IWorkbenchPart targetPart;

	public void run(IAction action) {
		try {
			IDE.openEditor(targetPart.getSite().getPage(), fileFromChangeSet.getFile());
		} catch (PartInitException e) {
			MercurialEclipsePlugin.logError(e);
		}
	}

	/**
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection newSelection) {
		action.setEnabled(false);
		if (newSelection instanceof IStructuredSelection) {
			IStructuredSelection sel = (IStructuredSelection) newSelection;
			if (sel.getFirstElement() instanceof FileFromChangeSet) {
				this.fileFromChangeSet = (FileFromChangeSet) sel.getFirstElement();
				IFile file = fileFromChangeSet.getFile();
				if (file.exists()) {
					action.setEnabled(true);
				}
			}
		} else {
			this.fileFromChangeSet = null;
		}
	}

	/**
	 * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.action.IAction, org.eclipse.ui.IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		this.targetPart = targetPart;
	}
}
