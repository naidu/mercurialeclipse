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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.FileFromChangeSet;
import com.vectrace.MercurialEclipse.model.JHgChangeSet;
import com.vectrace.MercurialEclipse.model.WorkingChangeSet;
import com.vectrace.MercurialEclipse.utils.CompareUtils;

public class ActionCompareWithParent extends ActionDelegate  {
	/**
	 * The action has been activated. The argument of the method represents the
	 * 'real' action sitting in the workbench UI.
	 * @throws HgException
	 *
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	@Override
	public void run(IAction action) {
		try {
			List<Object> selectedFiles = getSelectedFileResources();

			if (selectedFiles.size() > 0) {
				final Object firstFile = selectedFiles.get(0);

				if (firstFile instanceof FileFromChangeSet) {
					final ChangeSet cs = ((FileFromChangeSet)firstFile).getChangeset();
					final IFile file = ((FileFromChangeSet)firstFile).getFile();

					if (cs instanceof JHgChangeSet) {

						Job job = new Job("Diff for " + file.getName()) {

							@Override
							protected IStatus run(IProgressMonitor monitor) {

								try {
									CompareUtils.openCompareWithParentEditor((JHgChangeSet) cs,
											file, false, null);
								} catch (HgException e) {
									MercurialEclipsePlugin.logError(e);
									return e.getStatus();
								}

								return Status.OK_STATUS;
							}

						};
						job.schedule();
						return;
					} else if (cs instanceof WorkingChangeSet) { // Uncommitted
						new CompareAction().run(file);
						return;
					}
				}
				new CompareAction().run((IResource) firstFile);
			}
		} catch (Exception e) {
			MercurialEclipsePlugin.logError(e);
			MercurialEclipsePlugin.showError(e);
		}
	}


	protected List<Object> getSelectedFileResources() {
		List<Object> result = new ArrayList<Object>();

		if (selection != null) {
			for (Object o : selection.toList()) {
				Object resource = getFileResource(o);

				if (resource != null) {
					result.add(resource);
				}
			}
		} else if (projectSelection != null) {
			Object resource = getFileResource(projectSelection);

			if (resource != null) {
				result.add(resource);
			}
		}

		return result;
	}
}
