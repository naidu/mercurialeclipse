/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.compare;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.ICompareNavigator;
import org.eclipse.compare.ResourceNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgLogClient;
import com.vectrace.MercurialEclipse.commands.HgParentClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.FileFromChangeSet;
import com.vectrace.MercurialEclipse.team.MercurialRevisionStorage;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

public class HgCompareEditorInput extends CompareEditorInput {
	private static final Differencer DIFFERENCER = new Differencer();

	private final ResourceNode left;
	private final ResourceNode ancestor;
	private final ResourceNode right;

	private final ISynchronizePageConfiguration syncConfig;

	private final IFile resource;

	/**
	 * Does either a 2-way or 3-way compare, depending on if one is an ancestor
	 * of the other. If they are divergent, then it finds the common ancestor
	 * and does 3-way compare.
	 * @param syncConfig
	 */
	public HgCompareEditorInput(CompareConfiguration configuration,
			IFile resource, ResourceNode left, ResourceNode right, ISynchronizePageConfiguration syncConfig) {
		super(configuration);
		this.resource = resource;
		this.left = left;
		this.syncConfig = syncConfig;
		this.ancestor = findParentNodeIfExists(resource, left, right);
		this.right = right;
		setTitle(resource.getName());
		configuration.setLeftLabel(getLabel(left));
		// if left isn't a RevisionNode, then it must be the one on the filesystem
		configuration.setLeftEditable(!(left instanceof RevisionNode));
		configuration.setRightLabel(getLabel(right));
		configuration.setRightEditable(false);
	}


	private ResourceNode findParentNodeIfExists(IFile file, ResourceNode l, ResourceNode r) {
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

	public HgCompareEditorInput(CompareConfiguration configuration,
			IFile leftResource, ResourceNode ancestor, ResourceNode right, boolean localEditable) {
		super(configuration);
		this.syncConfig = null;
		this.left = new ResourceNode(leftResource);
		this.ancestor = ancestor;
		this.right = right;
		this.resource = leftResource;
		setTitle(left.getName());
		configuration.setLeftLabel(getLabel(left));
		configuration.setLeftEditable(localEditable);
		if(ancestor != null) {
			configuration.setAncestorLabel(getLabel(ancestor));
		}
		configuration.setRightLabel(getLabel(right));
		configuration.setRightEditable(false);
	}

	private static String getLabel(ResourceNode node) {
		if (node instanceof RevisionNode) {
			return ((RevisionNode) node).getLabel();
		}
		return node.getName();
	}

	@Override
	protected Object prepareInput(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		byte[] content = null;
		ResourceNode parent = ancestor;
		if(parent != null){
			content = parent.getContent();
		}
		if(content == null || content.length == 0){
			// See issue 11149: sometimes we fail to determine the ancestor version, but we
			// see it too late as the editor is opened with an "empty" parent node.
			// In such case ENTIRE file is considered as a huge merge conflict.
			// So as quick and dirty workaround we avoid using 3-way merge if parent content is unknown
			parent = null;
		}
		return DIFFERENCER.findDifferences(parent != null, monitor, null, parent, left, right);
	}


	@Override
	public String getOKButtonLabel() {
		if (getCompareConfiguration().isLeftEditable() || getCompareConfiguration().isRightEditable()) {
			return "Save Changes";
		}
		return super.getOKButtonLabel();
	}

	@Override
	public void saveChanges(IProgressMonitor monitor) throws CoreException {
		boolean save = isSaveNeeded();
		super.saveChanges(monitor);
		if(save) {
			((IFile) left.getResource()).setContents(left.getContents(), true, true, monitor);
		}
	}

//	/**
//	 *  Overriden to allow navigation through multiple changes in the sync view via shortcuts
//	 *  "Ctrl + ." (Navigate->Next) or "Ctrl + ," (Navigate->Previous).
//	 *  @see SyncInfoCompareInput
//	 */
//	@Override
//	public synchronized ICompareNavigator getNavigator() {
//		ICompareNavigator navigator = super.getNavigator();
//		if (syncConfig != null && isSelectedInSynchronizeView()) {
//			ICompareNavigator nav = (ICompareNavigator) syncConfig
//					.getProperty(SynchronizePageConfiguration.P_NAVIGATOR);
//			return new SyncNavigatorWrapper(navigator, nav);
//		}
//		return navigator;
//	}

	private class SyncNavigatorWrapper implements ICompareNavigator {

		private final ICompareNavigator textDfiffDelegate;
		private final ICompareNavigator syncViewDelegate;

		public SyncNavigatorWrapper(ICompareNavigator textDfiffDelegate,
				ICompareNavigator syncViewDelegate) {
			this.textDfiffDelegate = textDfiffDelegate;
			this.syncViewDelegate = syncViewDelegate;
		}

		public boolean selectChange(boolean next) {
			boolean endReached = textDfiffDelegate.selectChange(next);
			if(endReached && syncViewDelegate != null && isSelectedInSynchronizeView()){
				// forward navigation to the sync view
				return syncViewDelegate.selectChange(next);
			}
			return endReached;
		}

	}

	private boolean isSelectedInSynchronizeView() {
		if (syncConfig == null || resource == null) {
			return false;
		}
		ISelection s = syncConfig.getSite().getSelectionProvider().getSelection();
		if (!(s instanceof IStructuredSelection)) {
			return false;
		}
		IStructuredSelection ss = (IStructuredSelection) s;
		Object element = ss.getFirstElement();
		if (element instanceof FileFromChangeSet) {
			FileFromChangeSet sime = (FileFromChangeSet) element;
			return resource.equals(sime.getFile());
		}
		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ancestor == null) ? 0 : ancestor.hashCode());
		result = prime * result + ((left == null) ? 0 : left.hashCode());
		result = prime * result + ((right == null) ? 0 : right.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof HgCompareEditorInput)) {
			return false;
		}
		HgCompareEditorInput other = (HgCompareEditorInput) obj;
		if (ancestor == null) {
			if (other.ancestor != null) {
				return false;
			}
		} else if (!ancestor.equals(other.ancestor)) {
			return false;
		}
		if (left == null) {
			if (other.left != null) {
				return false;
			}
		} else if (!left.equals(other.left)) {
			return false;
		}
		if (right == null) {
			if (other.right != null) {
				return false;
			}
		} else if (!right.equals(other.right)) {
			return false;
		}
		return true;
	}


}