/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Andrei Loskutov (Intland) - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.history;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.actions.BaseSelectionListenerAction;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgLogClient;
import com.vectrace.MercurialEclipse.commands.HgParentClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.FileStatus;
import com.vectrace.MercurialEclipse.team.MercurialRevisionStorage;
import com.vectrace.MercurialEclipse.team.NullRevision;
import com.vectrace.MercurialEclipse.utils.CompareUtils;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

class CompareRevisionAction extends BaseSelectionListenerAction {

	private Object[] selection;
	private final MercurialHistoryPage page;
	private boolean enableCompareWithPrev;

	CompareRevisionAction(String text, MercurialHistoryPage page) {
		super(text);
		this.page = page;
		setImageDescriptor(MercurialEclipsePlugin.getImageDescriptor("actions/compare_with_local.gif"));
	}

	void setCompareWithPrevousEnabled(boolean enable){
		this.enableCompareWithPrev = enable;
	}

	@Override
	public void run() {
		final MercurialRevisionStorage [] right = new MercurialRevisionStorage [1];
		final MercurialRevisionStorage [] left = new MercurialRevisionStorage [1];
		final IRunnableWithProgress runnable = new IRunnableWithProgress() {

			public void run(IProgressMonitor monitor) throws InvocationTargetException,
					InterruptedException {
				try {
					if(selection.length > 0 && !monitor.isCanceled()){
						left[0] = getStorage((MercurialRevision) selection[0], monitor);
						if(selection.length > 1 && !monitor.isCanceled()){
							if(enableCompareWithPrev && selection[1] instanceof FileStatus){
								FileStatus clickedFileStatus = (FileStatus) selection[1];
								ChangeSet cs = left[0].getChangeSet();
								IPath fileAbsPath = cs.getHgRoot().toAbsolute(clickedFileStatus.getRootRelativePath());
								IFile file = ResourceUtils.getFileHandle(fileAbsPath);
								right[0] = getParentRevision(cs, file);
							} else if(selection[1] instanceof MercurialRevision) {
								right[0] = getStorage((MercurialRevision) selection[1], monitor);
							}
						} else if(enableCompareWithPrev){
							ChangeSet cs = left[0].getChangeSet();
							IFile file = left[0].getResource();
							right[0] = getParentRevision(cs, file);
						}
					}
				} catch (CoreException e) {
					MercurialEclipsePlugin.logError(e);
					throw new InvocationTargetException(e);
				}
				if(monitor.isCanceled()){
					throw new InterruptedException("Cancelled by user");
				}
			}
		};

		ProgressMonitorDialog progress = new ProgressMonitorDialog(Display.getDefault().getActiveShell());
		try {
			progress.run(true, true, runnable);
		} catch (InvocationTargetException e) {
			MercurialEclipsePlugin.logError(e.getCause());
			return;
		} catch (InterruptedException e) {
			// user cancel
			return;
		}

		if(left[0] == null){
			return;
		}

		boolean localEditable = right[0] == null;
		CompareUtils.openEditor(left[0], right[0], false, localEditable);
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

	/**
	 * this can take a lot of time, and UI must take care that it will not be frozen until
	 * the info is fetched...
	 * @param monitor
	 */
	private MercurialRevisionStorage getStorage(MercurialRevision rev, IProgressMonitor monitor) throws CoreException {
		if(rev.getParent() == null){
			// see issue #10302: this is a dirty trick to make sure to get content even
			// if the file was renamed/copied.
			HgLogClient.getLogWithBranchInfo(rev, page.getMercurialHistory(), monitor);
		}
		return (MercurialRevisionStorage) rev.getStorage(monitor);
	}


	private MercurialRevisionStorage getParentRevision(ChangeSet cs, IFile file) {
		MercurialRevisionStorage parentRev;
		String[] parents = cs.getParents();
		if(cs.getRevision().getRevision() == 0){
			parentRev = new NullRevision(file, cs);
		} else if (parents.length == 0) {
			// TODO for some reason, we do not always have right parent info in the changesets
			// If we are on the different branch then the changeset? or if the changeset
			// logs was created for a file, and not each version of a *file* has
			// direct version predecessor. So such tree 20 -> 21 -> 22 works fine,
			// but tree 20 -> 22 seems not to work per default
			// So simply enforce the parents resolving
			try {
				parents = HgParentClient.getParentNodeIds(file, cs);
			} catch (HgException e) {
				MercurialEclipsePlugin.logError(e);
			}
			if (parents.length == 0) {
				parentRev = new NullRevision(file, cs);
			} else {
				parentRev = new MercurialRevisionStorage(file, parents[0]);
			}
		} else {
			parentRev = new MercurialRevisionStorage(file, parents[0]);
		}
		return parentRev;
	}
}