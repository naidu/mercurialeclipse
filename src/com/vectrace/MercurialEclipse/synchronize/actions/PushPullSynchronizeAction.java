/*******************************************************************************
 * Copyright (c) 2004, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 *     Bastian Doetsch				- Adaption to Mercurial
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.actions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.team.ui.synchronize.SynchronizeModelAction;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.Branch;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.model.WorkingChangeSet;
import com.vectrace.MercurialEclipse.synchronize.MercurialSynchronizeParticipant;
import com.vectrace.MercurialEclipse.synchronize.cs.ChangesetGroup;
import com.vectrace.MercurialEclipse.synchronize.cs.RepositoryChangesetGroup;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

/**
 * Get action that appears in the synchronize view. It's main purpose is to filter the selection and
 * delegate its execution to the get operation.
 */
public class PushPullSynchronizeAction extends SynchronizeModelAction {

	private final boolean update;
	private final boolean isPull;

	public PushPullSynchronizeAction(String text, ISynchronizePageConfiguration configuration,
			ISelectionProvider selectionProvider, boolean isPull, boolean update) {
		super(text, configuration, selectionProvider);
		this.isPull = isPull;
		this.update = update;
		if (isPull) {
			setImageDescriptor(MercurialEclipsePlugin.getImageDescriptor("actions/update.gif"));
		} else {
			setImageDescriptor(MercurialEclipsePlugin.getImageDescriptor("actions/commit.gif"));
		}
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected SynchronizeModelOperation getSubscriberOperation(
			ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		IStructuredSelection sel = getStructuredSelection();

		// Object input = configuration.getPage().getViewer().getInput();
		// if(input instanceof HgChangeSetModelProvider) {
		// HgChangeSetModelProvider provider = (HgChangeSetModelProvider) input;
		// provider.getSubscriber().getCollector().get
		// }
		ISynchronizeParticipant part = getConfiguration().getParticipant();
		if (part instanceof MercurialSynchronizeParticipant) {
			List<Object> result = new ArrayList<Object>();
			MercurialSynchronizeParticipant participant = (MercurialSynchronizeParticipant) part;
			Set<IHgRepositoryLocation> repositoryLocation = participant.getRepositoryLocation();
			for (IHgRepositoryLocation repos : repositoryLocation) {
				Set<IProject> projects = MercurialEclipsePlugin.getRepoManager()
						.getAllRepoLocationProjects(repos);
				for (IProject proj : projects) {
					result.add(proj);
				}
			}
			HashSet h = new HashSet(result);
			result.clear();
			result.addAll(h);
			PushPullSynchronizeOperation pullupdate = new PushPullSynchronizeOperation(configuration, elements, result, isPull, update);
			return pullupdate;
		}
		// it's guaranteed that we have exact one, allowed element (project, changeset or csGroup)
		List<Object> object = sel.toList();
		return new PushPullSynchronizeOperation(configuration, elements, object, isPull, update);
	}

	@Override
	protected boolean updateSelection(IStructuredSelection selection) {
		boolean updateSelection = super.updateSelection(selection);
		if (!updateSelection) {
			Object[] array = selection.toArray();
			if (selection.size() != 1) {
				return false;
			}
			return isSupported(array[0]);
		}
		return updateSelection;
	}

	public boolean isPull() {
		return isPull;
	}

	private boolean isSupported(Object object) {
		if (object instanceof IProject) {
			return true;
		}
		if (object instanceof ChangesetGroup) {
			ChangesetGroup group = (ChangesetGroup) object;
			return isMatching(group.getDirection()) && !group.getChangesets().isEmpty();
		}
		if (object instanceof RepositoryChangesetGroup) {
			RepositoryChangesetGroup group = (RepositoryChangesetGroup) object;
			if (isPull && group.getIncoming().getChangesets().size() > 0) {
				return true;
			}
			if (!isPull && group.getOutgoing().getChangesets().size() > 0) {
				return true;
			}
		}
		if (object instanceof ChangeSet) {
			ChangeSet changeSet = (ChangeSet) object;
			return !(changeSet instanceof WorkingChangeSet) && isMatching(changeSet.getDirection())
					&& (!update || !isPull || isMatchingBranch(changeSet));
		}
		return false;
	}

	private boolean isMatching(Direction d) {
		return (d == Direction.INCOMING && isPull) || (d == Direction.OUTGOING && !isPull);
	}

	private boolean isMatchingBranch(ChangeSet cs) {
		return Branch.same(MercurialTeamProvider.getCurrentBranch(cs.getHgRoot()), cs.getBranch());
	}
}
