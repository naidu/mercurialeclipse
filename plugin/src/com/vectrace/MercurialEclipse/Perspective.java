/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * svetlana.daragatch	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.console.IConsoleConstants;

public class Perspective implements IPerspectiveFactory {

	/**
	 * @see org.eclipse.ui.IPerspectiveFactory#createInitialLayout(org.eclipse.ui.IPageLayout)
	 */
	public void createInitialLayout(IPageLayout layout) {
	      // Get the editor area.
	      String editorArea = layout.getEditorArea();

	      // Top left: Resource Navigator view
	      IFolderLayout topLeft = layout.createFolder("topLeft", IPageLayout.LEFT, 0.36f, editorArea);

	      topLeft.addView("com.vectrace.MercurialEclipse.repository.RepositoriesView");

	      IFolderLayout bottom = layout.createFolder("bottom", IPageLayout.BOTTOM, .70f, editorArea);

	      bottom.addView("org.eclipse.team.sync.views.SynchronizeView");
	      bottom.addView("org.eclipse.team.ui.GenericHistoryView");
	      bottom.addView("com.vectrace.MercurialEclipse.views.MergeView");
	      bottom.addView("com.vectrace.MercurialEclipse.views.PatchQueueView");

	      //bottom.addView(IConsoleConstants.ID_CONSOLE_VIEW);

	      layout.addActionSet("org.eclipse.pde.ui.SearchActionSet");
	      layout.addActionSet("com.vectrace.MercurialEclipse.actionSet");
	      layout.addActionSet("com.vectrace.MercurialEclipse.patchActionSet");

	      // Window -> Show View menu
	      layout.addShowViewShortcut("com.vectrace.MercurialEclipse.views.MergeView");
	      layout.addShowViewShortcut("com.vectrace.MercurialEclipse.views.PatchQueueView");
	      layout.addShowViewShortcut("com.vectrace.MercurialEclipse.repository.RepositoriesView");
	      layout.addShowViewShortcut("org.eclipse.team.sync.views.SynchronizeView");
	      layout.addShowViewShortcut("org.eclipse.team.ui.GenericHistoryView");

	      layout.addShowViewShortcut(IPageLayout.ID_OUTLINE);
	      layout.addShowViewShortcut(IPageLayout.ID_PROP_SHEET);
	      layout.addShowViewShortcut(IPageLayout.ID_PROBLEM_VIEW);
	      layout.addShowViewShortcut(IPageLayout.ID_PROBLEM_VIEW);
	      layout.addShowViewShortcut("org.eclipse.search.ui.views.SearchView");
	      layout.addShowViewShortcut(IConsoleConstants.ID_CONSOLE_VIEW);

	      // Window -> Open Perspective
	      layout.addPerspectiveShortcut("org.eclipse.jdt.ui.JavaPerspective");
	      layout.addPerspectiveShortcut("org.eclipse.ui.resourcePerspective");
	}
}
