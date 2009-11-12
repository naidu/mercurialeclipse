/*******************************************************************************
 * Copyright (c) 2008 MercurialEclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch				- implementation
 *     Andrei Loskutov (Intland) - bug fixes
 *     Zsolt Koppany (Intland)
 ******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipantActionGroup;
import org.eclipse.ui.IActionBars;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;

public class MercurialSynchronizePageActionGroup extends ModelSynchronizeParticipantActionGroup {

	private final IAction expandAction;

	public MercurialSynchronizePageActionGroup() {
		super();
		expandAction = new Action("Expand All", MercurialEclipsePlugin.getImageDescriptor("elcl16/expandall.gif") ) {
			@Override
			public void run() {
				Viewer viewer = getConfiguration().getPage().getViewer();
				if(viewer instanceof AbstractTreeViewer){
					AbstractTreeViewer treeViewer = (AbstractTreeViewer) viewer;
					treeViewer.expandAll();
				}
			}
		};
	}

	@Override
	public void initialize(ISynchronizePageConfiguration configuration) {
		super.initialize(configuration);

		appendToGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU,
				ISynchronizePageConfiguration.OBJECT_CONTRIBUTIONS_GROUP,
				new PushPullSynchronizeAction("Push",
						configuration, getVisibleRootsSelectionProvider(), false, false));

		appendToGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU,
				ISynchronizePageConfiguration.OBJECT_CONTRIBUTIONS_GROUP,
				new CommitSynchronizeAction("Commit...",
						configuration, getVisibleRootsSelectionProvider()));

		appendToGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU,
				ISynchronizePageConfiguration.OBJECT_CONTRIBUTIONS_GROUP,
				new RevertSynchronizeAction("Revert...",
						configuration, getVisibleRootsSelectionProvider()));

		appendToGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU,
				ISynchronizePageConfiguration.OBJECT_CONTRIBUTIONS_GROUP,
				new PushPullSynchronizeAction("Pull",
						configuration, getVisibleRootsSelectionProvider(), true, false));

		appendToGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU,
				ISynchronizePageConfiguration.OBJECT_CONTRIBUTIONS_GROUP,
				new PushPullSynchronizeAction("Pull and Update",
						configuration, getVisibleRootsSelectionProvider(), true, true));

		appendToGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU,
				ISynchronizePageConfiguration.OBJECT_CONTRIBUTIONS_GROUP,
				new ShowHistorySynchronizeAction("Show History",
						configuration, getVisibleRootsSelectionProvider()));
	}

	@Override
	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		IToolBarManager manager = actionBars.getToolBarManager();
		appendToGroup(manager, ISynchronizePageConfiguration.NAVIGATE_GROUP, expandAction);
	}
}
