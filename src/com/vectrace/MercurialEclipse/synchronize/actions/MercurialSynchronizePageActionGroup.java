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
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.team.internal.ui.synchronize.SynchronizePageConfiguration;
import org.eclipse.team.internal.ui.synchronize.actions.OpenInCompareAction;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipantActionGroup;
import org.eclipse.ui.IActionBars;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;

@SuppressWarnings("restriction")
public class MercurialSynchronizePageActionGroup extends ModelSynchronizeParticipantActionGroup {

	private final IAction expandAction;
	private OpenAction openAction;

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

		String keyOpen = SynchronizePageConfiguration.P_OPEN_ACTION;
		Object property = configuration.getProperty(keyOpen);
		if(property instanceof Action){
			openAction = new OpenAction((Action) property, configuration);
			// override default action used on double click with our custom
			configuration.setProperty(keyOpen, openAction);
		}

		appendToGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU, ISynchronizePageConfiguration.FILE_GROUP,
				new DeleteAction(configuration));

		appendToGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU,
				ISynchronizePageConfiguration.FILE_GROUP,
				new ShowHistorySynchronizeAction("Show History",
						configuration, getVisibleRootsSelectionProvider()));


		appendToGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU,
				"hg.commit",
				new CommitSynchronizeAction("Commit...",
						configuration, getVisibleRootsSelectionProvider()));

		appendToGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU,
				"hg.commit",
				new RevertSynchronizeAction("Revert...",
						configuration, getVisibleRootsSelectionProvider()));

		appendToGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU,
				ISynchronizePageConfiguration.OBJECT_CONTRIBUTIONS_GROUP,
				new PushPullSynchronizeAction("Push",
						configuration, getVisibleRootsSelectionProvider(), false, false));

		appendToGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU,
				ISynchronizePageConfiguration.OBJECT_CONTRIBUTIONS_GROUP,
				new PushPullSynchronizeAction("Pull and Update",
						configuration, getVisibleRootsSelectionProvider(), true, true));

		appendToGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU,
				ISynchronizePageConfiguration.OBJECT_CONTRIBUTIONS_GROUP,
				new PushPullSynchronizeAction("Pull",
						configuration, getVisibleRootsSelectionProvider(), true, false));

	}

	@Override
	public void fillContextMenu(IMenuManager menu) {
		if(menu.find("hg.commit") == null){
			menu.insertBefore(ISynchronizePageConfiguration.NAVIGATE_GROUP, new Separator("hg.commit"));
		}
		super.fillContextMenu(menu);
//		menu.remove("org.eclipse.team.ui.synchronizeLast");
		replaceCompareAction(menu);
	}

	/**
	 * Replaces default "OpenInCompareAction" action with our custom
	 * @param menu
	 * @see OpenInCompareAction
	 * @see ModelSynchronizeParticipantActionGroup
	 */
	private void replaceCompareAction(IMenuManager menu) {
		if(openAction == null){
			return;
		}
		Object[] elements = ((IStructuredSelection)getContext().getSelection()).toArray();
		if (elements.length == 0) {
			return;
		}
		IContributionItem fileGroup = findGroup(menu, ISynchronizePageConfiguration.FILE_GROUP);
		if (fileGroup == null) {
			return;
		}
		IContributionItem[] items = menu.getItems();
		for (IContributionItem ci : items) {
			if(!(ci instanceof ActionContributionItem)){
				continue;
			}
			ActionContributionItem ai = (ActionContributionItem) ci;
			IAction action = ai.getAction();
			if(action instanceof OpenInCompareAction){
				menu.remove(ai);
				openAction.setImageDescriptor(action.getImageDescriptor());
				openAction.setText(action.getText());
				if(menu.find(ShowHistorySynchronizeAction.ID) != null) {
					menu.insertBefore(ShowHistorySynchronizeAction.ID, openAction);
				} else {
					menu.appendToGroup(fileGroup.getId(), openAction);
				}
			}
		}
	}

	@Override
	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		IToolBarManager manager = actionBars.getToolBarManager();
		appendToGroup(manager, ISynchronizePageConfiguration.NAVIGATE_GROUP, expandAction);
	}
}
