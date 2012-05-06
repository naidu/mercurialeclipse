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
package com.vectrace.MercurialEclipse.views;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgResolveClient;
import com.vectrace.MercurialEclipse.commands.extensions.HgRebaseClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.menu.AbortRebaseHandler;
import com.vectrace.MercurialEclipse.menu.CommitMergeHandler;
import com.vectrace.MercurialEclipse.menu.ContinueRebaseHandler;
import com.vectrace.MercurialEclipse.menu.MergeHandler;
import com.vectrace.MercurialEclipse.menu.RunnableHandler;
import com.vectrace.MercurialEclipse.menu.UpdateHandler;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.ResolveStatus;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.CompareAction;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.ui.AbstractHighlightableTable;
import com.vectrace.MercurialEclipse.ui.AbstractHighlightableTable.HighlightingLabelProvider;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * TODO: Make use of JavaHg MergeContext
 */
public class MergeView extends AbstractRootView implements Observer {

	public static final String ID = MergeView.class.getName();

	private MergeTable table;

	private Action abortAction;

	private Action completeAction;

	private Action markResolvedAction;

	private Action markUnresolvedAction;

	protected boolean merging = true;

	@Override
	public void createPartControl(final Composite parent) {
		super.createPartControl(parent);
		MercurialStatusCache.getInstance().addObserver(this);
	}

	/**
	 * @see com.vectrace.MercurialEclipse.views.AbstractRootView#createTable(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createTable(Composite parent) {
		table = new MergeTable(parent);

		table.getTableViewer().addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				openMergeEditor((ResolveStatus) ((IStructuredSelection) event.getSelection()).getFirstElement());
			}
		});
	}

	/**
	 * @see com.vectrace.MercurialEclipse.views.AbstractRootView#createActions()
	 */
	@Override
	protected void createActions() {
		completeAction = new Action(Messages.getString("MergeView.merge.complete")) { //$NON-NLS-1$
			@Override
			public void run() {
				if (areAllResolved()) {
					attemptToCommit();
					refresh(hgRoot);
				}
			}
		};
		completeAction.setEnabled(false);
		completeAction.setImageDescriptor(MercurialEclipsePlugin.getImageDescriptor("actions/commit.gif"));

		abortAction = new Action(Messages.getString("MergeView.merge.abort")) { //$NON-NLS-1$
			@Override
			public void run() {
				try {
					RunnableHandler runnable;
					if (!merging) {
						runnable = new AbortRebaseHandler();
					} else {
						UpdateHandler update = new UpdateHandler();
						update.setCleanEnabled(true);
						update.setRevision(".");
						runnable = update;
					}

					runnable.setShell(table.getShell());
					runnable.run(hgRoot);
					refresh(hgRoot);
				} catch (CoreException e) {
					handleError(e);
				}
				MercurialUtilities.setOfferAutoCommitMerge(true);
			}
		};
		abortAction.setEnabled(false);
		abortAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
				ISharedImages.IMG_ELCL_STOP));

		markResolvedAction = new Action(Messages.getString("MergeView.markResolved")) { //$NON-NLS-1$
			@Override
			public void run() {
				try {
					List<IFile> files = getSelections();
					if (files != null) {
						for (IFile file : files) {
							HgResolveClient.markResolved(hgRoot, file);
						}
						populateView(true);
					}
				} catch (HgException e) {
					handleError(e);
				}
			}
		};
		markResolvedAction.setEnabled(false);
		markUnresolvedAction = new Action(Messages.getString("MergeView.markUnresolved")) { //$NON-NLS-1$
			@Override
			public void run() {
				try {
					List<IFile> files = getSelections();
					if (files != null) {
						for (IFile file : files) {
							HgResolveClient.markUnresolved(hgRoot, file);
						}
						populateView(true);
					}
				} catch (HgException e) {
					handleError(e);
				}
			}
		};
		markUnresolvedAction.setEnabled(false);
	}

	/**
	 * @see com.vectrace.MercurialEclipse.views.AbstractRootView#createToolBar(org.eclipse.jface.action.IToolBarManager)
	 */
	@Override
	protected void createToolBar(IToolBarManager mgr) {
		mgr.add(makeActionContribution(completeAction));
		mgr.add(makeActionContribution(abortAction));
	}

	/**
	 * @see com.vectrace.MercurialEclipse.views.AbstractRootView#createMenus(org.eclipse.jface.action.IMenuManager)
	 */
	@Override
	protected void createMenus(IMenuManager mgr) {
		final Action openMergeEditorAction = new Action("Open in Merge Editor") {
			@Override
			public void run() {
				ResolveStatus selection = table.getSelection();
				if (selection != null) {
					openMergeEditor(selection);
				}
			}
		};

		final Action openEditorAction = new Action("Open in Default Editor") {
			@Override
			public void run() {
				IFile file = getSelection();
				if(file == null){
					return;
				}
				ResourceUtils.openEditor(getSite().getPage(), file);
			}
		};

		final Action actionShowHistory = new Action("Show History") {
			@Override
			public void run() {
				IFile file = getSelection();
				if(file == null){
					return;
				}
				TeamUI.getHistoryView().showHistoryFor(file);
			}
		};
		actionShowHistory.setImageDescriptor(MercurialEclipsePlugin.getImageDescriptor("history.gif"));

		// Contribute actions to popup menu
		final MenuManager menuMgr = new MenuManager();
		Menu menu = menuMgr.createContextMenu(table);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager menuMgr1) {
				menuMgr1.add(openMergeEditorAction);
				menuMgr1.add(openEditorAction);
				menuMgr1.add(new Separator());
				menuMgr1.add(actionShowHistory);
				menuMgr1.add(new Separator());
				menuMgr1.add(markResolvedAction);
				menuMgr1.add(markUnresolvedAction);
				menuMgr1.add(new Separator());
				menuMgr1.add(completeAction);
				menuMgr1.add(abortAction);
			}
		});

		menuMgr.setRemoveAllWhenShown(true);
		table.getTableViewer().getControl().setMenu(menu);
	}

	private void populateView(boolean attemptToCommit) throws HgException {
		boolean bAllResolved = true;
		List<ResolveStatus> status = null;
		status = HgResolveClient.list(hgRoot);
		table.setItems(status);
		for (ResolveStatus flagged : status) {
			if (flagged.isUnresolved()) {
				bAllResolved = false;
			}
		}
		completeAction.setEnabled(bAllResolved);

		/* TODO: remove this block? Commit button enablement provides sufficient feedback
		if (bAllResolved) {
			String label;
			if (merging) {
				label = Messages.getString("MergeView.PleaseCommitMerge");
			} else {
				label = Messages.getString("MergeView.PleaseCommitRebase");
			}
			showInfo(label);
		} else {
			hideStatus();
		}*/

		// Show commit dialog
		if (attemptToCommit && MercurialUtilities.isOfferAutoCommitMerge()
				&& areAllResolved()) {
			/*
			 * Offer commit of merge or rebase exactly once if no conflicts are found. Uses {@link
			 * ResourceProperties#MERGE_COMMIT_OFFERED} to avoid showing the user the commit dialog
			 * repeatedly. This flag should be cleared when any of the following operations occur:
			 * commit, rebase, revert.
			 */
			attemptToCommit();
		}
	}

	/**
	 * @see com.vectrace.MercurialEclipse.views.AbstractRootView#onRootChanged()
	 */
	@Override
	protected void onRootChanged() {
		if (hgRoot == null || !MercurialStatusCache.getInstance().isMergeInProgress(hgRoot)) {
			table.setItems(null);
			abortAction.setEnabled(false);
			completeAction.setEnabled(false);
			markResolvedAction.setEnabled(false);
			markUnresolvedAction.setEnabled(false);
			table.setEnabled(false);

			return;
		}

		abortAction.setEnabled(true);
		completeAction.setEnabled(true);
		markResolvedAction.setEnabled(true);
		markUnresolvedAction.setEnabled(true);
		table.setEnabled(true);

		try {
			merging = !HgRebaseClient.isRebasing(hgRoot);

			if (merging) {
				abortAction.setText(Messages.getString("MergeView.merge.abort"));
				completeAction.setText(Messages.getString("MergeView.merge.complete"));
			} else {
				abortAction.setText(Messages.getString("MergeView.rebase.abort"));
				completeAction.setText(Messages.getString("MergeView.rebase.complete"));
			}

			getViewSite().getActionBars().getToolBarManager().update(true);
			populateView(false);
		} catch (HgException e) {
			handleError(e);
		}
	}

	private void attemptToCommit() {
		try {
			MercurialUtilities.setOfferAutoCommitMerge(false);
			RunnableHandler handler = merging ? new CommitMergeHandler()
					: new ContinueRebaseHandler();

			handler.setShell(getSite().getShell());
			handler.run(hgRoot);
		} catch (CoreException e) {
			MercurialEclipsePlugin.logError(e);
		}
	}

	/**
	 * @see com.vectrace.MercurialEclipse.views.AbstractRootView#getDescription()
	 */
	@Override
	protected String getDescription() {
		if (hgRoot == null || !MercurialStatusCache.getInstance().isMergeInProgress(hgRoot)) {
			return "No merge in progress. Select a merging or rebasing repository";
		}

		if (merging) {
			String mergeNodeId = MercurialStatusCache.getInstance().getMergeChangesetId(hgRoot);
			if (mergeNodeId != null) {
				return hgRoot.getName() + ": Merging with " + mergeNodeId;
			}
			return hgRoot.getName() + ": Merging";
		}
		return hgRoot.getName() + ": Rebasing";
	}

	/**
	 * @see com.vectrace.MercurialEclipse.views.AbstractRootView#canChangeRoot(com.vectrace.MercurialEclipse.model.HgRoot, boolean)
	 */
	@Override
	protected boolean canChangeRoot(HgRoot newRoot, boolean fromSelection) {
		boolean ok = super.canChangeRoot(newRoot, fromSelection);

		if (fromSelection) {
			ok &= MercurialStatusCache.getInstance().isMergeInProgress(newRoot);
		}

		return ok;
	}

	private boolean areAllResolved() {
		boolean allResolved = true;
		for (ResolveStatus fa : table.getItems()) {
			allResolved &= fa.isResolved();
		}
		return allResolved;
	}

	/**
	 * @see com.vectrace.MercurialEclipse.views.AbstractRootView#selectionChanged(org.eclipse.ui.IWorkbenchPart, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			IStructuredSelection structured = (IStructuredSelection) selection;
			IResource resource = MercurialEclipsePlugin.getAdapter(structured.getFirstElement(), IResource.class);
			if (resource != null) {
				rootSelected(MercurialTeamProvider.hasHgRoot(resource));
			}
		}
	}

	@Override
	public void setFocus() {
		table.setFocus();
	}

	@Override
	public void dispose() {
		super.dispose();
		MercurialStatusCache.getInstance().deleteObserver(this);
	}

	private IFile getSelection() {
		return getFile(table.getSelection());
	}

	private List<IFile> getSelections() {
		List<ResolveStatus> selections = table.getSelections();
		if (selections != null) {
			List<IFile> result = new ArrayList<IFile>();
			for (ResolveStatus flaggedAdaptable : selections) {
				IFile file = getFile(flaggedAdaptable);

				if (file != null) {
					result.add(file);
				}
			}
			return result;
		}
		return null;
	}

	private static IFile getFile(ResolveStatus adaptable) {
		if (adaptable != null) {
			return (IFile) adaptable.getAdapter(IFile.class);
		}
		return null;
	}

	public void update(Observable o, Object arg) {
		if(hgRoot == null || !(arg instanceof Set<?>)){
			return;
		}
		Set<?> set = (Set<?>) arg;
		Set<IProject> projects = ResourceUtils.getProjects(hgRoot);
		// create intersection of the root projects with the updated set
		projects.retainAll(set);
		// if the intersection contains common projects, we need update the view
		if(!projects.isEmpty()) {
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					refresh(hgRoot);
				}
			});
		}
	}

	private static void openMergeEditor(ResolveStatus flagged) {
		IFile file = (IFile) flagged.getAdapter(IFile.class);
		CompareAction compareAction = new CompareAction(file);
		compareAction.setEnableMerge(true);
		compareAction.run(null);
	}

	/**
	 * Must be called from the UI thread
	 */
	public static void showMergeConflict(HgRoot hgRoot, Shell shell) throws PartInitException {
		MergeView view = (MergeView) MercurialEclipsePlugin.getActivePage().showView(MergeView.ID);
		view.refresh(hgRoot);
		MercurialEclipsePlugin.showDontShowAgainConfirmDialog("A merge conflict occurred",
				"A merge conflict occurred. Use the merge view to resolve and commit the merge",
				MessageDialog.INFORMATION,
				MercurialPreferenceConstants.PREF_SHOW_MERGE_CONFICT_NOTIFICATION_DIALOG, shell);

	}

	/**
	 * Must be called from the UI thread
	 */
	public static void showRebaseConflict(HgRoot hgRoot, Shell shell) throws PartInitException {
		MergeView view = (MergeView) MercurialEclipsePlugin.getActivePage().showView(MergeView.ID);
		view.refresh(hgRoot);
		MercurialEclipsePlugin
				.showDontShowAgainConfirmDialog(
						"A rebase conflict occurred",
						"A rebase conflict occurred. Use the merge view to resolve and complete the rebase",
						MessageDialog.INFORMATION,
						MercurialPreferenceConstants.PREF_SHOW_REBASE_CONFICT_NOTIFICATION_DIALOG,
						shell);
	}

	/**
	 * Make a job change listener so that when the job is done the merge view will be opened an a
	 * message shown saying a merge or rebase conflict occurred.
	 *
	 * @param hgRoot The root
	 * @param shell The shell, may be null
	 * @param merge True if this is a merge, false if it's a rebase.
	 * @return A new job change listener
	 */
	public static IJobChangeListener makeConflictJobChangeListener(final HgRoot hgRoot,
			final Shell shell, final boolean merge) {
		return makeUIJobChangeAdapter(new Runnable() {
			public void run() {
				try {
					Shell sh = (shell == null) ? MercurialEclipsePlugin.getActiveShell() : shell;

					if (merge) {
						showMergeConflict(hgRoot, sh);
					} else {
						showRebaseConflict(hgRoot, sh);
					}
				} catch (PartInitException e1) {
					MercurialEclipsePlugin.logError(e1);
				}
			}
		});
	}

	/**
	 * Make a job change listener so that when the job is done the current merge will auto-committed
	 *
	 * @param hgRoot
	 *            The root to use
	 * @param shell
	 *            The shell to use. May be null.
	 * @return Newly created job change listener
	 */
	public static IJobChangeListener makeCommitMergeJobChangeListener(final HgRoot hgRoot,
			final Shell shell, final String mergeNode) {
		return makeUIJobChangeAdapter(new Runnable() {

			public void run() {
				MergeHandler.commitMerge(new NullProgressMonitor(), hgRoot, mergeNode, shell, true);
			}
		});
	}

	private static IJobChangeListener makeUIJobChangeAdapter(final Runnable run) {
		return new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				Display.getDefault().asyncExec(run);
			}
		};
	}

	private static class MergeTable extends AbstractHighlightableTable<ResolveStatus> {

		public MergeTable(Composite parent) {
			super(parent, new MergeTableLabelProvider());
		}

		/**
		 * @see com.vectrace.MercurialEclipse.ui.AbstractHighlightableTable#createColumns(org.eclipse.jface.viewers.TableViewer, org.eclipse.jface.layout.TableColumnLayout)
		 */
		@Override
		protected List<TableViewerColumn> createColumns(TableViewer viewer,
				TableColumnLayout tableColumnLayout) {
			List<TableViewerColumn> l = new ArrayList<TableViewerColumn>(2);
			String[] titles = {
					Messages.getString("MergeView.column.status"), Messages.getString("MergeView.column.file") }; //$NON-NLS-1$ //$NON-NLS-2$
			ColumnLayoutData[] widths = { new ColumnPixelData(100, false, true),
					new ColumnWeightData(100, 200, true) };

			for (int i = 0; i < titles.length; i++) {
				TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
				column.getColumn().setText(titles[i]);
				tableColumnLayout.setColumnData(column.getColumn(), widths[i]);
				l.add(column);
			}

			return l;
		}

	}

	private static class MergeTableLabelProvider extends HighlightingLabelProvider<ResolveStatus> {

		/**
		 * @see com.vectrace.MercurialEclipse.ui.AbstractHighlightableTable.HighlightingLabelProvider#isHighlighted(java.lang.Object)
		 */
		@Override
		public boolean isHighlighted(ResolveStatus flagged) {
			return flagged.isUnresolved();
		}

		/**
		 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
		 */
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		/**
		 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
		 */
		public String getColumnText(Object element, int columnIndex) {
			ResolveStatus flagged = (ResolveStatus) element;

			switch (columnIndex) {
			case 0:
				return flagged.getStatus();
			case 1:
				// TODO: this is wrong when hgroot not at project root
				return ((IFile) flagged.getAdapter(IFile.class)).getProjectRelativePath()
						.toString();
			}

			throw new IllegalStateException();
		}

	}
}
