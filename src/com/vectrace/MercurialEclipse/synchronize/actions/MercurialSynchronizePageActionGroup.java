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
 *     Adam Berkes (Intland) - modifications
 *     Ilya Ivanov (Intland) - modifications
 ******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.team.internal.ui.synchronize.SynchronizePageConfiguration;
import org.eclipse.team.internal.ui.synchronize.actions.OpenInCompareAction;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipantActionGroup;
import org.eclipse.ui.IActionBars;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.synchronize.cs.ChangesetGroup;
import com.vectrace.MercurialEclipse.synchronize.cs.HgChangeSetActionProvider;

@SuppressWarnings("restriction")
public class MercurialSynchronizePageActionGroup extends ModelSynchronizeParticipantActionGroup {

	private static final String HG_COMMIT_GROUP = "hg.commit";
	private static final String HG_PUSH_PULL_GROUP = "hg.push.pull";

	// see org.eclipse.ui.IWorkbenchCommandConstants.EDIT_DELETE which was introduced in 3.5
	// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=54581
	// TODO replace with the constant as soon as we drop Eclipse 3.4 support
	public static final String EDIT_DELETE = "org.eclipse.ui.edit.delete";
	private final IAction expandAction;
	private OpenAction openAction;

	public MercurialSynchronizePageActionGroup() {
		super();
		expandAction = new Action("Expand All", MercurialEclipsePlugin.getImageDescriptor("elcl16/expandall.gif")) {
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

		appendToGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU,
				ISynchronizePageConfiguration.FILE_GROUP,
				new OpenMergeEditorAction("Open In Merge Editor",
						configuration, getVisibleRootsSelectionProvider()));

		appendToGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU,
				ISynchronizePageConfiguration.FILE_GROUP,
				new ShowHistorySynchronizeAction("Show History",
						configuration, getVisibleRootsSelectionProvider()));

		appendToGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU,
				HG_COMMIT_GROUP,
				new AddAction("Add...",
						configuration, getVisibleRootsSelectionProvider()));

		appendToGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU,
				HG_COMMIT_GROUP,
				new CommitSynchronizeAction("Commit...",
						configuration, getVisibleRootsSelectionProvider()));

		appendToGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU,
				HG_COMMIT_GROUP,
				new RevertSynchronizeAction("Revert...",
						configuration, getVisibleRootsSelectionProvider()));

		appendToGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU,
				HG_COMMIT_GROUP,
				new ResolveSynchronizeAction("Mark as Resolved",
						configuration, getVisibleRootsSelectionProvider()));

		appendToGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU,
				HG_PUSH_PULL_GROUP,
				new PushPullSynchronizeAction("Push",
						configuration, getVisibleRootsSelectionProvider(), false, false));

		appendToGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU,
				HG_PUSH_PULL_GROUP,
				new PushPullSynchronizeAction("Pull and Update",
						configuration, getVisibleRootsSelectionProvider(), true, true));

		appendToGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU,
				HG_PUSH_PULL_GROUP,
				new PushPullSynchronizeAction("Pull",
						configuration, getVisibleRootsSelectionProvider(), true, false));

	}

	@Override
	public void fillContextMenu(IMenuManager menu) {
		if (menu.find(DeleteAction.HG_DELETE_GROUP) == null) {
			menu.insertBefore(ISynchronizePageConfiguration.NAVIGATE_GROUP, new Separator(
					DeleteAction.HG_DELETE_GROUP));
		}
		if (menu.find(HG_COMMIT_GROUP) == null){
			menu.insertBefore(DeleteAction.HG_DELETE_GROUP, new Separator(HG_COMMIT_GROUP));
		}
		if (menu.find(HG_PUSH_PULL_GROUP) == null){
			menu.insertAfter(ISynchronizePageConfiguration.NAVIGATE_GROUP, new Separator(HG_PUSH_PULL_GROUP));
		}

		addUndoMenu(menu);

		super.fillContextMenu(menu);
//		menu.remove("org.eclipse.team.ui.synchronizeLast");
		replaceCompareAndMoveDeleteAction(menu);
	}


	private void addUndoMenu(IMenuManager menu) {
		MenuManager submenu = new MenuManager("Undo",
				MercurialEclipsePlugin.getImageDescriptor("undo_edit.gif"), null);

		menu.insertBefore(ISynchronizePageConfiguration.NAVIGATE_GROUP, submenu);

		ISelection selection = getContext().getSelection();

		if (!(selection instanceof StructuredSelection)) {
			return;
		}

		StructuredSelection stSelection = (StructuredSelection) selection;

		if (stSelection.size() != 1) {
			return;
		}

		Object object = stSelection.iterator().next();
		if (object instanceof ChangesetGroup) {
			ChangesetGroup csGroup = ((ChangesetGroup) object);
			if (csGroup.getChangesets().isEmpty()) {
				return;
			}

			HgRoot hgRoot = csGroup.getChangesets().iterator().next().getHgRoot();
			submenu.add(new RollbackSynchronizeAction("Rollback", getConfiguration(), hgRoot, null));
			submenu.add(new BackoutSynchronizeAction("Backout...", getConfiguration(), hgRoot, null));
			submenu.add(new StripSynchronizeAction("Strip...", getConfiguration(), hgRoot, null));
		}
	}

	/**
	 * Replaces default "OpenInCompareAction" action with our custom, moves delete action
	 * @see OpenInCompareAction
	 * @see ModelSynchronizeParticipantActionGroup
	 * @see HgChangeSetActionProvider
	 */
	private void replaceCompareAndMoveDeleteAction(IMenuManager menu) {
		if(openAction == null){
			return;
		}
		Object[] elements = ((IStructuredSelection) getContext().getSelection()).toArray();
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
				menu.prependToGroup(fileGroup.getId(), openAction);
			} else if(EDIT_DELETE.equals(action.getActionDefinitionId())){
				menu.remove(ai);
				menu.appendToGroup(DeleteAction.HG_DELETE_GROUP, ai);
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
