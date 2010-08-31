/*******************************************************************************
 * Copyright (c) 2010 Bastian Doetsch.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch	- implementation
 *     Andrei Loskutov (Intland) - bug fixes
 *     Ilya Ivanov (Intland) - modifications
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgUpdateClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;

/**
 * @author Bastian
 */
public class UpdateJob extends Job {
	private final HgRoot hgRoot;
	private final boolean cleanEnabled;
	private final String revision;

	/**
	 * Job to do a working directory update to the specified version.
	 * @param revision the target revision
	 * @param cleanEnabled if true, discard all local changes.
	 */
	public UpdateJob(String revision, boolean cleanEnabled, HgRoot hgRoot) {
		super("Updating working directory");
		this.hgRoot = hgRoot;
		this.cleanEnabled = cleanEnabled;
		this.revision = revision;
	}

	@Override
	public IStatus run(IProgressMonitor monitor) {
		String jobName = "Updating " + hgRoot.getName();
		if (revision != null && revision.length()>0) {
			jobName += " to revision " + revision;
		}
		if (cleanEnabled) {
			jobName += " discarding all local changes (-C option).";
		}
		monitor.beginTask(jobName, 2);
		monitor.subTask("Calling Mercurial...");
		monitor.worked(1);
		try {
			HgUpdateClient.update(hgRoot, revision, cleanEnabled);
			monitor.worked(1);
		} catch (HgException e) {
			if (e.getMessage().contains("abort: crosses branches")
					&& e.getStatus().getCode() == -1) {

				// don't log this error because it's a common situation and can be handled
				return handleMultipleHeads(monitor);
			}
			MercurialEclipsePlugin.logError(e);
			return e.getStatus();
		} finally {
			monitor.done();
		}
		return new Status(IStatus.OK, MercurialEclipsePlugin.ID, "Update to revision " + revision
				+ " succeeded.");
	}

	private IStatus handleMultipleHeads(final IProgressMonitor monitor) {
		final IStatus[] status = new IStatus[1];
		status[0] = new Status(IStatus.OK, MercurialEclipsePlugin.ID, "Update canceled - merge needed");

		Display.getDefault().syncExec(new Runnable() {

			public void run() {
				try {
					int extraHeads = MergeHandler.getOtherHeadsInCurrentBranch(hgRoot).size();
					if (extraHeads == 1) {
						boolean mergeNow = MessageDialog.openQuestion(null,
								"Multiple heads", "You have one extra head in current branch. Do you want to merge now?");

						if (mergeNow) {
							MergeHandler.determineMergeHeadAndMerge(hgRoot, Display.getDefault().getActiveShell(), monitor, false, true);
						}
					} else {
						MessageDialog.openInformation(null,
								"Multiple heads", "Can't update to tip. "
								+ "You have " + extraHeads + " extra heads in current branch. Consider merging manually");
					}
				} catch (CoreException e) {
					MercurialEclipsePlugin.logError(e);
					status[0] = MercurialEclipsePlugin.createStatus(e.getMessage(), 0, IStatus.ERROR, e);
				}
			}
		});

		return status[0];
	}

}