/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre - implementation
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgClients;
import com.vectrace.MercurialEclipse.commands.HgLogClient;
import com.vectrace.MercurialEclipse.commands.HgMergeClient;
import com.vectrace.MercurialEclipse.commands.HgResolveClient;
import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.commands.RefreshWorkspaceStatusJob;
import com.vectrace.MercurialEclipse.dialogs.RevisionChooserDialog;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.Branch;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.FlaggedAdaptable;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.views.MergeView;

public class MergeHandler extends RootHandler {

	@Override
	protected void run(HgRoot hgRoot) throws CoreException {
		merge(hgRoot, getShell(), new NullProgressMonitor(), false, true);
	}

	public static String merge(HgRoot hgRoot, Shell shell, IProgressMonitor monitor,
			boolean autoPickOtherHead, boolean showCommitDialog) throws CoreException {

		// can we do the equivalent of plain "hg merge"?
		ChangeSet cs = getOtherHeadInCurrentBranch(hgRoot);
		boolean forced = false;

		String forceMessage = "Forced merge (this will discard all uncommitted changes!)";
		boolean hasDirtyFiles = HgStatusClient.isDirty(hgRoot);
		if (cs != null) {
			if (!autoPickOtherHead) {

				String csSummary = "    Changeset: " + cs.getRevision().toString().substring(0, 20)
						+ "\n    User: " + cs.getUser() + "\n    Date: "
						+ cs.getDateString() + "\n    Summary: " + cs.getSummary();

				String branch = cs.getBranch();
				if (Branch.isDefault(branch)) {
					branch = Branch.DEFAULT;
				}
				String message = MessageFormat.format(Messages
						.getString("MergeHandler.mergeWithOtherHead"), branch, csSummary);

				if(hasDirtyFiles){
					MessageDialogWithToggle dialog = MessageDialogWithToggle.openYesNoQuestion(
							shell, "Merge", message, forceMessage, false, null, null);
					if(dialog.getReturnCode() != IDialogConstants.YES_ID) {
						cs = null;
					}
					forced = dialog.getToggleState();
				} else {
					MessageBox mb = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
					mb.setText("Merge");
					mb.setMessage(message);
					if (mb.open() == SWT.NO) {
						cs = null;
					}
				}
			}
		}

		// have to open the dialog until we get a valid changeset
		while (cs == null) {
			RevisionChooserDialog dialog = new RevisionChooserDialog(shell,
					Messages.getString("MergeHandler.mergeWith"), hgRoot); //$NON-NLS-1$
			dialog.setDefaultShowingHeads(true);
			dialog.setDisallowSelectingParents(true);
			dialog.showForceButton(hasDirtyFiles);
			dialog.setForceChecked(forced);
			dialog.setForceButtonText(forceMessage);
			if (dialog.open() != IDialogConstants.OK_ID) {
				return "";
			}

			cs = dialog.getChangeSet();
			forced = dialog.isForceChecked();
		}

		boolean useExternalMergeTool = Boolean.valueOf(
				HgClients.getPreference(MercurialPreferenceConstants.PREF_USE_EXTERNAL_MERGE,
				"false")).booleanValue(); //$NON-NLS-1$

		String result = HgMergeClient.merge(hgRoot, cs.getRevision().getChangeset(),
				useExternalMergeTool, forced);

		String mergeChangesetId = cs.getChangeset();
		HgStatusClient.setMergeStatus(hgRoot, mergeChangesetId);
		try {
			result += commitMerge(monitor, hgRoot, mergeChangesetId, shell, showCommitDialog);
		} catch (CoreException e) {
			MercurialEclipsePlugin.logError(e);
			MercurialEclipsePlugin.showError(e);
		}
		new RefreshWorkspaceStatusJob(hgRoot, true).schedule();
		return result;
	}

	private static String commitMerge(IProgressMonitor monitor, final HgRoot hgRoot,
			final String mergeChangesetId, final Shell shell, boolean showCommitDialog) throws CoreException {
		boolean commit = true;

		String output = "";
		// check if auto-commit is possible
		List<FlaggedAdaptable> mergeAdaptables = HgResolveClient.list(hgRoot);
		monitor.subTask(com.vectrace.MercurialEclipse.wizards.Messages.getString("PullRepoWizard.pullOperation.mergeStatus")); //$NON-NLS-1$
		for (FlaggedAdaptable flaggedAdaptable : mergeAdaptables) {
			if (flaggedAdaptable.getFlag() == MercurialStatusCache.CHAR_UNRESOLVED) {
				// unresolved files, no auto-commit
				commit = false;
				break;
			}
		}
		monitor.worked(1);

		// always show Merge view, as it offers to abort a merge and revise the automatically merged files
		MergeView view = (MergeView) PlatformUI.getWorkbench()
			.getActiveWorkbenchWindow().getActivePage().showView(MergeView.ID);
		view.clearView();
		view.setCurrentRoot(hgRoot);

		// auto-commit if desired
		if (commit) {
			monitor.subTask(com.vectrace.MercurialEclipse.wizards.Messages.getString("PullRepoWizard.pullOperation.commit")); //$NON-NLS-1$
			output += com.vectrace.MercurialEclipse.wizards.Messages.getString("PullRepoWizard.pullOperation.commit.header"); //$NON-NLS-1$
			if (!showCommitDialog) {
				output += CommitMergeHandler.commitMerge(hgRoot, "Merge with " + mergeChangesetId);
			} else {
				output += new CommitMergeHandler().commitMergeWithCommitDialog(hgRoot, shell);
			}
			monitor.worked(1);
		}
		return output;
	}

	private static ChangeSet getOtherHeadInCurrentBranch(HgRoot hgRoot) throws HgException {
		ChangeSet[] heads = HgLogClient.getHeads(hgRoot);
		// have to be at least two heads total to do easy merge
		if (heads.length < 2) {
			return null;
		}

		ChangeSet currentRevision = LocalChangesetCache.getInstance().getChangesetForRoot(hgRoot);
		if(currentRevision == null){
			return null;
		}
		String branch = currentRevision.getBranch();

		ChangeSet candidate = null;
		for (ChangeSet cs : heads) {
			// must match branch
			if (!Branch.same(branch, cs.getBranch())) {
				continue;
			}
			// can't be the current
			if (cs.equals(currentRevision)) {
				continue;
			}
			// if we have more than one candidate, then have to ask user anyway.
			if (candidate != null) {
				return null;
			}
			candidate = cs;
		}


		return candidate;
	}

}
