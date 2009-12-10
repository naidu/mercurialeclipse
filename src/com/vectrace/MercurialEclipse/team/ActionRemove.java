/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Software Balm Consulting Inc (Peter Hunnisett <peter_hge at softwarebalm dot com>) - some updates
 *     Stefan Groschupf          - logError
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;

/**
 * @author zingo
 *
 */
public class ActionRemove implements IWorkbenchWindowActionDelegate {

	private IWorkbenchWindow window;
	// private IWorkbenchPart targetPart;
	private IStructuredSelection selection;

	public ActionRemove() {
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
		this.window = w;
	}

	/**
	 * The action has been activated. The argument of the method represents the
	 * 'real' action sitting in the workbench UI.
	 *
	 * @see IWorkbenchWindowActionDelegate#run
	 */

	@SuppressWarnings("unchecked")
	public void run(IAction action) {
		// String FullPath;
		Shell shell;
		IWorkbench workbench;

		// Get shell & workbench
		if ((window != null) && (window.getShell() != null)) {
			shell = window.getShell();
		} else {
			workbench = PlatformUI.getWorkbench();
			shell = workbench.getActiveWorkbenchWindow().getShell();
		}

		// the last argument will be replaced with a path
		String launchCmd[] = { MercurialUtilities.getHGExecutable(),
				"remove", "-Af", "--", "" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		Iterator itr = selection.iterator();
		Set<IProject> projects = new HashSet<IProject>();
		while (itr.hasNext()) {
			Object obj = itr.next();
			if (!(obj instanceof IResource) || obj instanceof IProject) {
				continue;
			}
			IResource resource = (IResource) obj;
			if (!MercurialUtilities.hgIsTeamProviderFor(resource, true)) {
				continue;
			}

			// Resource could be inside a link or something do nothing
			// in the future this could check is this is another repository

			// Setup and run command
			try {
				HgRoot root = AbstractClient.getHgRoot(resource);
				launchCmd[4] = root.toRelative(resource.getLocation().toFile());
				if (!confirmRemove(shell, launchCmd[4])) {
					continue;
				}
				projects.add(resource.getProject());
				String output = MercurialUtilities.executeCommand(
						launchCmd, root, false);
				if (output != null) {
					// output output in a window
					if (output.length() != 0) {
						MessageDialog
						.openInformation(
								shell,
								Messages
								.getString("ActionRemove.removeOutput"), output); //$NON-NLS-1$
					}
				}
			} catch (HgException e) {
				MercurialEclipsePlugin.logError(e);
			}

			// MercurialEclipsePlugin.refreshProjectFlags(proj);
		}

		for (IProject proj : projects) {
			try {
				MercurialStatusCache.getInstance().refreshStatus(proj, new NullProgressMonitor());
			} catch (TeamException e) {
				MercurialEclipsePlugin
				.logError(
						Messages
						.getString("ActionRemove.unableToRefresh"), e); //$NON-NLS-1$
			}
		}
	}

	private boolean confirmRemove(Shell shell, String fileName) {
		return MessageDialog.openConfirm(shell,
				Messages.getString("ActionRemove.removeFileQuestion"),
				Messages.getString("ActionRemove.removeFileConfirmation")
				+ fileName
				+ Messages.getString("ActionRemove.removeFileConfirmation.2"));
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
