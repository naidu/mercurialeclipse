/*******************************************************************************
 * Copyright (c) 2006-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     David Watson - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;

import java.io.File;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.annotations.ShowAnnotationOperation;
import com.vectrace.MercurialEclipse.menu.Messages;

public class ActionAnnotate implements IWorkbenchWindowActionDelegate {
	private IStructuredSelection selection;

	public ActionAnnotate() {
		super();
	}

	/**
	 * We can use this method to dispose of any system resources we previously
	 * allocated.
	 *
	 * @see IWorkbenchWindowActionDelegate#dispose
	 */
	public void dispose() {

	}

	/**
	 * We will cache window object in order to be able to provide parent shell
	 * for the message dialog.
	 *
	 * @see IWorkbenchWindowActionDelegate#init
	 */
	public void init(IWorkbenchWindow w) {
	}

	/**
	 * The action has been activated. The argument of the method represents the
	 * 'real' action sitting in the workbench UI.
	 *
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	public void run(IAction action) {
		IWorkbenchPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();
		if (selection.getFirstElement() instanceof IResource) {
			IResource firstElement = (IResource)selection.getFirstElement();
			File file = firstElement.getLocation().toFile();
			try {
				new ShowAnnotationOperation(part, file).run();
			} catch (Exception e) {
				MessageDialog.openError(part.getSite().getShell(), Messages.getString("ShowAnnotationHandler.hgSays"), e.getMessage() //$NON-NLS-1$
						+ Messages.getString("ShowAnnotationHandler.seeErrorLogForMoreDetails")); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Selection in the workbench has been changed. We can change the state of
	 * the 'real' action here if we want, but this can only happen after the
	 * delegate has been created.
	 *
	 * @see IWorkbenchWindowActionDelegate#selectionChanged
	 */
	public void selectionChanged(IAction action, ISelection in_selection) {
		if (in_selection != null
				&& in_selection instanceof IStructuredSelection) {
			selection = (IStructuredSelection) in_selection;
		}
	}

}
