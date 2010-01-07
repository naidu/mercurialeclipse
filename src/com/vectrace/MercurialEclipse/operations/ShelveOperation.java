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
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IWorkbenchPart;

import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.commands.HgClients;
import com.vectrace.MercurialEclipse.commands.HgIdentClient;
import com.vectrace.MercurialEclipse.commands.HgPatchClient;
import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.commands.HgUpdateClient;
import com.vectrace.MercurialEclipse.commands.RefreshWorkspaceStatusJob;
import com.vectrace.MercurialEclipse.commands.extensions.HgAtticClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.team.ResourceProperties;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * @author bastian
 */
public class ShelveOperation extends HgOperation {
	private final HgRoot hgRoot;

	public ShelveOperation(IWorkbenchPart part, HgRoot hgRoot) {
		super(part);
		this.hgRoot = hgRoot;
	}

	@Override
	protected String getActionDescription() {
		return Messages.getString("ShelveOperation.shelvingChanges"); //$NON-NLS-1$
	}

	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		File shelveDir = new File(hgRoot, ".hg" + File.separator //$NON-NLS-1$
				+ "mercurialeclipse-shelve-backups"); //$NON-NLS-1$
		try {
			// get modified files
			monitor.beginTask(Messages.getString("ShelveOperation.shelving"), 5); //$NON-NLS-1$
			// check if hgattic is available
			if (MercurialUtilities.isCommandAvailable("attic-shelve",// $NON-NLS-1$
							ResourceProperties.EXT_HGATTIC_AVAILABLE, "")) { // $NON-NLS-1$
				String output = HgAtticClient.shelve(hgRoot,
						"MercurialEclipse shelve operation", // $NON-NLS-1$
						true, MercurialUtilities.getHGUsername(),
						hgRoot.getName());
				monitor.worked(1);
				new RefreshWorkspaceStatusJob(hgRoot, true).schedule();
				monitor.worked(1);
				HgClients.getConsole().printMessage(output, null);
			} else {

				monitor.subTask(Messages.getString("ShelveOperation.determiningChanges")); //$NON-NLS-1$
				//
				if (!HgStatusClient.isDirty(hgRoot)) {
					throw new HgException(Messages.getString("ShelveOperation.error.nothingToShelve")); //$NON-NLS-1$
				}
				monitor.worked(1);
				monitor.subTask(Messages.getString("ShelveOperation.shelvingChanges")); //$NON-NLS-1$

				boolean mkdir = shelveDir.mkdir();
				if(!mkdir && !shelveDir.exists()){
					throw new HgException(Messages.getString("ShelveOperation.error.shelfDirCreateFailed")); //$NON-NLS-1$
				}
				File shelveFile = new File(shelveDir, hgRoot.getName() + "-patchfile.patch"); //$NON-NLS-1$
				if (shelveFile.exists()) {
					throw new HgException(Messages.getString("ShelveOperation.error.shelfNotEmpty")); //$NON-NLS-1$
				}
				// use empty resources to be able to shelve ALL files, also deleted/added
				Set<IPath> resources = new HashSet<IPath>();// getDirtyFiles(hgRoot);
				HgPatchClient.exportPatch(hgRoot, resources, shelveFile, new ArrayList<String>(0));
				monitor.worked(1);
				monitor.subTask(Messages.getString("ShelveOperation.determiningCurrentChangeset")); //$NON-NLS-1$
				String currRev = HgIdentClient.getCurrentChangesetId(hgRoot);
				monitor.worked(1);
				monitor.subTask(Messages.getString("ShelveOperation.cleaningDirtyFiles")); //$NON-NLS-1$
				HgUpdateClient.update(hgRoot, currRev, true);
			}
		} catch (CoreException e) {
			// cleanup directory which otherwise may contain empty or invalid files and
			// block next shelve operation to execute
			if(shelveDir.isDirectory()){
				ResourceUtils.delete(shelveDir, true);
			}
			throw new InvocationTargetException(e, e.getLocalizedMessage());
		} finally {
			monitor.done();
		}

	}

}
