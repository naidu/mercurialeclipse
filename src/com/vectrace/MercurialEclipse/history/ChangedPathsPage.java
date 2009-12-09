/*******************************************************************************
 * Copyright (c) 2007-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - reference
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.history;

import static com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants.*;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.team.ui.history.IHistoryPageSite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.wizards.Messages;

public class ChangedPathsPage {

	private final static String IMG_COMMENTS = "comments.gif"; //$NON-NLS-1$
	private final static String IMG_AFFECTED_PATHS_FLAT_MODE = "flatLayout.gif"; //$NON-NLS-1$

	private SashForm mainSashForm;
	private SashForm innerSashForm;

	private boolean showComments;
	private boolean showAffectedPaths;
	private boolean wrapCommentsText;

	private StructuredViewer changePathsViewer;
	private TextViewer textViewer;

	private final IPreferenceStore store = MercurialEclipsePlugin.getDefault()
			.getPreferenceStore();
	private ToggleAffectedPathsOptionAction[] toggleAffectedPathsLayoutActions;

	private final MercurialHistoryPage page;

	public ChangedPathsPage(MercurialHistoryPage page, Composite parent) {
		this.page = page;
		init(parent);
	}

	private void init(Composite parent) {
		this.showComments = store.getBoolean(PREF_SHOW_COMMENTS);
		this.wrapCommentsText = store.getBoolean(PREF_WRAP_COMMENTS);
		this.showAffectedPaths = store.getBoolean(PREF_SHOW_PATHS);

		this.mainSashForm = new SashForm(parent, SWT.VERTICAL);
		this.mainSashForm.setLayoutData(new GridData(
				GridData.FILL_BOTH));

		this.toggleAffectedPathsLayoutActions = new ToggleAffectedPathsOptionAction[] {
				new ToggleAffectedPathsOptionAction(this,
						"HistoryView.affectedPathsHorizontalLayout", //$NON-NLS-1$
						PREF_AFFECTED_PATHS_LAYOUT, LAYOUT_HORIZONTAL),
				new ToggleAffectedPathsOptionAction(this,
						"HistoryView.affectedPathsVerticalLayout", //$NON-NLS-1$
						PREF_AFFECTED_PATHS_LAYOUT, LAYOUT_VERTICAL), };

	}


	public void createControl() {
		createAffectedPathsViewer();
		addSelectionListener();
		contributeActions();
	}

	private void addSelectionListener() {
		page.getTableViewer().addSelectionChangedListener(
				new ISelectionChangedListener() {
					private Object currentLogEntry;

					public void selectionChanged(SelectionChangedEvent event) {
						ISelection _selection = event.getSelection();
						Object logEntry = ((IStructuredSelection) _selection)
								.getFirstElement();
						if (logEntry != currentLogEntry) {
							this.currentLogEntry = logEntry;
							updatePanels(_selection);
						}
					}
				});
	}

	private void createAffectedPathsViewer() {
		int[] weights = null;
		weights = mainSashForm.getWeights();
		if (innerSashForm != null) {
			innerSashForm.dispose();
		}
		if (changePathsViewer != null) {
			changePathsViewer.getControl().dispose();
		}

		int layout = store.getInt(PREF_AFFECTED_PATHS_LAYOUT);

		if (layout == LAYOUT_HORIZONTAL) {
			innerSashForm = new SashForm(mainSashForm, SWT.HORIZONTAL);
		} else {
			innerSashForm = new SashForm(mainSashForm, SWT.VERTICAL);
			createText(innerSashForm);
		}

		changePathsViewer = new ChangePathsTableProvider(innerSashForm, this);

		if (layout == LAYOUT_HORIZONTAL) {
			createText(innerSashForm);
		}

		setViewerVisibility();

		innerSashForm.layout();
		if (weights != null && weights.length == 2) {
			mainSashForm.setWeights(weights);
		}
		mainSashForm.layout();

		updatePanels(page.getTableViewer().getSelection());
	}

	private void updatePanels(ISelection selection) {
		if (selection == null || !(selection instanceof IStructuredSelection)) {
			textViewer.setDocument(new Document("")); //$NON-NLS-1$
			changePathsViewer.setInput(null);
			return;
		}
		IStructuredSelection ss = (IStructuredSelection) selection;
		if (ss.size() != 1) {
			textViewer.setDocument(new Document("")); //$NON-NLS-1$
			changePathsViewer.setInput(null);
			return;
		}
		MercurialRevision entry = (MercurialRevision) ss.getFirstElement();
		textViewer.setDocument(new Document(entry.getChangeSet()
				.getComment()));
		changePathsViewer.setInput(entry);
	}

	/**
	 * Create the TextViewer for the logEntry comments
	 */
	private void createText(Composite parent) {
		SourceViewer result = new SourceViewer(parent, null, null, true,
				SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.READ_ONLY);
		result.getTextWidget().setIndent(2);

		this.textViewer = result;

		// Create actions for the text editor (copy and select all)
		final TextViewerAction copyAction = new TextViewerAction(
				this.textViewer, ITextOperationTarget.COPY);
		copyAction.setText(Messages.getString("HistoryView.copy")); //$NON-NLS-1$

		this.textViewer
				.addSelectionChangedListener(new ISelectionChangedListener() {
					public void selectionChanged(SelectionChangedEvent event) {
						copyAction.update();
					}
				});

		final TextViewerAction selectAllAction = new TextViewerAction(
				this.textViewer, ITextOperationTarget.SELECT_ALL);
		selectAllAction.setText(Messages.getString("HistoryView.selectAll")); //$NON-NLS-1$

		IHistoryPageSite parentSite = getHistoryPageSite();
		IPageSite pageSite = parentSite.getWorkbenchPageSite();
		IActionBars actionBars = pageSite.getActionBars();

		actionBars.setGlobalActionHandler(ITextEditorActionConstants.COPY,
				copyAction);
		actionBars.setGlobalActionHandler(
				ITextEditorActionConstants.SELECT_ALL, selectAllAction);
		actionBars.updateActionBars();

		// Contribute actions to popup menu for the comments area
		{
			MenuManager menuMgr = new MenuManager();
			menuMgr.setRemoveAllWhenShown(true);
			menuMgr.addMenuListener(new IMenuListener() {
				public void menuAboutToShow(IMenuManager menuMgr1) {
					menuMgr1.add(copyAction);
					menuMgr1.add(selectAllAction);
				}
			});

			StyledText text = this.textViewer.getTextWidget();
			Menu menu = menuMgr.createContextMenu(text);
			text.setMenu(menu);
		}
	}

	private void contributeActions() {

		Action toggleShowComments = new Action(Messages
				.getString("HistoryView.showComments"), //$NON-NLS-1$
				MercurialEclipsePlugin.getImageDescriptor(IMG_COMMENTS)) {
			@Override
			public void run() {
				showComments = isChecked();
				setViewerVisibility();
				store.setValue(PREF_SHOW_COMMENTS, showComments);
			}
		};
		toggleShowComments.setChecked(showComments);

		// Toggle wrap comments action
		Action toggleWrapCommentsAction = new Action(Messages
				.getString("HistoryView.wrapComments")) { //$NON-NLS-1$
			@Override
			public void run() {
				wrapCommentsText = isChecked();
				setViewerVisibility();
				store.setValue(PREF_WRAP_COMMENTS, wrapCommentsText);
			}
		};
		toggleWrapCommentsAction.setChecked(wrapCommentsText);

		// Toggle path visible action
		Action toggleShowAffectedPathsAction = new Action(Messages
				.getString("HistoryView.showAffectedPaths"), //$NON-NLS-1$
				MercurialEclipsePlugin
						.getImageDescriptor(IMG_AFFECTED_PATHS_FLAT_MODE)) {
			@Override
			public void run() {
				showAffectedPaths = isChecked();
				setViewerVisibility();
				store.setValue(PREF_SHOW_PATHS, showAffectedPaths);
			}
		};
		toggleShowAffectedPathsAction.setChecked(showAffectedPaths);

		IHistoryPageSite parentSite = getHistoryPageSite();
		IPageSite pageSite = parentSite.getWorkbenchPageSite();
		IActionBars actionBars = pageSite.getActionBars();

		// Contribute toggle text visible to the toolbar drop-down
		IMenuManager actionBarsMenu = actionBars.getMenuManager();
		actionBarsMenu.add(toggleWrapCommentsAction);
		actionBarsMenu.add(new Separator());
		actionBarsMenu.add(toggleShowComments);
		actionBarsMenu.add(toggleShowAffectedPathsAction);

		actionBarsMenu.add(new Separator());
		for (int i = 0; i < toggleAffectedPathsLayoutActions.length; i++) {
			actionBarsMenu.add(toggleAffectedPathsLayoutActions[i]);
		}

		// Create the local tool bar
		IToolBarManager tbm = actionBars.getToolBarManager();
		tbm.add(new Separator());
		tbm.add(toggleShowComments);
		tbm.add(toggleShowAffectedPathsAction);
		tbm.update(false);

		actionBars.updateActionBars();
	}

	private void setViewerVisibility() {
		if (showComments && showAffectedPaths) {
			mainSashForm.setMaximizedControl(null);
			innerSashForm.setMaximizedControl(null);
		} else if (showComments) {
			mainSashForm.setMaximizedControl(null);
			innerSashForm.setMaximizedControl(textViewer.getTextWidget());
		} else if (showAffectedPaths) {
			mainSashForm.setMaximizedControl(null);
			innerSashForm.setMaximizedControl(changePathsViewer.getControl());
		} else {
			mainSashForm.setMaximizedControl(page.getTableViewer().getControl().getParent());
		}
		changePathsViewer.refresh();
		textViewer.getTextWidget().setWordWrap(wrapCommentsText);
	}

	public static class ToggleAffectedPathsOptionAction extends Action {
		private final ChangedPathsPage page;
		private final String preferenceName;
		private final int value;

		public ToggleAffectedPathsOptionAction(ChangedPathsPage page,
				String label, String preferenceName, int value) {
			super(Messages.getString(label), AS_RADIO_BUTTON);
			this.page = page;
			this.preferenceName = preferenceName;
			this.value = value;
			IPreferenceStore store = MercurialEclipsePlugin.getDefault()
					.getPreferenceStore();
			setChecked(value == store.getInt(preferenceName));
		}

		@Override
		public void run() {
			if (isChecked()) {
				MercurialEclipsePlugin.getDefault().getPreferenceStore()
						.setValue(preferenceName, value);
				page.createAffectedPathsViewer();
			}
		}

	}

	public MercurialHistoryPage getHistoryPage() {
		return page;
	}

	public IHistoryPageSite getHistoryPageSite() {
		return page.getHistoryPageSite();
	}

	public Composite getControl() {
		return mainSashForm;
	}

	public boolean isShowChangePaths() {
		return showAffectedPaths;
	}

	public MercurialHistory getMercurialHistory() {
		return page.getMercurialHistory();
	}
}
