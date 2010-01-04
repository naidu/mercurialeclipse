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
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.window.Window;
import org.eclipse.team.internal.ui.IPreferenceIds;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.IWorkbenchPage;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.compare.HgCompareEditorInput;
import com.vectrace.MercurialEclipse.compare.RevisionNode;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.team.MercurialRevisionStorage;

/**
 * This class helps to invoke the compare facilities of Eclipse.
 * @author bastian
 */
public class CompareUtils {
	public static void openEditor(IFile file, ChangeSet changeset, boolean dialog, boolean localEditable) {
		int changesetIndex = changeset == null ? 0 : changeset.getChangesetIndex();
		String changesetId = changeset == null ? null : changeset.getChangeset();

		openEditor(file, new MercurialRevisionStorage(file, changesetIndex, changesetId, changeset), dialog, localEditable);
	}

	public static void openEditor(IFile file, ChangeSet changeset, boolean localEditable) {
		int changesetIndex = changeset == null ? 0 : changeset.getChangesetIndex();
		String changesetId = changeset == null ? null : changeset.getChangeset();

		openEditor(file, new MercurialRevisionStorage(file, changesetIndex, changesetId, changeset), false, localEditable);
	}

	public static void openEditor(IResource file, MercurialRevisionStorage right, boolean dialog, boolean localEditable) {
		ResourceNode leftNode = null;
		if (file != null) {
			leftNode = new ResourceNode(file);
		}
		openEditor(leftNode, getNode(right), dialog, localEditable);
	}

	public static void openEditor(MercurialRevisionStorage left, MercurialRevisionStorage right, boolean dialog, boolean localEditable) {
		ResourceNode leftNode, rightNode;
		if (right == null) {
			// comparing with file-system
			rightNode = getNode(left);
			leftNode = new ResourceNode(rightNode.getResource());
		} else {
			leftNode = getNode(left);
			rightNode = getNode(right);
		}
		openEditor(leftNode, rightNode, dialog, localEditable);
	}

	@SuppressWarnings("restriction")
	public static void openEditor(ResourceNode left, ResourceNode right, boolean dialog, boolean localEditable) {
		Assert.isNotNull(right);

		if (dialog) {
			openCompareDialog(left, right, localEditable);
			return;
		}

		CompareEditorInput compareInput = getCompareInput(left, right, localEditable);
		if (compareInput == null) {
			return;
		}

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
			return;
		}

		// re-use existing editor enforces Eclipse to re-compare the both sides
		// even if the compare editor already opened the file. The point is, that the
		// file may be changed by user after opening the compare editor and so editor
		// still shows "old" diff state and to be updated. See also issue #10757.
		CompareUI.reuseCompareEditor(compareInput, (IReusableEditor) editor);

		// provide focus to editor
		workBenchPage.activate(editor);
	}

	/**
	 * Opens a compare dialog and returns it. Unfortunately the dialog is
	 * internal Eclipse API, so it might change in future Eclipse versions.
	 *
	 * @param left
	 *            the left ResourceNode to determine the compare editor input
	 * @param right
	 *            the right ResourceNode to determine the compare editor input
	 */
	public static int openCompareDialog(ResourceNode left, ResourceNode right, boolean localEditable) {
		CompareEditorInput compareInput = getCompareInput(left, right, localEditable);
		return openCompareDialog(compareInput);
	}

	/**
	 * Opens a compare dialog using the given input.
	 *
	 * @param compareInput
	 * @return
	 */
	public static int openCompareDialog(CompareEditorInput compareInput) {
		CompareUI.openCompareDialog(compareInput);
		return Window.CANCEL;
	}

	public static CompareEditorInput getCompareInput(ResourceNode left, ResourceNode right, boolean localEditable) {
		IFile resource = (IFile) right.getResource();
		// switch left to right if left is null and put local to left
		ResourceNode leftNode = left != null ? left : right;
		ResourceNode rightNode = left != null ? right : new ResourceNode(resource);

		return new HgCompareEditorInput(new CompareConfiguration(), resource, leftNode, rightNode);
	}

	private static RevisionNode getNode(MercurialRevisionStorage rev) {
		return rev == null ? null : new RevisionNode(rev);
	}
}
