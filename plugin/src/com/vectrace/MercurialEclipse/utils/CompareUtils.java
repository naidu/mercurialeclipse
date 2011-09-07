/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch  implementation
 *     Andrei Loskutov - bugfixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.utils;

import java.util.TreeSet;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.internal.CompareEditor;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;
import org.eclipse.team.internal.ui.IPreferenceIds;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SyncInfoCompareInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.progress.UIJob;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.commands.HgLocateClient;
import com.vectrace.MercurialEclipse.commands.HgParentClient;
import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.compare.HgCompareEditorInput;
import com.vectrace.MercurialEclipse.compare.RevisionNode;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgFile;
import com.vectrace.MercurialEclipse.model.HgFolder;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgResource;
import com.vectrace.MercurialEclipse.model.NullHgFile;
import com.vectrace.MercurialEclipse.synchronize.MercurialResourceVariant;
import com.vectrace.MercurialEclipse.synchronize.MercurialResourceVariantComparator;
import com.vectrace.MercurialEclipse.team.MercurialRevisionStorage;
import com.vectrace.MercurialEclipse.team.cache.MercurialRootCache;

/**
 * This class helps to invoke the compare facilities of Eclipse.
 * @author bastian
 */
@SuppressWarnings("restriction")
public final class CompareUtils {

	public static final IResourceVariantComparator COMPARATOR = new MercurialResourceVariantComparator();

	private CompareUtils() {
		// hide constructor of utility class.
	}

	public static void openEditor(IFile file, ChangeSet changeset) throws HgException {
		int changesetIndex = changeset == null ? 0 : changeset.getChangesetIndex();
		String changesetId = changeset == null ? null : changeset.getChangeset();

		openEditor(file, new MercurialRevisionStorage(file, changesetIndex, changesetId, changeset), false, null);
	}

	public static void openEditor(IResource resource, ChangeSet changeset) throws HgException {
		String changesetId = changeset == null ? null : changeset.getChangeset();

		IHgResource left = null;
		IHgResource right = null;
		HgRoot root = MercurialRootCache.getInstance().getHgRoot(resource);

		if(resource instanceof IContainer) {
			String inPattern = AbstractClient.getHgResourceSearchPattern(resource);
			String[] status = HgStatusClient.getStatus(root, changesetId, null, "-mardu", inPattern, null);
			TreeSet<String> filter = HgStatusClient.removeStatusIndicator(status);
			left = new HgFolder(root, (IContainer)resource, filter);
			right = HgLocateClient.getHgResources(resource, changeset, filter);
		} else if (resource instanceof IFile) {
			left = new HgFile(root, (IFile)resource);
			right = HgLocateClient.getHgResources(resource, changeset, null);
		}

		openEditor(new RevisionNode(left), new RevisionNode(right), false, null);
	}

	public static void openEditor(MercurialRevisionStorage left, MercurialRevisionStorage right, boolean dialog) {
		openEditor(left, right, dialog, null);
	}

	public static void openEditor(MercurialRevisionStorage left, MercurialRevisionStorage right,
			boolean dialog, ISynchronizePageConfiguration configuration) {
		if (right == null && left != null) {
			// comparing with file-system
			try {
				openEditor(left.getResource(), left, dialog, configuration);
			} catch (HgException e) {
				MercurialEclipsePlugin.logError(e);
			}
		} else {
			RevisionNode leftNode = getNode(left);
			RevisionNode rightNode = getNode(right);
			openEditor(leftNode, rightNode, dialog, configuration);
		}
	}

	/**
	 * Open a compare editor asynchronously
	 *
	 * @param configuration might be null
	 */
	public static void openEditor(final RevisionNode left, final RevisionNode right,
			final boolean dialog, final ISynchronizePageConfiguration configuration) {
		Assert.isNotNull(right);
		if (dialog) {
			// TODO: is it intentional the config is ignored?
			openCompareDialog(getCompareInput(left, right, null));
		} else {
			openEditor(getCompareInput(left, right, configuration));
		}
	}

	/**
	 * Open a compare editor asynchronously
	 *
	 * @param configuration might be null
	 * @throws HgException
	 */
	public static void openEditor(final IResource left, final MercurialRevisionStorage right,
			final boolean dialog, final ISynchronizePageConfiguration configuration) throws HgException {
		Assert.isNotNull(right);
		if(!left.getProject().isOpen()) {
			final boolean [] open = new boolean[1];
			Runnable runnable = new Runnable() {
				public void run() {
					open[0] = MessageDialog.openQuestion(null, "Compare",
							"To compare selected file, enclosing project must be opened.\n" +
							"Open the appropriate project (may take time)?");
				}
			};
			Display.getDefault().syncExec(runnable);
			if(open[0]) {
				try {
					left.getProject().open(null);
				} catch (CoreException e) {
					MercurialEclipsePlugin.logError(e);
				}
			} else {
				return;
			}
		}
		if (dialog) {
			// TODO: is it intentional the config is ignored?
			openCompareDialog(getPrecomputedCompareInput(null, left, null, getNode(right)));
		} else {
			openEditor(getPrecomputedCompareInput(configuration, left, null, getNode(right)));
		}
	}

	private static void openEditor(final CompareEditorInput compareInput) {
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
	private static CompareEditorInput getCompareInput(RevisionNode left, RevisionNode right,
			ISynchronizePageConfiguration configuration) {
		// switch left to right if left is null and put local to left
		RevisionNode leftNode = left != null ? left : right;

		return new HgCompareEditorInput(new CompareConfiguration(), leftNode,
				right, findCommonAncestorIfExists(left, right), configuration);
	}

	public static CompareEditorInput getPrecomputedCompareInput(IResource leftResource,
			MercurialRevisionStorage ancestor, MercurialRevisionStorage right) throws HgException {
		return getPrecomputedCompareInput(null, leftResource, getNode(ancestor), getNode(right));
	}

	private static CompareEditorInput getPrecomputedCompareInput(
			ISynchronizePageConfiguration configuration, IResource leftResource,
			RevisionNode ancestor, RevisionNode iResourceNode) throws HgException {

		IResourceVariant ancestorRV = getResourceVariant(ancestor);
		IResourceVariant rightRV = getResourceVariant(iResourceNode);

		if (ancestorRV == null) {
			// 2 way diff
			ancestorRV = rightRV;
		}

		SyncInfo syncInfo = new SyncInfo(leftResource, ancestorRV, rightRV, COMPARATOR);

		try {
			syncInfo.init();
		} catch(TeamException e) {
			throw new HgException(e);
		}

		if (configuration == null) {
			return new SyncInfoCompareInput(leftResource.getName(), syncInfo);
		}

		return new SyncInfoCompareInput(configuration, syncInfo);
	}

	private static IResourceVariant getResourceVariant(RevisionNode storage) {
		return storage == null ? null : new MercurialResourceVariant(storage);
	}

	private static RevisionNode getNode(MercurialRevisionStorage rev) {
		if (rev == null) {
			return null;
		}
		IHgResource hgresource = null;
		try {
			hgresource = HgLocateClient.getHgResources(rev.getResource(), rev.getChangeSet(), null);
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
		}
		if (hgresource != null) {
			// existing file
			return new RevisionNode(hgresource);
		}
		// non-existing file
		IFile file = rev.getResource();
		HgRoot hgRoot = MercurialRootCache.getInstance().getHgRoot(file);
		return new RevisionNode(new NullHgFile(hgRoot, rev.getChangeSet(),
				file.getFullPath().makeRelativeTo(hgRoot.getIPath())));
	}

	private static RevisionNode findCommonAncestorIfExists(RevisionNode lNode, RevisionNode rNode) {
		if (lNode.isWorkingCopy() || rNode.isWorkingCopy()) {
			return null;
		}

		HgRoot hgRoot = lNode.getHgResource().getHgRoot();
		ChangeSet lCS = lNode.getHgResource().getChangeSet();
		ChangeSet rCS = rNode.getHgResource().getChangeSet();
		if (hgRoot == null || lCS == null || rCS == null) {
			return null;
		}

		try {
			String commonAncestor = null;

				try {
					commonAncestor = HgParentClient.findCommonAncestor(hgRoot,lCS, rCS)[1];
				} catch (HgException e) {
					// continue
				}

			String lId = lCS.getChangeset();
			String rId = rCS.getChangeset();

			if (commonAncestor == null || commonAncestor.length() == 0){
				try {
					commonAncestor = HgParentClient.findCommonAncestor(hgRoot, lId, rId)[1];
				} catch (HgException e) {
					// continue: no changeset in the local repo, se issue #10616
				}
			}

			if (commonAncestor == null || commonAncestor.equals(lCS.getChangeset()) ||
					commonAncestor.equals(rCS.getChangeset())) {
				return null;
			}

			//TODO: should apply filter here and recreate left and right
			IHgResource hgResource = HgLocateClient.getHgResources(lNode.getHgResource(), commonAncestor, null);
			return new RevisionNode(hgResource);
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
			return null;
		}
	}
}
