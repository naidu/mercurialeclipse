/*******************************************************************************
 * Copyright (c) 2008 MercurialEclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch				- implementation
 *     Andrei Loskutov - bug fixes
 ******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeUiJob;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.menu.CommitHandler;

public class CommitSynchronizeOperation extends SynchronizeModelOperation {
	private final IResource[] resources;

	public CommitSynchronizeOperation(
			ISynchronizePageConfiguration configuration,
			IDiffElement[] elements, IResource[] resources) {
		super(configuration, elements);
		this.resources = resources;
	}

	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		monitor.beginTask("Loading Commit Window...", 1);
		new SafeUiJob("Committing selected resources...") {

			@Override
			protected IStatus runSafe(IProgressMonitor moni) {
				if (resources.length == 0) {
					MessageDialog.openInformation(getShell(), "Mercurial Commit", //$NON-NLS-1$
							"Please select at least one file to commit!"); //$NON-NLS-1$
					return super.runSafe(moni);
				}
				try {
					final CommitHandler commitAction = new CommitHandler();
					commitAction.run(Arrays.asList(resources));
				} catch (HgException e) {
					MercurialEclipsePlugin.logError(e);
				}

				return super.runSafe(moni);
			}

		}.schedule();
		monitor.done();
	}

}
