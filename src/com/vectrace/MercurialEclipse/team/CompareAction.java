/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.ResourceNode;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgParentClient;
import com.vectrace.MercurialEclipse.compare.HgCompareEditorInput;
import com.vectrace.MercurialEclipse.compare.RevisionNode;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.utils.Bits;
import com.vectrace.MercurialEclipse.utils.CompareUtils;

/**
 * @author zingo, Jerome Negre <jerome+hg@jnegre.org>
 */
public class CompareAction extends SingleFileAction {

	private boolean mergeEnabled;
	private ISynchronizePageConfiguration syncConfig;

	/**
	 * Empty constructor must be here, otherwise Eclipse wouldn't be able to create the object via reflection
	 */
	public CompareAction() {
		super();
	}

	public CompareAction(IFile file) {
		this();
		this.selection = file;
	}

	@Override
	protected void run(final IFile file) throws TeamException {
		Job job = new Job("Diff for " + file.getName()) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				if (mergeEnabled || isConflict(file)) {
					openMergeEditor(file);
					return Status.OK_STATUS;
				}
				boolean clean = MercurialStatusCache.getInstance().isClean(file);
				if (!clean) {
					compareToLocal(file);
					return Status.OK_STATUS;
				}
				try {
					// get the predecessor version and compare current version with it
					String[] parents = HgParentClient.getParentNodeIds(file);
					ChangeSet cs = LocalChangesetCache.getInstance().getOrFetchChangeSetById(file,
							parents[0]);
					if (cs != null && cs.getChangesetIndex() != 0) {
						parents = cs.getParents();
						if (parents == null || parents.length == 0) {
							parents = HgParentClient.getParentNodeIds(file, cs);
						}
						if (parents != null && parents.length > 0) {
							ChangeSet cs2 = LocalChangesetCache.getInstance()
									.getOrFetchChangeSetById(file, parents[0]);
							if (cs2 != null) {
								CompareUtils.openEditor(file, cs2, true);
								return Status.OK_STATUS;
							}
						}
					}
				} catch (TeamException e) {
					MercurialEclipsePlugin.logError(e);
				}
				// something went wrong. So compare to local
				compareToLocal(file);
				return Status.OK_STATUS;
			}

		};
		job.schedule();
	}

	private void compareToLocal(IFile file) {
		// local workspace version
		ResourceNode leftNode = new ResourceNode(file);
		// mercurial version
		RevisionNode rightNode = new RevisionNode(new MercurialRevisionStorage(file));
		CompareUtils.openEditor(leftNode, rightNode, false, true, syncConfig);
	}

	private void openMergeEditor(IFile file){

		try {
			RevisionNode ancestorNode;
			RevisionNode mergeNode;
			if (isConflict(file)) {
				ChangeSet parent = LocalChangesetCache.getInstance().getWorkingChangeSetParent();

				mergeNode = new RevisionNode(new MercurialRevisionStorage(file));
				if (parent != null) {
					ancestorNode = new RevisionNode(new MercurialRevisionStorage(file, parent.getChangeset()));
				} else {
					ancestorNode = new RevisionNode(new MercurialRevisionStorage(file));
				}
			} else {
				HgRoot hgRoot = MercurialTeamProvider.getHgRoot(file);
				String mergeNodeId = MercurialStatusCache.getInstance().getMergeChangesetId(hgRoot);

				String[] parents = HgParentClient.getParentNodeIds(hgRoot);

				int ancestor = HgParentClient
				.findCommonAncestor(hgRoot, parents[0], parents[1]);

				mergeNode = new RevisionNode(new MercurialRevisionStorage(file, mergeNodeId));
				ancestorNode = new RevisionNode(new MercurialRevisionStorage(file, ancestor));
			}


			final HgCompareEditorInput compareInput = new HgCompareEditorInput(
					new CompareConfiguration(), file, ancestorNode, mergeNode, true);

			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					CompareUI.openCompareEditor(compareInput);
				}
			});
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
			MercurialEclipsePlugin.showError(e);
		}
	}

	public void setEnableMerge(boolean enable) {
		mergeEnabled = enable;
	}

	public void setSynchronizePageConfiguration(ISynchronizePageConfiguration syncConfig){
		this.syncConfig = syncConfig;
	}

	private boolean isConflict(IFile file) {
		Integer status = MercurialStatusCache.getInstance().getStatus(file);
		int sMask = status != null? status.intValue() : 0;
		return Bits.contains(sMask, MercurialStatusCache.BIT_CONFLICT);
	}

}
