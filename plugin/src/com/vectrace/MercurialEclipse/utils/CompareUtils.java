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

import java.util.SortedSet;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.internal.CompareEditor;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
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

import com.aragost.javahg.Changeset;
import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.commands.HgLocateClient;
import com.vectrace.MercurialEclipse.commands.HgParentClient;
import com.vectrace.MercurialEclipse.commands.HgResolveClient;
import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.compare.HgCompareEditorInput;
import com.vectrace.MercurialEclipse.compare.RevisionNode;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgFile;
import com.vectrace.MercurialEclipse.model.HgResource;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.HgWorkspaceFile;
import com.vectrace.MercurialEclipse.model.HgWorkspaceFolder;
import com.vectrace.MercurialEclipse.model.IChangeSetHolder;
import com.vectrace.MercurialEclipse.model.IHgResource;
import com.vectrace.MercurialEclipse.model.JHgChangeSet;
import com.vectrace.MercurialEclipse.model.NullHgFile;
import com.vectrace.MercurialEclipse.synchronize.MercurialResourceVariant;
import com.vectrace.MercurialEclipse.synchronize.MercurialResourceVariantComparator;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.team.cache.MercurialRootCache;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;

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

	/**
	 * Compare workspace with workspace as it was at a changeset
	 */
	public static void openEditor(IResource resource, JHgChangeSet changeset) throws HgException {
		String changesetId = changeset.getNode();

		IHgResource left = null;
		IHgResource right = null;
		HgRoot root = MercurialRootCache.getInstance().getHgRoot(resource);

		if(resource instanceof IContainer) {
			String inPattern = AbstractClient.getHgResourceSearchPattern(root,
					root.getRelativePath(resource), false);
			SortedSet<String> filter = HgStatusClient.getFiles(HgStatusClient.getStatusMARDU(root,
					changesetId, inPattern));

			left = new HgWorkspaceFolder(root, (IContainer)resource, filter);
			right = HgLocateClient.getHgResources(root, root.getRelativePath(resource), false, changeset, filter);
		} else if (resource instanceof IFile) {
			left = new HgWorkspaceFile(root, (IFile)resource);
			right = HgLocateClient.getHgResources(root, root.getRelativePath(resource), true, changeset, null);
		}

		openEditor(new RevisionNode(left), new RevisionNode(right), false, null);
	}

	public static void openCompareWithParentEditor(JHgChangeSet cs, IFile resource, boolean dialog,
			ISynchronizePageConfiguration configuration) throws HgException {
		CompareUtils.openEditor(HgFile.locate(cs, resource),
				MercurialUtilities.getParentRevision(cs, resource), false, configuration);
	}

	public static void openEditor(HgResource left, HgResource right, boolean dialog,
			ISynchronizePageConfiguration configuration) {

		Assert.isNotNull(left);
		Assert.isNotNull(right);

		RevisionNode leftNode = new RevisionNode(left);
		RevisionNode rightNode = new RevisionNode(right);

		try {
			openEditor(leftNode, rightNode, dialog, configuration);
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
			MercurialEclipsePlugin.showError(e);
		}
	}

	/**
	 * Open a compare editor asynchronously
	 *
	 * @param configuration might be null
	 */
	public static void openEditor(final RevisionNode left, final RevisionNode right,
			final boolean dialog, final ISynchronizePageConfiguration configuration) throws HgException {
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
	public static void openEditor(final IResource left, final IHgResource right,
			final boolean dialog, final ISynchronizePageConfiguration configuration) throws HgException {
		Assert.isNotNull(right);
		openEditor(left, getNode(right, left), dialog, configuration);
	}

	public static void openEditor(final IResource left, final RevisionNode right,
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
			getDisplay().syncExec(runnable);
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
			openCompareDialog(getPrecomputedCompareInput(null, left, null, right));
		} else {
			openEditor(getPrecomputedCompareInput(configuration, left, null, right));
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
		getDisplay().asyncExec(uiAction);
		return Window.CANCEL;
	}

	/**
	 * @param configuration might be null
	 */
	private static CompareEditorInput getCompareInput(RevisionNode left, RevisionNode right,
			ISynchronizePageConfiguration configuration) throws HgException {
		// switch left to right if left is null and put local to left
		RevisionNode leftNode = left != null ? left : right;

		return new HgCompareEditorInput(new CompareConfiguration(), leftNode,
				right, findCommonAncestorIfExists(left, right), configuration);
	}

	public static CompareEditorInput getPrecomputedCompareInput(IResource leftResource,
			IHgResource ancestor, IHgResource right) throws HgException {
		return getPrecomputedCompareInput(null, leftResource, getNode(ancestor, leftResource),
				getNode(right, leftResource));
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

	private static RevisionNode getNode(IHgResource rev, IResource resource) {
		if (rev == null) {
			return null;
		}
		final JHgChangeSet changeSet = rev instanceof IChangeSetHolder ? ((IChangeSetHolder) rev).getChangeSet() : null;
		IHgResource hgresource = null;

		if (changeSet == null) {

			// local resource
			if (resource instanceof IFile) {
				hgresource = new HgWorkspaceFile(rev.getHgRoot(), (IFile)resource);
			}
			if (resource instanceof IContainer) {
				hgresource = new HgWorkspaceFolder(rev.getHgRoot(), (IContainer)resource, null);
			}
		} else {
			try {
				hgresource = HgLocateClient.getHgResources(rev, changeSet.getNode(), null);
			} catch (HgException e) {
				MercurialEclipsePlugin.logError(e);
			}
		}

		// non-existing file
		if (hgresource == null) {
			HgRoot hgRoot = MercurialRootCache.getInstance().getHgRoot(resource);

			hgresource = new NullHgFile(hgRoot, changeSet, hgRoot.getRelativePath(resource));
		}

		return new RevisionNode(hgresource);
	}

	private static RevisionNode findCommonAncestorIfExists(RevisionNode lNode, RevisionNode rNode) throws HgException {
		if (lNode == null || lNode.isWorkingCopy() || rNode.isWorkingCopy()) {
			return null;
		}

		HgRoot hgRoot = lNode.getHgResource().getHgRoot();

		if (hgRoot == null || !(lNode.getHgResource() instanceof IChangeSetHolder)
				|| !(lNode.getHgResource() instanceof IChangeSetHolder)) {
			return null;
		}

		ChangeSet lCS = ((IChangeSetHolder) lNode.getHgResource()).getChangeSet();
		ChangeSet rCS = ((IChangeSetHolder) rNode.getHgResource()).getChangeSet();

		if (lCS == null || rCS == null) {
			return null;
		}

		String commonAncestor = HgParentClient.findCommonAncestor(hgRoot, lCS, rCS);

		if (commonAncestor == null || commonAncestor.equals(lCS.getNode()) ||
				commonAncestor.equals(rCS.getNode())) {
			return null;
		}

		//TODO: should apply filter here and recreate left and right
		IHgResource hgResource = HgLocateClient.getHgResources(lNode.getHgResource(), commonAncestor, null);
		return new RevisionNode(hgResource);
	}

	/**
	 * @param file non null
	 */
	public static void openMergeEditor(final IFile file, boolean workspaceUpdateConflict){
		try {
			IHgResource ancestorNode;
			IHgResource mergeNode;
			HgRoot root = MercurialRootCache.getInstance().getHgRoot(file);

			if (workspaceUpdateConflict) {
				String[] changeSets = HgResolveClient.restartMergeAndGetChangeSetsForCompare(file);
				String otherId = changeSets[1];
				String ancestorId = changeSets[2];

				if (otherId == null || ancestorId == null) {

					getDisplay().asyncExec(new Runnable() {
						public void run() {
							MessageDialog.openError(MercurialEclipsePlugin.getActiveShell(), "Merge error",
									"Couldn't retrieve merge info from Mercurial");
						}
					});

					MercurialEclipsePlugin.logError(new HgException("HgResolveClient returned null revision id"));
					return;
				}

				// TODO: renames
				IPath path = root.getRelativePath(file);

				mergeNode = new HgFile(root, otherId, path);
				ancestorNode = new HgFile(root, ancestorId, path);
			} else {
				HgRoot hgRoot = MercurialTeamProvider.getHgRoot(file);
				if(hgRoot == null) {
					MercurialEclipsePlugin.showError(new IllegalStateException(
							"Failed to find hg root for: " + file.getLocation()));
					return;
				}
				String mergeNodeId = MercurialStatusCache.getInstance().getMergeChangesetId(hgRoot);
				Changeset[] parents = HgParentClient.getParents(hgRoot);
				String ancestor = HgParentClient.findCommonAncestor(hgRoot, parents[0].getNode(), parents[1].getNode());
				IPath path = root.getRelativePath(file);

				if (ancestor == null) {
					throw new HgException("Couldn't calculate common ancestor");
				}

				// TODO: renames
				mergeNode = new HgFile(root, mergeNodeId, path);
				ancestorNode = new HgFile(root, ancestor, path);
			}

			final CompareEditorInput compareInput = getPrecomputedCompareInput(file,
					ancestorNode, mergeNode);

			getDisplay().asyncExec(new Runnable() {
				public void run() {
					CompareUI.openCompareEditor(compareInput);
				}
			});
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
			MercurialEclipsePlugin.showError(e);
		}
	}

	private static Display getDisplay() {
		return MercurialEclipsePlugin.getStandardDisplay();
	}
}
