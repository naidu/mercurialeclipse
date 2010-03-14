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
import org.eclipse.compare.ResourceNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgLogClient;
import com.vectrace.MercurialEclipse.commands.HgParentClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.team.MercurialRevisionStorage;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

public class HgCompareEditorInput extends CompareEditorInput {
	private static final Differencer DIFFERENCER = new Differencer();

	private final ResourceNode left;
	private final ResourceNode ancestor;
	private final ResourceNode right;

	/**
	 * Does either a 2-way or 3-way compare, depending on if one is an ancestor
	 * of the other. If they are divergent, then it finds the common ancestor
	 * and does 3-way compare.
	 */
	public HgCompareEditorInput(CompareConfiguration configuration,
			IFile resource, ResourceNode left, ResourceNode right) {
		super(configuration);
		this.left = left;
		this.ancestor = findParentNodeIfExists(resource, left, right);
		this.right = right;
		setTitle(resource.getName());
		configuration.setLeftLabel(left.getName());
		// if left isn't a RevisionNode, then it must be the one on the filesystem
		configuration.setLeftEditable(!(left instanceof RevisionNode));
		configuration.setRightLabel(right.getName());
		configuration.setRightEditable(false);
	}


	private ResourceNode findParentNodeIfExists(IFile resource, ResourceNode l, ResourceNode r) {
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
							MercurialTeamProvider.getHgRoot(resource),
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
							MercurialTeamProvider.getHgRoot(resource),
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
			ChangeSet tip = HgLogClient.getTip(MercurialTeamProvider.getHgRoot(resource));
			boolean localKnown = tip.getChangesetIndex() >= commonAncestor;
			if(!localKnown){
				// no common ancestor
				return null;
			}
			return new RevisionNode(new MercurialRevisionStorage(resource, commonAncestor));
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
			return null;
		}
	}

	public HgCompareEditorInput(CompareConfiguration configuration,
			IResource leftResource, ResourceNode ancestor, ResourceNode right, boolean localEditable) {
		super(configuration);
		this.left = new ResourceNode(leftResource);
		this.ancestor = ancestor;
		this.right = right;
		setTitle(left.getName());
		configuration.setLeftLabel(left.getName());
		configuration.setLeftEditable(localEditable);
		configuration.setAncestorLabel(ancestor.getName());
		configuration.setRightLabel(right.getName());
		configuration.setRightEditable(false);
	}

	@Override
	protected Object prepareInput(IProgressMonitor monitor)
	throws InvocationTargetException, InterruptedException
	{
		return DIFFERENCER.findDifferences(ancestor != null, monitor, null, ancestor, left, right);
	}


	@Override
	public String getOKButtonLabel() {
		if (getCompareConfiguration().isLeftEditable() || getCompareConfiguration().isRightEditable()) {
			return "Save Changes";
		}
		return super.getOKButtonLabel();
	}

	@Override
	public void saveChanges(IProgressMonitor monitor) throws CoreException
	{
		boolean save = isSaveNeeded();
		super.saveChanges(monitor);
		if(save) {
			((IFile)left.getResource()).setContents(left.getContents(), true, true, monitor);
		}
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