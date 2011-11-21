/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *     Andrei Loskutov           - bug fixes
 *     Philip Graf               - bug fix, popup menu
 *******************************************************************************/
package com.vectrace.MercurialEclipse.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeUiJob;
import com.vectrace.MercurialEclipse.commands.extensions.mq.HgQFinishClient;
import com.vectrace.MercurialEclipse.commands.extensions.mq.HgQFoldClient;
import com.vectrace.MercurialEclipse.commands.extensions.mq.HgQPopClient;
import com.vectrace.MercurialEclipse.commands.extensions.mq.HgQPushClient;
import com.vectrace.MercurialEclipse.commands.extensions.mq.HgQSeriesClient;
import com.vectrace.MercurialEclipse.dialogs.RejectsDialog;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.menu.QDeleteHandler;
import com.vectrace.MercurialEclipse.menu.QImportHandler;
import com.vectrace.MercurialEclipse.menu.QNewHandler;
import com.vectrace.MercurialEclipse.menu.QRefreshHandler;
import com.vectrace.MercurialEclipse.menu.StripHandler;
import com.vectrace.MercurialEclipse.model.Patch;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.cache.RefreshRootJob;
import com.vectrace.MercurialEclipse.team.cache.RefreshWorkspaceStatusJob;
import com.vectrace.MercurialEclipse.ui.PatchTable;
import com.vectrace.MercurialEclipse.ui.TableSortListener;

/**
 * @author bastian
 *
 */
public class PatchQueueView extends AbstractRootView {

	public static final String ID = PatchQueueView.class.getName();

	private PatchTable table;

	private Action qNewAction;
	private Action qRefreshAction;
	private Action qPushAllAction;
	private Action qPopAllAction;
	private Action qDeleteAction;
	private Action qFinishAction;
	private Action qFoldAction;
	private Action qImportAction;
	private Action qGotoAction;
	private Action stripAction;
	private Patch topmostAppliedPatch;

	/**
	 * @see com.vectrace.MercurialEclipse.views.AbstractRootView#createTable(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createTable(Composite parent) {
		table = new PatchTable(parent);
		getSite().setSelectionProvider(table.getTableViewer());
	}

	/**
	 * @see com.vectrace.MercurialEclipse.views.AbstractRootView#createToolBar(org.eclipse.jface.action.IToolBarManager)
	 */
	@Override
	protected void createToolBar(IToolBarManager mgr) {
		mgr.add(makeActionContribution(qImportAction));
		mgr.add(makeActionContribution(qNewAction));
		mgr.add(makeActionContribution(qRefreshAction));
		mgr.add(makeActionContribution(qFinishAction));
		mgr.add(makeActionContribution(qFoldAction));
		mgr.add(makeActionContribution(qDeleteAction));
		mgr.add(makeActionContribution(stripAction));
	}

	/**
	 * @see com.vectrace.MercurialEclipse.views.AbstractRootView#createActions()
	 */
	@Override
	protected void createActions() {
		qImportAction = new Action("qimport...", MercurialEclipsePlugin.getImageDescriptor("import.gif")) { //$NON-NLS-1$
			@Override
			public void run() {
				try {
					hideStatus();
					QImportHandler.openWizard(hgRoot, getSite().getShell());
				} catch (Exception e) {
					MercurialEclipsePlugin.logError(e);
				}
			}
		};
		qImportAction.setEnabled(false);

		qNewAction = new Action("qnew...", MercurialEclipsePlugin.getImageDescriptor("import.gif")) { //$NON-NLS-1$
			@Override
			public void run() {
				try {
					hideStatus();
					QNewHandler.openWizard(hgRoot, getSite().getShell());
				} catch (Exception e) {
					MercurialEclipsePlugin.logError(e);
				}
			}
		};
		qNewAction.setEnabled(false);

		qRefreshAction = new Action("qrefresh...", MercurialEclipsePlugin.getImageDescriptor("actions/qrefresh.gif")) { //$NON-NLS-1$
			@Override
			public void run() {
				hideStatus();
				QRefreshHandler.openWizard(hgRoot, getSite().getShell());
			}
		};
		qRefreshAction.setEnabled(false);

		qGotoAction = new MQPushAction(Messages.getString("PatchQueueView.switchTo"), RefreshRootJob.LOCAL_AND_OUTGOING) { //$NON-NLS-1$
			@Override
			public boolean invokePush() throws HgException {
				// Switch to the first selected patch. There is only one patch selected because
				// the action is disabled if zero or more than one patches are selected.
				Patch patch = table.getSelection();
				if (patch != null) {
					if (patch.isApplied()) {
						HgQPopClient.pop(hgRoot, false, patch.getName());
					} else {
						HgQPushClient.push(hgRoot, false, patch.getName());
					}
					return true;
				}
				return false;
			}
		};
		qGotoAction.setImageDescriptor(MercurialEclipsePlugin.getImageDescriptor("actions/switch.gif"));
		qGotoAction.setEnabled(false);

		qPushAllAction = new MQPushAction(Messages.getString("PatchQueueView.applyAll"), RefreshRootJob.LOCAL_AND_OUTGOING) { //$NON-NLS-1$
			@Override
			public boolean invokePush() throws HgException {
				HgQPushClient.pushAll(hgRoot, false);
				return true;
			}
		};
		qPushAllAction.setEnabled(true);

		qPopAllAction = new MQAction(Messages.getString("PatchQueueView.unapplyAll"), RefreshRootJob.LOCAL_AND_OUTGOING) { //$NON-NLS-1$
			@Override
			public boolean invoke() throws HgException {
				HgQPopClient.popAll(hgRoot, false);
				return true;
			}
		};
		qPopAllAction.setEnabled(false);

		qFoldAction = new MQAction("qfold", RefreshRootJob.LOCAL_AND_OUTGOING) { //$NON-NLS-1$
			@Override
			public boolean invoke() throws HgException {
				List<Patch> patches = new ArrayList<Patch>(table.getSelections());
				if (patches.size() > 0) {
					Collections.sort(patches, new Comparator<Patch>() {
						public int compare(Patch o1, Patch o2) {
							return TableSortListener.sort(o1.getIndex(), o2.getIndex());
						}
					});
					HgQFoldClient.fold(hgRoot, true, null, patches);
					return true;
				}
				return false;
			}
		};
		qFoldAction.setEnabled(false);

		stripAction = new Action("strip...", MercurialEclipsePlugin.getImageDescriptor("actions/revert.gif")) { //$NON-NLS-1$
			@Override
			public void run() {
				try {
					hideStatus();
					// TODO: set initial selection to selected applied patch
					StripHandler.openWizard(hgRoot, getSite().getShell(), null);
				} catch (Exception e) {
					MercurialEclipsePlugin.logError(e);
				}
			}
		};
		stripAction.setEnabled(false);

		qDeleteAction = new Action("qdelete...", MercurialEclipsePlugin.getImageDescriptor("rem_co.gif")) { //$NON-NLS-1$
			@Override
			public void run() {
				hideStatus();
				QDeleteHandler.openWizard(hgRoot, getSite().getShell(), false);
			}
		};
		qDeleteAction.setEnabled(false);

		qFinishAction = new MQAction("qfinish", MercurialEclipsePlugin.getImageDescriptor("actions/qrefresh.gif"), RefreshRootJob.OUTGOING) { //$NON-NLS-1$
			@Override
			public boolean invoke() throws HgException {
				List<Patch> patches = table.getSelections();
				Patch max = null;
				boolean unappliedSelected = false;

				for (Patch p : patches) {
					if (p.isApplied()) {
						if (max == null || p.getIndex() > max.getIndex()) {
							max = p;
						}
					} else {
						unappliedSelected = true;
					}
				}

				List<Patch> toApply = new ArrayList<Patch>(table.getItems());

				for (Iterator<Patch> it = toApply.iterator(); it.hasNext();) {
					Patch cur = it.next();

					if (!cur.isApplied()) {
						it.remove();
					} else if (max != null && cur.getIndex() > max.getIndex()) {
						it.remove();
					}
				}

				if (toApply.size() == 0) {
					MessageDialog.openInformation(getSite().getShell(), "No applied patches",
							"Only applied patches can be promoted.");
				} else if (max == null) {
					if (unappliedSelected) {
						MessageDialog
								.openInformation(getSite().getShell(),
										"Only applied patches can be promoted",
										"Only applied patches can be promoted. Select an applied patch to promote.");
					} else if (MercurialEclipsePlugin.showDontShowAgainConfirmDialog(
							"Promote all applied patches?", "No patches selected, promote all "
									+ toApply.size() + " applied patches?",
							MessageDialog.CONFIRM,
							MercurialPreferenceConstants.PREF_SHOW_QFINISH_WARNING_DIALOG, getSite()
									.getShell())) {
						HgQFinishClient.finish(hgRoot, toApply);
						return true;
					}
				} else if (toApply.size() == 1
						|| MercurialEclipsePlugin.showDontShowAgainConfirmDialog(
								"QFinish multiple patches", "Promote " + toApply.size()
										+ " patches?", MessageDialog.CONFIRM,
								MercurialPreferenceConstants.PREF_SHOW_QFINISH_WARNING_DIALOG, getSite()
										.getShell())) {

					HgQFinishClient.finish(hgRoot, toApply);
					return true;
				}

				return false;
			}
		};
		qFinishAction.setEnabled(false);

		table.getTableViewer().addPostSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateEnablement((IStructuredSelection) event.getSelection());
			}
		});
	}

	@SuppressWarnings("unchecked")
	protected void updateEnablement(IStructuredSelection selection) {
		assert hgRoot != null;

		boolean isTopSelected = false;
		boolean selectionHasApplied = false;
		for (Iterator<Patch> i = selection.iterator(); i.hasNext();) {
			Patch p = i.next();
			if (p.equals(topmostAppliedPatch)) {
				isTopSelected = true;
			}

			selectionHasApplied |= p.isApplied();
		}

		boolean isAllApplied = true;
		boolean isAllUnapplied = true;
		for (Patch patch : (List<Patch>) table.getTableViewer().getInput()) {
			if (patch.isApplied()) {
				isAllUnapplied = false;
			} else {
				isAllApplied = false;
			}
		}

		qNewAction.setEnabled(true);
		qRefreshAction.setEnabled(true);
		qImportAction.setEnabled(true);
		qDeleteAction.setEnabled(true);
		qGotoAction.setEnabled(selection.size() == 1 && !isTopSelected);
		qPushAllAction.setEnabled(!isAllApplied);
		qPopAllAction.setEnabled(!isAllUnapplied);
		qFoldAction.setEnabled(!selectionHasApplied && !selection.isEmpty());
		qFinishAction.setEnabled(selectionHasApplied || selection.isEmpty());
		stripAction.setEnabled(true);
	}

	/**
	 * @see com.vectrace.MercurialEclipse.views.AbstractRootView#createMenus(org.eclipse.jface.action.IMenuManager)
	 */
	@Override
	protected void createMenus(IMenuManager menuMgr) {
		menuMgr.add(qImportAction);
		menuMgr.add(qNewAction);
		menuMgr.add(qRefreshAction);
		menuMgr.add(qFinishAction);
		menuMgr.add(qFoldAction);
		menuMgr.add(qDeleteAction);
		menuMgr.add(stripAction);

		MenuManager popupMenuMgr = new MenuManager();
		popupMenuMgr.add(qGotoAction);
		popupMenuMgr.add(qPushAllAction);
		popupMenuMgr.add(qPopAllAction);

		popupMenuMgr.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
		getSite().registerContextMenu(popupMenuMgr, table.getTableViewer());

		Control control = table.getTableViewer().getControl();
		control.setMenu(popupMenuMgr.createContextMenu(control));
	}

	@Override
	public void setFocus() {
		populateTable();
	}

	public void populateTable() {
		Job job = new SafeUiJob(Messages.getString("PatchQueueView.jobName.populateTable")) { //$NON-NLS-1$
			@Override
			protected IStatus runSafe(IProgressMonitor monitor)	{
				IStatus status = Status.OK_STATUS;
				if (hgRoot != null && !table.isDisposed()) {
					try {
						List<Patch> patches = HgQSeriesClient.getPatchesInSeries(hgRoot);

						// TODO: We have to hide the popup menu if the table is empty or the team
						// menu will be appended. This is a side effect of the team menu's
						// locationURI being "popup:org.eclipse.ui.popup.any". If the team menu gets
						// a proper locationURI, the next line can be deleted.
						table.setEnabled(!patches.isEmpty());

						topmostAppliedPatch = null;
						for (Patch patch : patches) {
							if (patch.isApplied()) {
								topmostAppliedPatch = patch;
							}
						}

						// Reverse so it's in the same order as the sync view
						patches = new ArrayList<Patch>(patches);
						Collections.reverse(patches);
						table.setItems(patches);
					} catch (HgException e) {
						if (e.getCause() instanceof InterruptedException) {
							// ?
						} else {
							showWarning(e.getConciseMessage());
							MercurialEclipsePlugin.logError(e);
							status = new Status(IStatus.ERROR, MercurialEclipsePlugin.ID,
									Messages.getString("PatchQueueView.cannotPopulatePatchViewTable"), e); //$NON-NLS-1$
						}
					} finally {
						updateEnablement(StructuredSelection.EMPTY);
					}
				}
				return status;
			}
		};

		job.setUser(false);
		job.setSystem(true);
		job.schedule();
	}

	/**
	 * @see org.eclipse.ui.ISelectionListener#selectionChanged(org.eclipse.ui.IWorkbenchPart, org.eclipse.jface.viewers.ISelection)
	 */
	public final void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structured = (IStructuredSelection) selection;
			if (structured.getFirstElement() instanceof IAdaptable) {
				IResource newResource = (IResource) ((IAdaptable) structured.getFirstElement())
						.getAdapter(IResource.class);

				if (newResource != null && newResource.isAccessible()
						&& MercurialTeamProvider.isHgTeamProviderFor(newResource)) {
					rootSelected(MercurialTeamProvider.getHgRoot(newResource));
				}
			}
		}

		// TODO: why?
		if (part instanceof IEditorPart) {
			IEditorInput input = ((IEditorPart) part).getEditorInput();
			IFile file = (IFile) input.getAdapter(IFile.class);
			if (file != null && file.isAccessible()
					&& MercurialTeamProvider.isHgTeamProviderFor(file)) {
				rootSelected(MercurialTeamProvider.getHgRoot(file));
			}
		}
	}

	/**
	 * @see com.vectrace.MercurialEclipse.views.AbstractRootView#onRootChanged()
	 */
	@Override
	protected void onRootChanged() {
		populateTable();
	}

	public static PatchQueueView getView() {
		PatchQueueView view = (PatchQueueView) MercurialEclipsePlugin.getActivePage().findView(ID);
		if (view == null) {
			try {
				view = (PatchQueueView) MercurialEclipsePlugin.getActivePage().showView(ID);
			} catch (PartInitException e) {
				MercurialEclipsePlugin.logError(e);
			}
		}
		return view;
	}

	/**
	 * @see com.vectrace.MercurialEclipse.views.AbstractRootView#getDescription()
	 */
	@Override
	protected String getDescription() {
		if (hgRoot == null) {
			return "No repository selected";
		}

		return Messages.getString("PatchQueueView.repository") + hgRoot;
	}

	private abstract class MQAction extends Action {
		protected final int refreshFlag;

		public MQAction(String name, int refreshFlag) {
			super(name);
			this.refreshFlag = refreshFlag;
		}

		public MQAction(String name, ImageDescriptor desc, int refreshFlag) {
			super(name, desc);
			this.refreshFlag = refreshFlag;
		}

		/**
		 * @see org.eclipse.jface.action.Action#run()
		 */
		@Override
		public final void run() {
			boolean changed = true;

			try {
				hideStatus();
				changed = invoke();
			} catch (HgException e) {
				MercurialEclipsePlugin.logError(e);
				showWarning(e.getConciseMessage());

				if (e.isMultiLine()) {
					MercurialEclipsePlugin.showError(e);
				}
			} catch (CoreException e) {
				MercurialEclipsePlugin.logError(e);
				showWarning(e.getLocalizedMessage());
				MercurialEclipsePlugin.showError(e);
			} finally {
				if (changed) {
					populateTable();
					new RefreshWorkspaceStatusJob(hgRoot, refreshFlag).schedule();
				}
			}
		}
		public abstract boolean invoke() throws HgException, CoreException;
	}

	private abstract class MQPushAction extends MQAction {

		public MQPushAction(String name, ImageDescriptor desc, int refreshFlag) {
			super(name, desc, refreshFlag);
		}

		public MQPushAction(String name, int refreshFlag) {
			super(name, refreshFlag);
		}

		@Override
		public final boolean invoke() throws HgException, CoreException {
			try {
				return invokePush();
			} catch (HgException e) {
				if (HgQPushClient.isPatchApplyConflict(e)) {
					try {
						populateTable();
						Job job = new RefreshWorkspaceStatusJob(hgRoot, refreshFlag);
						job.schedule();
						job.join();

						new RejectsDialog(getSite().getShell(), hgRoot, e.getMessage(),
								"QPushRejectsDialog.title",
								"QPushRejectsDialog.conflict").open();
						return false; // already refreshed and populated the table
					} catch (HgException e2) {
						MercurialEclipsePlugin.logError(e2);
					} catch (InterruptedException e1) {
						MercurialEclipsePlugin.logError(e1);
					}
				}

				throw e;
			}
		}

		public abstract boolean invokePush() throws HgException, CoreException;
	}
}
