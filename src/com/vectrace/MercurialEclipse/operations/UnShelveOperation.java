/*******************************************************************************
 * Copyright (c) 2005-2008 Bastian Doetsch and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.operations;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IWorkbenchPart;

import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.commands.HgClients;
import com.vectrace.MercurialEclipse.commands.HgPatchClient;
import com.vectrace.MercurialEclipse.commands.RefreshWorkspaceStatusJob;
import com.vectrace.MercurialEclipse.commands.extensions.HgAtticClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.team.ResourceProperties;

/**
 * @author bastian
 */
public class UnShelveOperation extends HgOperation {
	private final HgRoot hgRoot;

	public UnShelveOperation(IWorkbenchPart part, HgRoot hgRoot) {
		super(part);
		this.hgRoot = hgRoot;
	}

	@Override
	protected String getActionDescription() {
		return Messages.getString("UnShelveOperation.UnshelvingChanges"); //$NON-NLS-1$
	}

	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		try {
			// get modified files
			monitor.beginTask(Messages.getString("UnShelveOperation.Unshelving"), 4); //$NON-NLS-1$

			// check if hgattic is available
			if (MercurialUtilities.isCommandAvailable("attic-shelve",// $NON-NLS-1$
							ResourceProperties.EXT_HGATTIC_AVAILABLE, "")) { // $NON-NLS-1$
				String output = HgAtticClient.unshelve(hgRoot, false, true, hgRoot.getName());
				monitor.worked(1);
				new RefreshWorkspaceStatusJob(hgRoot, true).schedule();
				monitor.worked(1);
				HgClients.getConsole().printMessage(output, null);
			} else {
				monitor.subTask(Messages
						.getString("UnShelveOperation.GettingChanges")); //$NON-NLS-1$
				File shelveDir = new File(hgRoot, ".hg" + File.separator //$NON-NLS-1$
						+ "mercurialeclipse-shelve-backups"); //$NON-NLS-1$

				if (shelveDir.exists()) {
					File shelveFile = new File(shelveDir, hgRoot.getName() + "-patchfile.patch");
					if (shelveFile.exists()) {
						monitor.worked(1);
						monitor.subTask(Messages.getString("UnShelveOperation.applyingChanges")); //$NON-NLS-1$
						ArrayList<String> opts = new ArrayList<String>();
						opts.add("--no-commit");
						HgPatchClient.importPatch(hgRoot, shelveFile, opts);
						monitor.worked(1);
						monitor.subTask(Messages.getString("UnShelveOperation.emptyingShelf")); //$NON-NLS-1$
						boolean deleted = shelveFile.delete();
						monitor.worked(1);
						monitor.subTask(Messages.getString("UnShelveOperation.refreshingProject")); //$NON-NLS-1$
						new RefreshWorkspaceStatusJob(hgRoot, true).schedule();
						monitor.worked(1);
						if (!deleted) {
							throw new HgException(shelveFile.getName() + " could not be deleted.");
						}
					} else {
						throw new HgException(Messages
								.getString("UnShelveOperation.error.ShelfEmpty")); //$NON-NLS-1$
					}
				}
			}
		} catch (Exception e) {
			throw new InvocationTargetException(e, e.getLocalizedMessage());
		} finally {
			monitor.done();
		}

	}

}
