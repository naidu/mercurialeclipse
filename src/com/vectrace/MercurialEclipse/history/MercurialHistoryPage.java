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

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.team.core.history.IFileHistory;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.ui.history.HistoryPage;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.actions.OpenMercurialRevisionAction;
import com.vectrace.MercurialEclipse.commands.HgLogClient;
import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.commands.HgUpdateClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialRevisionStorage;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.utils.CompareUtils;
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
	private Action openEditorAction;
	private boolean showTags;

	private final class CompareRevisionAction extends Action {

		private CompareRevisionAction(String text) {
			super(text);
			setImageDescriptor(MercurialEclipsePlugin.getImageDescriptor("compare_view.gif"));
		}

		@Override
		public void run() {
			IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
			final Object[] revs = selection.toArray();

			final MercurialRevisionStorage [] right = new MercurialRevisionStorage [1];
			final MercurialRevisionStorage [] left = new MercurialRevisionStorage [1];
			final IRunnableWithProgress runnable = new IRunnableWithProgress() {

				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					try {
						if(revs.length > 0 && !monitor.isCanceled()){
							left[0] = getStorage((MercurialRevision) revs[0], monitor);
							if(revs.length > 1 && !monitor.isCanceled()){
								right[0] = getStorage((MercurialRevision) revs[1], monitor);
							}
						}
					} catch (CoreException e) {
						MercurialEclipsePlugin.logError(e);
						throw new InvocationTargetException(e);
					}
					if(monitor.isCanceled()){
						throw new InterruptedException("Cancelled by user");
					}
				}
			};
			ProgressMonitorDialog progress = new ProgressMonitorDialog(viewer.getControl().getShell());
			try {
				progress.run(true, true, runnable);
			} catch (InvocationTargetException e) {
				MercurialEclipsePlugin.logError(e.getCause());
				return;
			} catch (InterruptedException e) {
				// user cancel
				return;
			}

			if(left[0] == null){
				return;
			}
			boolean localEditable = right[0] == null;
			CompareUtils.openEditor(left[0], right[0], false, localEditable);
		}

		@Override
		public boolean isEnabled() {
			int size = ((IStructuredSelection) viewer.getSelection()).size();
			return size == 1;
		}

		/**
		 * this can take a lot of time, and UI must take care that it will not be frozen until
		 * the info is fetched...
		 * @param monitor
		 */
		private MercurialRevisionStorage getStorage(MercurialRevision rev, IProgressMonitor monitor) throws CoreException {
			if(rev.getParent() == null){
				// see issue #10302: this is a dirty trick to make sure to get content even
				// if the file was renamed/copied.
				HgLogClient.getLogWithBranchInfo(rev, mercurialHistory, monitor);
			}
			return (MercurialRevisionStorage) rev.getStorage(monitor);
		}
	}

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
				getOpenAction();
				updateOpenActionEnablement();
				if(openAction.isEnabled()) {
					openAction.run();
				}
			}
		});
		contributeActions();
	}

	private void copyToClipboard() {
		IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
		Iterator<?> iterator = selection.iterator();
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

	private void contributeActions() {
		final Action compareAction = getCompareAction();

		final Action updateAction = new Action(Messages.getString("MercurialHistoryPage.updateAction.name")) { //$NON-NLS-1$
			private MercurialRevision rev;

			@Override
			public void run() {
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
					HgUpdateClient.update(hgRoot, rev.getChangeSet().getChangeset(), true);
					refresh();
				} catch (HgException e) {
					MercurialEclipsePlugin.logError(e);
				}
			}

			@Override
			public boolean isEnabled() {
				IStructuredSelection selection = (IStructuredSelection) viewer
						.getSelection();
				Object[] revs = selection.toArray();
				if (revs != null && revs.length == 1) {
					rev = (MercurialRevision) revs[0];
					return true;
				}
				return false;
			}
		};
		updateAction.setImageDescriptor(MercurialEclipsePlugin.getImageDescriptor("actions/update.gif"));

		openEditorAction = new Action("Open Version on the Disk") {
			@Override
			public void run() {
				try {
					IDE.openEditor(getSite().getPage(), (IFile) resource);
				} catch (PartInitException e) {
					MercurialEclipsePlugin.logError(e);
				}
			}
		};

		// Contribute actions to popup menu
		final MenuManager menuMgr = new MenuManager();
		Menu menu = menuMgr.createContextMenu(viewer.getTable());
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager menuMgr1) {
				getOpenAction();
				updateOpenActionEnablement();
				menuMgr1.add(openAction);
				menuMgr1.add(openEditorAction);
				menuMgr1.add(new Separator(IWorkbenchActionConstants.GROUP_FILE));
				// TODO This is a HACK but I can't get the menu to update on
				// selection :-(
				compareAction.setEnabled(compareAction.isEnabled());
				menuMgr1.add(compareAction);
				updateAction.setEnabled(updateAction.isEnabled());
				menuMgr1.add(updateAction);
			}
		});
		menuMgr.setRemoveAllWhenShown(true);
		viewer.getTable().setMenu(menu);
	}

	private OpenMercurialRevisionAction getOpenAction() {
		if(openAction != null){
			return openAction;
		}
		openAction = new OpenMercurialRevisionAction("Open Selected Version");
		viewer.getTable().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				openAction.selectionChanged((IStructuredSelection) viewer
						.getSelection());
			}
		});
		openAction.setPage(this);
		return openAction;
	}

	private Action getCompareAction() {
		return new CompareRevisionAction(Messages.getString("CompareAction.label"));
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
		return object == null || object instanceof IResource;
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

	private void updateOpenActionEnablement() {
		openAction.selectionChanged((IStructuredSelection) viewer.getSelection());
		if (resource == null || resource.getType() != IResource.FILE) {
			openAction.setEnabled(false);
			openEditorAction.setEnabled(false);
		} else {
			openAction.setEnabled(true);
			openEditorAction.setEnabled(true);
		}
	}
}
