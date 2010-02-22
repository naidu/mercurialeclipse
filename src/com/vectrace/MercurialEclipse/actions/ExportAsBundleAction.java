/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.FileDialog;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeUiJob;
import com.vectrace.MercurialEclipse.SafeWorkspaceJob;
import com.vectrace.MercurialEclipse.commands.HgBundleClient;
import com.vectrace.MercurialEclipse.history.MercurialHistoryPage;
import com.vectrace.MercurialEclipse.history.MercurialRevision;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

/**
 * @author Bastian
 *
 */
public class ExportAsBundleAction extends Action {
	private final MercurialHistoryPage mhp;
	private String file;
	private String base;
	private final static ImageDescriptor imageDesc = MercurialEclipsePlugin
			.getImageDescriptor("export.gif"); //$NON-NLS-1$

	/**
	 *
	 */
	public ExportAsBundleAction(MercurialHistoryPage mhp) {
		super(
				Messages
						.getString("ExportAsBundleAction.exportSelectedRevisionAsBundle"), imageDesc); //$NON-NLS-1$
		this.mhp = mhp;
	}

	@Override
	public void run() {
		final MercurialRevision rev = getRevision();
		new SafeWorkspaceJob(
				Messages.getString("ExportAsBundleAction.exportingRevision") + rev.getContentIdentifier() + Messages.getString("ExportAsBundleAction.toBundle")) { //$NON-NLS-1$ //$NON-NLS-2$
			@Override
			protected org.eclipse.core.runtime.IStatus runSafe(
					org.eclipse.core.runtime.IProgressMonitor monitor) {
				try {
					monitor
							.beginTask(
									Messages
											.getString("ExportAsBundleAction.exportingRevision") + rev.getContentIdentifier() //$NON-NLS-1$
											+ Messages
													.getString("ExportAsBundleAction.toBundle"), 3); //$NON-NLS-1$
					monitor
							.subTask(Messages
									.getString("ExportAsBundleAction.determiningRepositoryRoot")); //$NON-NLS-1$
					HgRoot root = MercurialTeamProvider.getHgRoot(rev
							.getResource());
					monitor.worked(1);
					monitor
							.subTask(Messages
									.getString("ExportAsBundleAction.callingMercurial")); //$NON-NLS-1$
					SafeUiJob uiJob = new SafeUiJob(
							Messages
									.getString("ExportAsBundleAction.determineLocationForBundleFile")) { //$NON-NLS-1$
						@Override
						protected org.eclipse.core.runtime.IStatus runSafe(
								org.eclipse.core.runtime.IProgressMonitor mon) {
							FileDialog fileDialog = new FileDialog(getDisplay()
									.getActiveShell());
							fileDialog
									.setText(Messages
											.getString("ExportAsBundleAction.pleaseEnterTheNameOfTheBundleFile")); //$NON-NLS-1$
							file = fileDialog.open();
							InputDialog d = new InputDialog(
									getDisplay().getActiveShell(),
									"Please specify the base revision",
									"Please specify the base revision e.g. 1333",
									"0", null);
							d.open();
							base = d.getValue();
							return super.runSafe(mon);
						}
					};
					uiJob.schedule();
					uiJob.join();

					final String bundleResult = HgBundleClient.bundle(root, rev
							.getChangeSet(), null, file,
							false, base);
					monitor.worked(1);
					new SafeUiJob(
							Messages
									.getString("ExportAsBundleAction.createdSuccessfully")) { //$NON-NLS-1$
						@Override
						protected org.eclipse.core.runtime.IStatus runSafe(
								org.eclipse.core.runtime.IProgressMonitor m) {
							String message = Messages
									.getString("ExportAsBundleAction.theRevision") + rev.getContentIdentifier() //$NON-NLS-1$
									+ Messages
											.getString("ExportAsBundleAction.andAllPreviousRevisionsHaveBeenExported") //$NON-NLS-1$
									+ file;
							message = message.concat("\n\n" + bundleResult);
							MessageDialog
									.openInformation(
											getDisplay().getActiveShell(),
											Messages
													.getString("ExportAsBundleAction.createdSuccessfully"), message); //$NON-NLS-1$
							return super.runSafe(m);
						}
					}.schedule();
				} catch (Exception e) {
					MercurialEclipsePlugin.logError(e);
					MercurialEclipsePlugin.showError(e);
				}
				monitor.done();
				return super.runSafe(monitor);
			}
		}.schedule();
		super.run();
	}

	/**
	 * @return
	 */
	private MercurialRevision getRevision() {
		MercurialRevision[] selectedRevisions = mhp.getSelectedRevisions();
		if (selectedRevisions != null && selectedRevisions.length == 1) {
			return selectedRevisions[0];
		}
		ChangeSet cs = mhp.getCurrentWorkdirChangeset();
		return (MercurialRevision) mhp.getMercurialHistory().getFileRevision(
				cs.getChangeset());
	}
}
