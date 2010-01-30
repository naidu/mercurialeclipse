/*******************************************************************************
 * Copyright (c) 2007-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     StefanC                   - some updates, code cleanup
 *     Stefan Groschupf          - logError
 *     Subclipse project committers - reference
 *     Charles O'Farrell         - comparison diff
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.history;

import static com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants.PREF_SHOW_ALL_TAGS;

import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.team.core.history.IFileHistory;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.ui.history.HistoryPage;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.BaseSelectionListenerAction;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.actions.OpenMercurialRevisionAction;
import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.menu.UpdateJob;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.ActionRevert;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.wizards.Messages;

public class MercurialHistoryPage extends HistoryPage {

	private GraphLogTableViewer viewer;
	private IResource resource;
	private ChangeLogContentProvider changeLogViewContentProvider;
	private MercurialHistory mercurialHistory;
	private RefreshMercurialHistory refreshFileHistoryJob;
	private ChangedPathsPage changedPaths;
	private ChangeSet currentWorkdirChangeset;
	private OpenMercurialRevisionAction openAction;
	private BaseSelectionListenerAction openEditorAction;
	private boolean showTags;
	private CompareRevisionAction compareWithCurrAction;
	private CompareRevisionAction compareWithPrevAction;
	private CompareRevisionAction compareTwo;
	private BaseSelectionListenerAction revertAction;

	class RefreshMercurialHistory extends Job {
		private final int from;

		public RefreshMercurialHistory(int from) {
			super("Fetching Mercurial revisions..."); //$NON-NLS-1$
			this.from = from;
			setRule(new ExclusiveHistoryRule());
		}

		@Override
		public IStatus run(IProgressMonitor monitor) {
			if (mercurialHistory == null) {
				return Status.OK_STATUS;
			}
			mercurialHistory.setEnableExtraTags(showTags);
			try {
				mercurialHistory.refresh(monitor, from);
				currentWorkdirChangeset = LocalChangesetCache.getInstance().getChangesetByRootId(resource);
			} catch (CoreException e) {
				MercurialEclipsePlugin.logError(e);
				return e.getStatus();
			}

			final Runnable runnable = new Runnable() {
				public void run() {
					viewer.setInput(mercurialHistory);
					viewer.refresh();
				}
			};

			// Internal code copied here from Utils.asyncExec
			if (viewer == null) {
				return Status.OK_STATUS;
			}

			final Control ctrl = viewer.getControl();
			if (ctrl != null && !ctrl.isDisposed()) {
				ctrl.getDisplay().asyncExec(new Runnable() {
					public void run() {
						if (!ctrl.isDisposed()) {
							BusyIndicator.showWhile(ctrl.getDisplay(),
									runnable);
						}
					}
				});
			}
			return Status.OK_STATUS;
		}
	}

	static class ChangeLogContentProvider implements IStructuredContentProvider {
		private IFileRevision[] entries;
		public ChangeLogContentProvider() {
			super();
		}

		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
			entries = null;
		}

		public void dispose() {
			entries = null;
		}

		public Object[] getElements(Object parent) {
			if (entries != null) {
				return entries;
			}

			final IFileHistory fileHistory = (IFileHistory) parent;
			entries = fileHistory.getFileRevisions();
			return entries;
		}
	}

	static class ChangeSetLabelProvider extends LabelProvider implements
			ITableLabelProvider {

		public String getColumnText(Object obj, int index) {
			String ret;

			if (!(obj instanceof MercurialRevision)) {
				return "Type Error"; //$NON-NLS-1$
			}

			MercurialRevision revision = (MercurialRevision) obj;
			ChangeSet changeSet = revision.getChangeSet();

			switch (index) {
			case 1:
				ret = changeSet.toString();
				break;
			case 2:
				ret = revision.getTagsString();
				break;
			case 3:
				ret = changeSet.getBranch();
				break;
			case 4:
				ret = changeSet.getUser();
				break;
			case 5:
				ret = changeSet.getDateString();
				break;
			case 6:
				ret = changeSet.getSummary();
				break;
			default:
				ret = null;
				break;
			}
			return ret;
		}

		public Image getColumnImage(Object obj, int index) {
			return null;
		}
	}

	public MercurialHistoryPage() {
		super();
	}

	public MercurialHistory getMercurialHistory() {
		return mercurialHistory;
	}

	@Override
	public boolean setInput(Object object) {
		if (isValidInput(object)) {
			IResource old = resource;
			this.resource = (IResource)object;
			super.setInput(object);
			if(resource == null || (resource != null && !resource.equals(old))){
				if(resource != null) {
					mercurialHistory = new MercurialHistory(resource);
				} else {
					mercurialHistory = null;
				}
				refresh();
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean inputSet() {
		return true;
	}

	@Override
	public void createControl(Composite parent) {
		IActionBars actionBars = getHistoryPageSite().getWorkbenchPageSite().getActionBars();
		IMenuManager actionBarsMenu = actionBars.getMenuManager();
		final IPreferenceStore store = MercurialEclipsePlugin.getDefault().getPreferenceStore();
		showTags = store.getBoolean(PREF_SHOW_ALL_TAGS);
		Action toggleShowTags = new Action(Messages.getString("HistoryView.showTags"), //$NON-NLS-1$
				MercurialEclipsePlugin.getImageDescriptor("actions/tag.gif")) {
			@Override
			public void run() {
				showTags = isChecked();
				store.setValue(PREF_SHOW_ALL_TAGS, showTags);
				if(mercurialHistory != null) {
					refresh();
				}
			}
		};
		toggleShowTags.setChecked(showTags);
		actionBarsMenu.add(toggleShowTags);
		IToolBarManager tbm = actionBars.getToolBarManager();
		tbm.add(new Separator());
		tbm.add(toggleShowTags);

		changedPaths = new ChangedPathsPage(this, parent);
		createTableHistory(changedPaths.getControl());
		changedPaths.createControl();
		getSite().setSelectionProvider(viewer);
		getSite().getActionBars().setGlobalActionHandler(ActionFactory.COPY.getId(), new Action() {
			@Override
			public void run() {
				copyToClipboard();
			}
		});

	}

	private void createTableHistory(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout0 = new GridLayout();
		layout0.marginHeight = 0;
		layout0.marginWidth = 0;
		composite.setLayout(layout0);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.grabExcessVerticalSpace = true;
		composite.setLayoutData(data);

		viewer = new GraphLogTableViewer(composite, SWT.MULTI | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.VIRTUAL, this);
		Table changeLogTable = viewer.getTable();

		changeLogTable.setLinesVisible(true);
		changeLogTable.setHeaderVisible(true);

		GridData gridData = new GridData(GridData.FILL_BOTH);
		changeLogTable.setLayoutData(gridData);

		TableLayout layout = new TableLayout();
		changeLogTable.setLayout(layout);

		TableColumn column = new TableColumn(changeLogTable, SWT.CENTER);
		column.setText(Messages.getString("MercurialHistoryPage.columnHeader.graph")); //$NON-NLS-1$
		layout.addColumnData(new ColumnWeightData(7, true));
		column = new TableColumn(changeLogTable, SWT.LEFT);
		column.setText(Messages.getString("MercurialHistoryPage.columnHeader.changeset")); //$NON-NLS-1$
		layout.addColumnData(new ColumnWeightData(15, true));
		column = new TableColumn(changeLogTable, SWT.LEFT);
		column.setText(Messages.getString("MercurialHistoryPage.columnHeader.tag")); //$NON-NLS-1$
		layout.addColumnData(new ColumnWeightData(10, true));
		column = new TableColumn(changeLogTable, SWT.LEFT);
		column.setText(Messages.getString("MercurialHistoryPage.columnHeader.branch")); //$NON-NLS-1$
		layout.addColumnData(new ColumnWeightData(10, true));
		column = new TableColumn(changeLogTable, SWT.LEFT);
		column.setText(Messages.getString("MercurialHistoryPage.columnHeader.user")); //$NON-NLS-1$
		layout.addColumnData(new ColumnWeightData(12, true));
		column = new TableColumn(changeLogTable, SWT.LEFT);
		column.setText(Messages.getString("MercurialHistoryPage.columnHeader.date")); //$NON-NLS-1$
		layout.addColumnData(new ColumnWeightData(12, true));
		column = new TableColumn(changeLogTable, SWT.LEFT);
		column.setText(Messages.getString("MercurialHistoryPage.columnHeader.summary")); //$NON-NLS-1$
		layout.addColumnData(new ColumnWeightData(25, true));

		viewer.setLabelProvider(new ChangeSetLabelProvider());
		changeLogViewContentProvider = new ChangeLogContentProvider();
		viewer.setContentProvider(changeLogViewContentProvider);
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				getCompareWithPreviousAction();
				updateActionEnablement();
				if(compareWithPrevAction.isEnabled()) {
					compareWithPrevAction.run();
				}
			}
		});
		contributeActions();
	}

	private void copyToClipboard() {
		Iterator<?> iterator = getSelection().iterator();
		StringBuilder text = new StringBuilder();
		Table table = viewer.getTable();
		for(int columnIndex = 1; columnIndex < table.getColumnCount(); columnIndex++) {
			text.append(table.getColumn(columnIndex).getText()).append('\t');
		}

		String crlf = System.getProperty("line.separator");
		text.append(crlf);

		while(iterator.hasNext()) {
			Object next = iterator.next();
			ITableLabelProvider labelProvider = (ITableLabelProvider) viewer.getLabelProvider();
			for(int columnIndex = 1; columnIndex < table.getColumnCount(); columnIndex++) {
				text.append(labelProvider.getColumnText(next, columnIndex)).append('\t');
			}
			text.append(crlf);
		}
		Clipboard clipboard = null;
		try {
			clipboard = new Clipboard(getSite().getShell().getDisplay());
			clipboard.setContents(new String[]{text.toString()},
					new Transfer[]{ TextTransfer.getInstance() });
		} finally {
			if(clipboard != null){
				clipboard.dispose();
			}
		}
	}

	private IStructuredSelection getSelection() {
		return (IStructuredSelection) viewer.getSelection();
	}

	private void contributeActions() {

		final Action updateAction = new Action(Messages.getString("MercurialHistoryPage.updateAction.name")) { //$NON-NLS-1$
			private MercurialRevision rev;

			@Override
			public void run() {
				if(rev == null){
					return;
				}
				try {
					HgRoot hgRoot = MercurialTeamProvider.getHgRoot(resource);
					Assert.isNotNull(hgRoot);
					if (HgStatusClient.isDirty(hgRoot)) {
						if (!MessageDialog
								.openQuestion(getControl().getShell(),
										"Uncommited Changes",
										"Your hg root has uncommited changes.\nDo you really want to continue?")){
							return;
						}
					}
					UpdateJob job = new UpdateJob(rev.getHash(), true, hgRoot);
					JobChangeAdapter adap = new JobChangeAdapter() {
						@Override
						public void done(IJobChangeEvent event) {
							refresh();
						}
					};
					job.addJobChangeListener(adap);
					job.schedule();
				} catch (HgException e) {
					MercurialEclipsePlugin.logError(e);
				}
			}

			@Override
			public boolean isEnabled() {
				Object[] revs = getSelection().toArray();
				if (revs.length == 1) {
					rev = (MercurialRevision) revs[0];
					return true;
				}
				rev = null;
				return false;
			}
		};
		updateAction.setImageDescriptor(MercurialEclipsePlugin.getImageDescriptor("actions/update.gif"));

		// Contribute actions to popup menu
		final MenuManager menuMgr = new MenuManager();
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager menuMgr1) {
				if(resource instanceof IFile){
					IStructuredSelection sel = updateActionEnablement();
					menuMgr1.add(openAction);
					menuMgr1.add(openEditorAction);
					menuMgr1.add(new Separator(IWorkbenchActionConstants.GROUP_FILE));
					if(sel.size() == 2){
						menuMgr1.add(compareTwo);
					} else {
						menuMgr1.add(compareWithPrevAction);
						menuMgr1.add(compareWithCurrAction);
						menuMgr1.add(new Separator());
						menuMgr1.add(revertAction);
					}
				}
				updateAction.setEnabled(updateAction.isEnabled());
				menuMgr1.add(new Separator());
				menuMgr1.add(updateAction);
			}
		});
		menuMgr.setRemoveAllWhenShown(true);
		viewer.getTable().setMenu(menuMgr.createContextMenu(viewer.getTable()));
	}

	private void createActions() {
		getOpenAction();
		getOpenEditorAction();
		getCompareWithCurrentAction();
		getRevertAction();
		compareTwo = new CompareRevisionAction(Messages.getString("CompareWithEachOtherAction.label"), this){
			@Override
			protected boolean updateSelection(IStructuredSelection selection) {
				if(selection.size() != 2){
					return false;
				}
				return super.updateSelection(selection);
			}
		};
	}

	OpenMercurialRevisionAction getOpenAction() {
		if(openAction != null){
			return openAction;
		}
		openAction = new OpenMercurialRevisionAction("Open Selected Version");
		openAction.setPage(this);
		return openAction;
	}

	BaseSelectionListenerAction getOpenEditorAction() {
		if(openEditorAction != null){
			return openEditorAction;
		}

		openEditorAction = new BaseSelectionListenerAction("Open Current Version") {
			private IFile file;

			@Override
			public void run() {
				if(file == null){
					return;
				}
				try {
					IDE.openEditor(getSite().getPage(), file);
				} catch (PartInitException e) {
					MercurialEclipsePlugin.logError(e);
				}
			}

			@Override
			protected boolean updateSelection(IStructuredSelection selection) {
				Object element = selection.getFirstElement();
				if(element instanceof MercurialHistory){
					MercurialHistory history = (MercurialHistory) element;
					IFileRevision[] revisions = history.getFileRevisions();
					if(revisions.length != 1 || !(revisions[0] instanceof MercurialRevision)){
						file = null;
						return false;
					}
					MercurialRevision rev = (MercurialRevision) revisions[0];
					if(rev.getResource() instanceof IFile){
						file = (IFile) rev.getResource();
						return file.exists();
					}
				} else if (element instanceof MercurialRevision){
					MercurialRevision rev = (MercurialRevision) element;
					if(rev.getResource() instanceof IFile){
						file = (IFile) rev.getResource();
						return file.exists();
					}
				}
				if(resource instanceof IFile){
					file = (IFile) resource;
					return file.exists();
				}
				file = null;
				return false;
			}
		};
		return openEditorAction;
	}

	CompareRevisionAction getCompareWithCurrentAction() {
		if(compareWithCurrAction == null) {
			compareWithCurrAction = new CompareRevisionAction(Messages.getString("CompareAction.label"), this);
			}
		return compareWithCurrAction;
	}

	CompareRevisionAction getCompareWithPreviousAction() {
		if(compareWithPrevAction == null) {
			compareWithPrevAction = new CompareRevisionAction(Messages.getString("CompareWithPreviousAction.label"), this);
			compareWithPrevAction.setImageDescriptor(MercurialEclipsePlugin.getImageDescriptor("compare_view.gif"));
			compareWithPrevAction.setCompareWithPrevousEnabled(true);
		}
		return compareWithPrevAction;
	}

	@Override
	public Control getControl() {
		return changedPaths.getControl();
	}

	@Override
	public void setFocus() {
		// Nothing to see here
	}

	public String getDescription() {
		return resource.getFullPath().toOSString();
	}

	public String getName() {
		return resource.getFullPath().toOSString();
	}

	public boolean isValidInput(Object object) {
		return object == null || object instanceof IResource || getAdapter(IResource.class) != null;
	}

	public ChangeSet getCurrentWorkdirChangeset() {
		return currentWorkdirChangeset;
	}

	public void refresh() {
		if (refreshFileHistoryJob == null) {
			refreshFileHistoryJob = new RefreshMercurialHistory(Integer.MAX_VALUE);
		}

		if (refreshFileHistoryJob.getState() != Job.NONE) {
			refreshFileHistoryJob.cancel();
		}
		scheduleInPage(refreshFileHistoryJob);
	}

	public void scheduleInPage(Job job) {
		IWorkbenchSiteProgressService progressService = getProgressService();

		if (progressService != null) {
			progressService.schedule(job);
		} else {
			job.schedule();
		}
	}

	private IWorkbenchSiteProgressService getProgressService() {
		IWorkbenchSiteProgressService progressService = (IWorkbenchSiteProgressService) getHistoryPageSite()
				.getWorkbenchPageSite().getService(IWorkbenchSiteProgressService.class);
		return progressService;
	}

	@SuppressWarnings("unchecked")
	public Object getAdapter(Class adapter) {
		return null;
	}

	public TableViewer getTableViewer() {
		return viewer;
	}

	private IStructuredSelection updateActionEnablement() {
		createActions();
		IStructuredSelection selection = getSelection();
		openAction.selectionChanged(selection);
		openEditorAction.selectionChanged(selection);
		compareWithCurrAction.selectionChanged(selection);
		compareWithPrevAction.selectionChanged(selection);
		compareTwo.selectionChanged(selection);
		revertAction.selectionChanged(selection);
		return selection;
	}

	public BaseSelectionListenerAction getRevertAction() {
		if (revertAction == null) {
			revertAction = new BaseSelectionListenerAction("Replace Current With Selected") {
				@Override
				public void run() {
					IStructuredSelection selection = getStructuredSelection();
					if (selection.isEmpty()) {
						return;
					}
					ActionRevert revert = new ActionRevert();
					MercurialRevision revision = (MercurialRevision) selection.getFirstElement();
					IResource selectedElement = revision.getResource();
					if (!MercurialStatusCache.getInstance().isClean(selectedElement) &&
							!MessageDialog.openQuestion(getControl().getShell(), "Uncommited Changes",
							"File '" + selectedElement.getName()
									+ "' has uncommited changes.\nDo you really want to revert?")) {
						return;
					}
					selection = new StructuredSelection(selectedElement);
					revert.setChangesetToRevert(revision.getChangeSet());
					revert.selectionChanged(this, selection);
					revert.run(this);
				}

				@Override
				protected boolean updateSelection(IStructuredSelection sSelection) {
					if(sSelection.size() != 1 ){
						return false;
					}
					if(sSelection.size() == 1){
						Object element = sSelection.getFirstElement();
						if(element instanceof MercurialRevision){
							MercurialRevision rev = (MercurialRevision) element;
							if(rev.getResource() instanceof IFile){
								return true;
							}
						}
					}
					return false;
				}
			};
			revertAction.setImageDescriptor(MercurialEclipsePlugin
					.getImageDescriptor("actions/revert.gif"));
		}
		return revertAction;
	}
}
