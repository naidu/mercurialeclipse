/*******************************************************************************
 * Copyright (c) 2010 Bastian Doetsch.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch	- implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeWorkspaceJob;
import com.vectrace.MercurialEclipse.commands.HgUpdateClient;

/**
 * @author Bastian
 *
 */
final class UpdateJob extends SafeWorkspaceJob {
	private final IProject project;
	private final boolean cleanEnabled;
	private final String revision;

	/**
	 * Job to do a working directory update to the specified version.
	 * @param revision the target revision
	 * @param cleanEnabled if true, discard all local changes.
	 */
	public UpdateJob(String revision, boolean cleanEnabled, IProject project) {
		super("Updating working directory");
		this.project = project;
		this.cleanEnabled = cleanEnabled;
		this.revision = revision;
	}

	@Override
	public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
		String jobName = "Updating project " + project.getName();
		if (revision != null && revision.length()>0) {
			jobName.concat(" to revision " + revision);
		}
		if (cleanEnabled) {
			jobName.concat(" discarding all local changes (-C option).");
		}
		monitor.beginTask(jobName, 1);
		monitor.subTask("Calling Mercurial...");
		HgUpdateClient.update(project, revision, cleanEnabled);
		monitor.worked(1);
		monitor.done();
		return new Status(IStatus.OK, MercurialEclipsePlugin.ID, "Update to revision " + revision
				+ " succeeded.");
	}
}