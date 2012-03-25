/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Andrei Loskutov          - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.history;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.actions.BaseSelectionListenerAction;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.FileStatus;
import com.vectrace.MercurialEclipse.model.HgFile;
import com.vectrace.MercurialEclipse.model.HgResource;
import com.vectrace.MercurialEclipse.model.HgWorkspaceFile;
import com.vectrace.MercurialEclipse.model.JHgChangeSet;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.utils.CompareUtils;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

class CompareRevisionAction extends BaseSelectionListenerAction {

	private Object[] selection;
	private boolean enableCompareWithPrev;

	CompareRevisionAction(String text) {
		super(text);
		setImageDescriptor(MercurialEclipsePlugin.getImageDescriptor("actions/compare_with_local.gif")); //$NON-NLS-1$
	}

	void setCompareWithPrevousEnabled(boolean enable){
		this.enableCompareWithPrev = enable;
	}

	@Override
	public void run() {
		final HgResource [] right = new HgResource [1];
		final HgResource [] left = new HgResource [1];
		final Job job = new Job(Messages.CompareRevisionAction_retrievingDiffData) {

			@Override
			public IStatus run(IProgressMonitor monitor) {
				try {
					if(selection.length > 0 && !monitor.isCanceled()){
						MercurialRevision mercRev = (MercurialRevision) selection[0];
						IFile file = (IFile) mercRev.getResource();
						JHgChangeSet cs = mercRev.getChangeSet();

						if(selection.length > 1 && !monitor.isCanceled()) {
							if(enableCompareWithPrev && selection[1] instanceof FileStatus) {
								FileStatus clickedFileStatus = (FileStatus) selection[1];

								file = ResourceUtils.getFileHandle(cs.getHgRoot().toAbsolute(clickedFileStatus.getRootRelativePath()));

								if(file != null) {
									left[0] = HgFile.make(cs, file);
									right[0] = MercurialUtilities.getParentRevision(cs, file);
								}
							} else if(selection[1] instanceof MercurialRevision) {
								left[0] = HgFile.make(cs, file);
								// TODO: file may be renamed between the two revisions
								right[0] = HgFile.make(((MercurialRevision) selection[1]).getChangeSet(), file);
							}
						} else if(enableCompareWithPrev) {
							left[0] = HgFile.make(cs, file);
							right[0] = MercurialUtilities.getParentRevision(cs, file);
						} else {
							left[0] = HgWorkspaceFile.make(file);
							// TODO: file may be renamed between the two revisions
							right[0] = HgFile.make(cs, file);
						}
					}
				} catch (CoreException e) {
					MercurialEclipsePlugin.logError(e);
					return e.getStatus();
				}
				if(monitor.isCanceled()){
					return Status.CANCEL_STATUS;
				}
				return Status.OK_STATUS;
			}
		};

		job.addJobChangeListener(new JobChangeAdapter(){
			@Override
			public void done(IJobChangeEvent event) {
				if(left[0] == null || !event.getResult().isOK()){
					return;
				}

				CompareUtils.openEditor(left[0], right[0], false, null);
			}
		});
		job.schedule();
	}

	@Override
	protected boolean updateSelection(IStructuredSelection sSelection) {
		if(sSelection.size() != 1 && sSelection.size() != 2){
			return false;
		}
		if(sSelection.size() == 1){
			Object element = sSelection.getFirstElement();
			if(element instanceof MercurialRevision){
				MercurialRevision rev = (MercurialRevision) element;
				if(rev.getResource() instanceof IFile){
					selection = sSelection.toArray();
					return true;
				}
			}
			return false;
		} else if(enableCompareWithPrev && sSelection.size() == 2){
			selection = sSelection.toArray();
			return sSelection.toArray()[1] instanceof FileStatus;
		}
		selection = sSelection.toArray();
		return true;
	}
}