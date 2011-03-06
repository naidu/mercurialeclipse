/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch  implementation
 *     Andrei Loskutov (Intland) - bugfixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.utils;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.ResourceNode;
import org.eclipse.compare.internal.CompareEditor;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.internal.ui.IPreferenceIds;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.progress.UIJob;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgLogClient;
import com.vectrace.MercurialEclipse.commands.HgParentClient;
import com.vectrace.MercurialEclipse.compare.HgCompareEditorInput;
import com.vectrace.MercurialEclipse.compare.RevisionNode;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.team.MercurialRevisionStorage;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

/**
 * This class helps to invoke the compare facilities of Eclipse.
 * @author bastian
 */
@SuppressWarnings("restriction")
public final class CompareUtils {

	private CompareUtils() {
		// hide constructor of utility class.
	}

	public static void openEditor(IFile file, ChangeSet changeset) {
		int changesetIndex = changeset == null ? 0 : changeset.getChangesetIndex();
		String changesetId = changeset == null ? null : changeset.getChangeset();

		ResourceNode leftNode = null;
		if (file != null) {
			leftNode = new ResourceNode(file);
		}
		RevisionNode rightNode = getNode(new MercurialRevisionStorage(file, changesetIndex, changesetId, changeset));
		openEditor(leftNode, rightNode, false, null);
	}

	public static void openEditor(MercurialRevisionStorage left, MercurialRevisionStorage right, boolean dialog) {
		openEditor(left, right, dialog, null);
	}

	public static void openEditor(MercurialRevisionStorage left, MercurialRevisionStorage right,
			boolean dialog, ISynchronizePageConfiguration configuration) {
		ResourceNode leftNode;
		ResourceNode rightNode;
		if (right == null) {
			// comparing with file-system
			rightNode = getNode(left);
			leftNode = new ResourceNode(rightNode.getResource());
		} else {
			leftNode = getNode(left);
			rightNode = getNode(right);
		}
		openEditor(leftNode, rightNode, dialog, configuration);
	}

	/**
	 * Open a compare editor asynchronously
	 *
	 * @param configuration might be null
	 */
	public static void openEditor(final ResourceNode left, final ResourceNode right,
			final boolean dialog, final ISynchronizePageConfiguration configuration) {
		Assert.isNotNull(right);
		if (dialog) {
			openCompareDialog(getCompareInput(left, right, null));
			return;
		}

		final CompareEditorInput compareInput = getCompareInput(left, right, configuration);

		UIJob uiDiffJob = new UIJob("Preparing hg diff...") {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {

				IWorkbenchPage workBenchPage = MercurialEclipsePlugin.getActivePage();
				boolean reuse = TeamUIPlugin.getPlugin().getPreferenceStore().getBoolean(
						IPreferenceIds.REUSE_OPEN_COMPARE_EDITOR);
				IEditorPart editor = null;
				if(reuse) {
					IEditorReference[] editorRefs = workBenchPage.getEditorReferences();
					for (IEditorReference ref : editorRefs) {
						IEditorPart part = ref.getEditor(false);
						if(part != null && part instanceof CompareEditor){
							editor = part;
							break;
						}
					}
				}

				if (editor == null) {
					CompareUI.openCompareEditor(compareInput);
					return Status.OK_STATUS;
				}

				// re-use existing editor enforces Eclipse to re-compare the both sides
				// even if the compare editor already opened the file. The point is, that the
				// file may be changed by user after opening the compare editor and so editor
				// still shows "old" diff state and to be updated. See also issue #10757.
				CompareUI.reuseCompareEditor(compareInput, (IReusableEditor) editor);

				// provide focus to editor
				workBenchPage.activate(editor);

				return Status.OK_STATUS;
			}
		};
		uiDiffJob.schedule();
	}

	/**
	 * Opens a compare dialog using the given input.
	 *
	 * @param compareInput
	 * @return
	 */
	private static int openCompareDialog(final CompareEditorInput compareInput) {
		Runnable uiAction = new Runnable() {
			public void run() {
				CompareUI.openCompareDialog(compareInput);
			}
		};
		Display.getDefault().asyncExec(uiAction);
		return Window.CANCEL;
	}

	/**
	 * @param configuration might be null
	 */
	private static CompareEditorInput getCompareInput(ResourceNode left, ResourceNode right,
			ISynchronizePageConfiguration configuration) {
		IFile resource = (IFile) right.getResource();
		// switch left to right if left is null and put local to left
		ResourceNode leftNode = left != null ? left : right;
		ResourceNode rightNode = left != null ? right : new ResourceNode(resource);

		return new HgCompareEditorInput(new CompareConfiguration(), resource, leftNode, rightNode,
				findCommonAncestorIfExists(resource, left, right), configuration);
	}

	public static CompareEditorInput getPrecomputedCompareInput(IFile leftResource,
			ResourceNode ancestor, ResourceNode right) {
		return new HgCompareEditorInput(new CompareConfiguration(), leftResource, right, ancestor);
	}

	private static RevisionNode getNode(MercurialRevisionStorage rev) {
		return rev == null ? null : new RevisionNode(rev);
	}

	private static ResourceNode findCommonAncestorIfExists(IFile file, ResourceNode l, ResourceNode r) {
		if (!(l instanceof RevisionNode && r instanceof RevisionNode)) {
			return null;
		}
		RevisionNode lNode = (RevisionNode) l;
		RevisionNode rNode = (RevisionNode) r;

		try {
			int commonAncestor = -1;
			if(lNode.getChangeSet() != null && rNode.getChangeSet() != null){
				try {
					commonAncestor = HgParentClient.findCommonAncestor(
							MercurialTeamProvider.getHgRoot(file),
							lNode.getChangeSet(), rNode.getChangeSet());
				} catch (HgException e) {
					// continue
				}
			}

			int lId = lNode.getRevision();
			int rId = rNode.getRevision();

			if(commonAncestor == -1){
				try {
					commonAncestor = HgParentClient.findCommonAncestor(
							MercurialTeamProvider.getHgRoot(file),
							Integer.toString(lId), Integer.toString(rId));
				} catch (HgException e) {
					// continue: no changeset in the local repo, se issue #10616
				}
			}

			if (commonAncestor == lId) {
				return null;
			}
			if (commonAncestor == rId) {
				return null;
			}
			ChangeSet tip = HgLogClient.getTip(MercurialTeamProvider.getHgRoot(file));
			boolean localKnown = tip.getChangesetIndex() >= commonAncestor;
			if(!localKnown){
				// no common ancestor
				return null;
			}
			return new RevisionNode(new MercurialRevisionStorage(file, commonAncestor));
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
			return null;
		}
	}
}
