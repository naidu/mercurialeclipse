/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *     Andrei Loskutov           - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

import com.aragost.javahg.Changeset;
import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgParentClient;
import com.vectrace.MercurialEclipse.compare.RevisionNode;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgResource;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.MercurialRootCache;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.utils.CompareUtils;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * @author zingo, Jerome Negre <jerome+hg@jnegre.org>
 */
public class CompareAction extends SingleResourceAction {

	private boolean mergeEnabled;
	private ISynchronizePageConfiguration syncConfig;
	private boolean isUncommittedCompare = false;

	// constructors

	/**
	 * Empty constructor must be here, otherwise Eclipse wouldn't be able to create the object via reflection
	 */
	public CompareAction() {
		super();
	}

	public CompareAction(IResource res) {
		this();
		this.selection = res;
	}

	// operations

	/**
	 * @param file non null
	 */
	@Override
	protected void run(final IResource resource) throws TeamException {
		Job job = new Job("Diff for " + resource.getName()) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				boolean workspaceUpdateConflict = isUncommittedCompare && resource instanceof IFile
						&& MercurialStatusCache.getInstance().isWorkspaceUpdateConfict((IFile)resource);

				if (workspaceUpdateConflict) {
					// This job shouldn't run in the UI thread despite needing it a lot because it
					// invokes hg multiple times.
					final Boolean[] resultRef = { Boolean.FALSE };
					getShell().getDisplay().syncExec(new Runnable() {
						public void run() {
							resultRef[0] = Boolean
									.valueOf(MessageDialog
											.openQuestion(
													getShell(),
													"Compare",
													"This file is in conflict. Would you like to use resolve to *RESTART* the merge with 3-way differences? Warning: if resolve fails this operations can potentially lose changes. If in doubt select 'No'."));
						}
					});

					workspaceUpdateConflict = resultRef[0].booleanValue();
				}

				if (mergeEnabled || workspaceUpdateConflict) {
					CompareUtils.openMergeEditor((IFile)resource, workspaceUpdateConflict);
					return Status.OK_STATUS;
				}
				boolean clean = MercurialStatusCache.getInstance().isClean(resource);
				if (!clean) {
					compareToLocal(resource);
					return Status.OK_STATUS;
				}
				try {
					HgRoot root = MercurialRootCache.getInstance().getHgRoot(resource);
					Changeset[] parents = HgParentClient.getParents(root, resource);
					ChangeSet cs = LocalChangesetCache.getInstance().get(root, parents[0]);

					if (cs != null) {
						// TODO: compare with parent on a project?
						if (resource instanceof IFile) {
							CompareUtils.openEditor(resource, MercurialUtilities.getParentRevision(cs, (IFile)resource), false, null);
						}
						return Status.OK_STATUS;
					}
				} catch (TeamException e) {
					MercurialEclipsePlugin.logError(e);
				}
				// something went wrong. So compare to local
				compareToLocal(resource);
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	private void compareToLocal(IResource res) {
		IHgResource right = ResourceUtils.getCleanLocalHgResource(res);
		try {
			CompareUtils.openEditor(res, new RevisionNode(right), false, syncConfig);
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
		}
	}

	public void setEnableMerge(boolean enable) {
		mergeEnabled = enable;
	}

	public void setUncommittedCompare(boolean enable) {
		isUncommittedCompare = enable;
	}

	public void setSynchronizePageConfiguration(ISynchronizePageConfiguration syncConfig){
		this.syncConfig = syncConfig;
	}
}
