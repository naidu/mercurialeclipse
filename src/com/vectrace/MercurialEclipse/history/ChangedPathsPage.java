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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.team.ui.TeamOperation;
import org.eclipse.team.ui.history.IHistoryPageSite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.actions.BaseSelectionListenerAction;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgPatchClient;
import com.vectrace.MercurialEclipse.commands.HgPatchClient.DiffLineType;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.FileStatus;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;
import com.vectrace.MercurialEclipse.wizards.Messages;

public class ChangedPathsPage {

	private static final String IMG_COMMENTS = "comments.gif"; //$NON-NLS-1$
	private static final String IMG_DIFFS = "diffs.gif"; //$NON-NLS-1$
	private static final String IMG_AFFECTED_PATHS_FLAT_MODE = "flatLayout.gif"; //$NON-NLS-1$

	private SashForm mainSashForm;
	private SashForm innerSashForm;

	private boolean showComments;
	private boolean showAffectedPaths;
	private boolean showDiffs;
	private boolean wrapCommentsText;

	private ChangePathsTableProvider changePathsViewer;
	private TextViewer commentTextViewer;
	private TextViewer diffTextViewer;

	private Object currentPath;

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
		this.showDiffs = store.getBoolean(PREF_SHOW_DIFFS);

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
		addSelectionListeners();
		contributeActions();
	}

	private void addSelectionListeners() {
		page.getTableViewer().addSelectionChangedListener(
				new ISelectionChangedListener() {
					private Object currentLogEntry;
					private int currentNumberOfSelectedElements;

					public void selectionChanged(SelectionChangedEvent event) {
						ISelection selection = event.getSelection();
						Object logEntry = ((IStructuredSelection) selection).getFirstElement();
						int nrOfSelectedElements = ((IStructuredSelection) selection).size();
						if (logEntry != currentLogEntry || nrOfSelectedElements != currentNumberOfSelectedElements) {
							this.currentLogEntry = logEntry;
							this.currentNumberOfSelectedElements = nrOfSelectedElements;
							updatePanels(selection);
						}
					}
				});

		changePathsViewer.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent event) {
				ISelection selection = event.getSelection();
				FileStatus path = (FileStatus) ((IStructuredSelection) selection)
					.getFirstElement();
				if (path != currentPath) {
					ChangedPathsPage.this.currentPath = path;
					inDiffViewerScrollTo(path);
				}

			}

		});
	}

	private void inDiffViewerScrollTo(FileStatus path) {
		if(path == null) {
			return;
		}

		String pathString = path.getRootRelativePath().toPortableString();
		int indexOf = diffTextViewer.getDocument().get().indexOf(pathString);
		if(indexOf != -1) {
			diffTextViewer.setSelectedRange(indexOf, pathString.length());
			try {
				int line = diffTextViewer.getDocument().getLineOfOffset(indexOf);
				diffTextViewer.setTopIndex(line);
				// TODO why is this necessary here??? Color shouldn't change. Or should it?
				applyColoringOnDiffPanel();
			} catch (BadLocationException e) {
				MercurialEclipsePlugin.logError(e);
			}
		}
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

			createText(innerSashForm);
			changePathsViewer = new ChangePathsTableProvider(innerSashForm, this);
			createDiffViewer(innerSashForm);
		} else {
			innerSashForm = new SashForm(mainSashForm, SWT.VERTICAL);

			createText(innerSashForm);
			changePathsViewer = new ChangePathsTableProvider(innerSashForm, this);
			createDiffViewer(innerSashForm);
		}



		updatePanels(page.getTableViewer().getSelection());
		setViewerVisibility();
		innerSashForm.layout();

		if (weights != null && weights.length == 2) {
			mainSashForm.setWeights(weights);
		}
		mainSashForm.layout();
	}

	private void updatePanels(ISelection selection) {
		if (!(selection instanceof IStructuredSelection)) {
			clearTextChangePathsAndDiffTextViewers();
			return;
		}

		IStructuredSelection ss = (IStructuredSelection) selection;
		int nrOfSelectedElements = ss.size();
		if (nrOfSelectedElements == 1) {
			MercurialRevision entry = (MercurialRevision) ss.getFirstElement();
			commentTextViewer.setDocument(new Document(entry.getChangeSet().getComment()));
			changePathsViewer.setInput(entry);
			updateDiffPanelFor(entry, null);
		} else if (nrOfSelectedElements == 2) {
			Object[] selectedElememts = ss.toArray();
			MercurialRevision firstEntry = (MercurialRevision) selectedElememts[1];
			MercurialRevision secondEntry = (MercurialRevision) selectedElememts[0];
			commentTextViewer.setDocument(new Document());
			changePathsViewer.setInput(null);
			updateDiffPanelFor(firstEntry, secondEntry);
		} else {
			clearTextChangePathsAndDiffTextViewers();
		}
	}

	private void clearTextChangePathsAndDiffTextViewers() {
		commentTextViewer.setDocument(new Document("")); //$NON-NLS-1$
		changePathsViewer.setInput(null);
		diffTextViewer.setDocument(new Document(""));
	}

	private void updateDiffPanelFor(MercurialRevision entry, MercurialRevision secondEntry) {
		diffTextViewer.setDocument(new Document(getDiff(entry, secondEntry)));
		applyColoringOnDiffPanel();
	}

	private String getDiff(final MercurialRevision entry, final MercurialRevision secondEntry) {
		final String[] resultHolder = new String[] {""};
			TeamOperation operation = new TeamOperation(page.getSite().getWorkbenchWindow()) {

				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					try {
						resultHolder[0] = HgPatchClient.getDiff(entry.getChangeSet().getHgRoot() , entry, secondEntry);
						Thread.sleep(5000);
					} catch (HgException e) {
						// TODO Check how ExceptionHandling should be done here.
						MercurialEclipsePlugin.logError(e);
						throw new IllegalStateException(e);
					}
				}

				@Override
				protected String getJobName() {
					// TODO Replace this with from resource
					return "Moin Moin!";
				}

				@Override
				protected boolean shouldRun() {
					// TODO Auto-generated method stub
					return super.shouldRun();
				}

			};

			try {
				operation.run();
			} catch (InvocationTargetException e) {
				MercurialEclipsePlugin.logError(e);
			} catch (InterruptedException e) {
				MercurialEclipsePlugin.logError(e);
			}
			return resultHolder[0];
	}

	/**
	 * @param document
	 */
	private void applyColoringOnDiffPanel() {
		IDocument document = diffTextViewer.getDocument();
		int nrOfLines = document.getNumberOfLines();
		for (int i = 0; i < nrOfLines; i++) {
			try {
				IRegion lineInformation = document.getLineInformation(i);
				int offset = lineInformation.getOffset();
				int length = lineInformation.getLength();
				DiffLineType diffLineType = HgPatchClient.getDiffLineType(document.get( offset, length));
				diffTextViewer.setTextColor(getDiffLineColor(diffLineType), offset, length, true);
			} catch (BadLocationException e) {
				MercurialEclipsePlugin.logError(e);
			}
		}
	}

	private Color getDiffLineColor(DiffLineType diffLineType) {
		Display display = this.diffTextViewer.getControl().getDisplay();
		switch (diffLineType) {
		case HEADER:
			return display.getSystemColor(SWT.COLOR_BLUE);
		case ADDED:
			return display.getSystemColor(SWT.COLOR_DARK_GREEN);
		case REMOVED:
			return display.getSystemColor(SWT.COLOR_DARK_RED);
		case META:
			return display.getSystemColor(SWT.COLOR_DARK_GRAY);
		case CONTEXT:
			return display.getSystemColor(SWT.COLOR_BLACK);
		default:
			throw new IllegalStateException("Unexpected DiffLineType: "+diffLineType);
		}
	}

	/**
	 * @return may return null
	 */
	MercurialRevision getCurrentRevision() {
		return (MercurialRevision) changePathsViewer.getInput();
	}

	/**
	 * Create the TextViewer for the logEntry comments
	 */
	private void createText(Composite parent) {
		SourceViewer result = new SourceViewer(parent, null, null, true,
				SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.READ_ONLY);
		result.getTextWidget().setIndent(2);

		this.commentTextViewer = result;

		// Create actions for the text editor (copy and select all)
		final TextViewerAction copyAction = new TextViewerAction(
				this.commentTextViewer, ITextOperationTarget.COPY);
		copyAction.setText(Messages.getString("HistoryView.copy"));

		this.commentTextViewer
				.addSelectionChangedListener(new ISelectionChangedListener() {
					public void selectionChanged(SelectionChangedEvent event) {
						copyAction.update();
					}
				});

		final TextViewerAction selectAllAction = new TextViewerAction(
				this.commentTextViewer, ITextOperationTarget.SELECT_ALL);
		selectAllAction.setText(Messages.getString("HistoryView.selectAll"));

		IHistoryPageSite parentSite = getHistoryPageSite();
		IPageSite pageSite = parentSite.getWorkbenchPageSite();
		IActionBars actionBars = pageSite.getActionBars();

		actionBars.setGlobalActionHandler(ITextEditorActionConstants.COPY,
				copyAction);
		actionBars.setGlobalActionHandler(
				ITextEditorActionConstants.SELECT_ALL, selectAllAction);
		actionBars.updateActionBars();

		// Contribute actions to popup menu for the comments area
		MenuManager menuMgr = new MenuManager();
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager menuMgr1) {
				menuMgr1.add(copyAction);
				menuMgr1.add(selectAllAction);
			}
		});

		StyledText text = this.commentTextViewer.getTextWidget();
		Menu menu = menuMgr.createContextMenu(text);
		text.setMenu(menu);
	}

	private void createDiffViewer(SashForm parent) {
		SourceViewer sourceViewer = new SourceViewer(parent, null, null, true,
				SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.READ_ONLY);
		sourceViewer.getTextWidget().setIndent(2);

		diffTextViewer = sourceViewer;
		diffTextViewer.setDocument(new Document());
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

		Action toggleShowDiffs = new Action(Messages
				// TODO create new text & image
				.getString("HistoryView.showDiffs"), //$NON-NLS-1$
				MercurialEclipsePlugin.getImageDescriptor(IMG_DIFFS)) {
			@Override
			public void run() {
				showDiffs = isChecked();
				setViewerVisibility();
				store.setValue(PREF_SHOW_DIFFS, showDiffs);
			}
		};
		toggleShowDiffs.setChecked(showDiffs);

		// Toggle wrap comments action
		Action toggleWrapCommentsAction = new Action(Messages
				.getString("HistoryView.wrapComments")) {
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
		actionBarsMenu.add(toggleShowDiffs);

		actionBarsMenu.add(new Separator());
		for (int i = 0; i < toggleAffectedPathsLayoutActions.length; i++) {
			actionBarsMenu.add(toggleAffectedPathsLayoutActions[i]);
		}

		// Create the local tool bar
		IToolBarManager tbm = actionBars.getToolBarManager();
		tbm.add(new Separator());
		tbm.add(toggleShowComments);
		tbm.add(toggleShowAffectedPathsAction);
		tbm.add(toggleShowDiffs);
		tbm.update(false);

		actionBars.updateActionBars();

		final BaseSelectionListenerAction openAction = page.getOpenAction();
		final BaseSelectionListenerAction openEditorAction = page.getOpenEditorAction();
		final BaseSelectionListenerAction compareWithCurrent = page.getCompareWithCurrentAction();
		final BaseSelectionListenerAction compareWithPrevious = page.getCompareWithPreviousAction();
		final BaseSelectionListenerAction actionRevert = page.getRevertAction();

		changePathsViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				FileStatus fileStatus = (FileStatus) ((IStructuredSelection) event.getSelection()).getFirstElement();
				MercurialRevision derived = getDerivedRevision(fileStatus, getCurrentRevision());
				if(derived == null){
					return;
				}
				StructuredSelection selection = new StructuredSelection(new Object[]{derived, fileStatus});
				compareWithPrevious.selectionChanged(selection);
				compareWithPrevious.run();
			}
		});

		// Contribute actions to popup menu
		final MenuManager menuMgr = new MenuManager();
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager menuMgr1) {
				IStructuredSelection selection = (IStructuredSelection) changePathsViewer.getSelection();
				if(selection.isEmpty()){
					return;
				}
				FileStatus fileStatus = (FileStatus) selection.getFirstElement();
				MercurialRevision base = getCurrentRevision();
				MercurialRevision derived = getDerivedRevision(fileStatus, base);
				if(derived == null){
					// XXX currently files outside workspace are not supported...
					return;
				}
				selection = new StructuredSelection(derived);
				openAction.selectionChanged(selection);
				openEditorAction.selectionChanged(selection);
				compareWithCurrent.selectionChanged(selection);
				selection = new StructuredSelection(new Object[]{derived, fileStatus});
				compareWithPrevious.selectionChanged(selection);
				menuMgr1.add(openAction);
				menuMgr1.add(openEditorAction);
				menuMgr1.add(new Separator(IWorkbenchActionConstants.GROUP_FILE));
				menuMgr1.add(compareWithCurrent);
				menuMgr1.add(compareWithPrevious);
				menuMgr1.add(new Separator());
				selection = new StructuredSelection(new Object[]{derived});
				actionRevert.selectionChanged(selection);
				menuMgr1.add(actionRevert);
			}

		});
		menuMgr.setRemoveAllWhenShown(true);
		changePathsViewer.getTable().setMenu(menuMgr.createContextMenu(changePathsViewer.getTable()));
	}

	private void setViewerVisibility() {
		// TODO there is some trouble with a missing update after horizontal/vertical change
		boolean lowerPartVisible = showAffectedPaths || showComments ||  showDiffs;
		mainSashForm.setMaximizedControl(lowerPartVisible ? null : getChangesetsTableControl());
		if(!lowerPartVisible) {
			return;
		}

		int[] weights = {
				showComments ? 1 : 0, //
				showAffectedPaths ? 1 : 0, //
				showDiffs ? 1 : 0 //
		};
		innerSashForm.setWeights(weights);

		commentTextViewer.getTextWidget().setWordWrap(wrapCommentsText);
	}

	private Composite getChangesetsTableControl() {
		return page.getTableViewer().getControl().getParent();
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

	/**
	 * @return might return null, if the file is outside Eclipse workspace
	 */
	private MercurialRevision getDerivedRevision(FileStatus fileStatus, MercurialRevision base) {
		IFile file = ResourceUtils.getFileHandle(fileStatus.getAbsolutePath());
		if(file == null){
			return null;
		}
		MercurialRevision derived = new MercurialRevision(base.getChangeSet(), base
				.getGChangeSet(), file, null, null);
		return derived;
	}
}
