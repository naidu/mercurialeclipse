/*******************************************************************************
 * Copyright (c) 2003, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 *     Bastian Doetsch              - adaptation
 *     Andrei Loskutov (Intland) - bug fixes
 ******************************************************************************/
package com.vectrace.MercurialEclipse.repository;

import java.util.Iterator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.SameShellProvider;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.ViewPart;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.repository.actions.RemoveRootAction;
import com.vectrace.MercurialEclipse.repository.model.AllRootsElement;
import com.vectrace.MercurialEclipse.repository.model.RemoteContentProvider;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.wizards.CloneRepoWizard;
import com.vectrace.MercurialEclipse.wizards.NewLocationWizard;

/**
 * RepositoriesView is a view on a set of known Hg repositories
 */
public class RepositoriesView extends ViewPart implements ISelectionListener {
	public static final String VIEW_ID = "com.vectrace.MercurialEclipse.repository.RepositoriesView"; //$NON-NLS-1$

	// The root
	private AllRootsElement root;

	// Actions
	private Action newAction;
	private RemoveRootAction removeRootAction;

	// The tree viewer
	protected TreeViewer treeViewer;

	// Drill down adapter
	private DrillDownAdapter drillPart; // Home, back, and "drill into"

	private Action refreshAction;
	private Action refreshPopupAction;
	private Action collapseAllAction;
	private Action propertiesAction;
	private Action cloneAction;

	private RemoteContentProvider contentProvider;

	// this listener is used when a repository is added, removed or changed
	private final IRepositoryListener repositoryListener = new IRepositoryListener() {
		public void repositoryAdded(final HgRepositoryLocation loc) {
			getViewer().getControl().getDisplay().asyncExec(new Runnable() {
				public void run() {
					refreshViewer(null, false);
					getViewer().setSelection(new StructuredSelection(loc));
				}
			});
		}

		public void repositoryRemoved(HgRepositoryLocation loc) {
			refresh(null, false);
		}

		public void repositoriesChanged(HgRepositoryLocation[] roots) {
			refresh(null, false);
		}

		private void refresh(final Object object, boolean refreshRepositoriesFolders) {
			final boolean finalRefreshReposFolders = refreshRepositoriesFolders;
			Display display = getViewer().getControl().getDisplay();
			display.asyncExec(new Runnable() {
				public void run() {
					refreshViewer(object, finalRefreshReposFolders);
				}
			});
		}

		public void repositoryModified(HgRepositoryLocation loc) {
			refresh(null, false);
		}
	};

	public RepositoriesView() {
		super();
	}

	/**
	 * Contribute actions to the view
	 */
	protected void contributeActions() {

		final Shell shell = getShell();

		// Create actions

		// New Repository (popup)
		newAction = new Action(Messages.getString("RepositoriesView.createRepo"), MercurialEclipsePlugin //$NON-NLS-1$
				.getImageDescriptor("wizards/newlocation_wiz.gif")) { //$NON-NLS-1$
			@Override
			public void run() {
				NewLocationWizard wizard = new NewLocationWizard();
				WizardDialog dialog = new WizardDialog(shell, wizard);
				dialog.open();
			}
		};

		// Clone Repository (popup)
		cloneAction = new Action(Messages.getString("RepositoriesView.cloneRepo"), MercurialEclipsePlugin //$NON-NLS-1$
				.getImageDescriptor("clone_repo.gif")) { //$NON-NLS-1$
			@Override
			public void run() {
				IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
				CloneRepoWizard wizard = new CloneRepoWizard();
				wizard.init(PlatformUI.getWorkbench(), selection);
				WizardDialog dialog = new WizardDialog(shell, wizard);
				dialog.open();
			}
		};

		// Properties
		propertiesAction = new PropertyDialogAction(
				new SameShellProvider(shell), getViewer()){
			@Override
			public void run() {
				super.run();
				IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
				if(!selection.isEmpty()) {
					treeViewer.refresh(selection.getFirstElement());
				}
			}
		};
		getViewSite().getActionBars().setGlobalActionHandler(
				ActionFactory.PROPERTIES.getId(), propertiesAction);
		IStructuredSelection selection = (IStructuredSelection) getViewer()
				.getSelection();
		if (selection.size() == 1
				&& selection.getFirstElement() instanceof HgRepositoryLocation) {
			propertiesAction.setEnabled(true);
		} else {
			propertiesAction.setEnabled(false);
		}
		getViewer().addSelectionChangedListener(
				new ISelectionChangedListener() {
					public void selectionChanged(SelectionChangedEvent event) {
						IStructuredSelection ss = (IStructuredSelection) event
								.getSelection();
						boolean enabled = ss.size() == 1
								&& ss.getFirstElement() instanceof HgRepositoryLocation;
						propertiesAction.setEnabled(enabled);
					}
				});

		// Remove Root
		removeRootAction = new RemoveRootAction(treeViewer.getControl()
				.getShell());
		removeRootAction.selectionChanged((IStructuredSelection) null);

		IActionBars bars = getViewSite().getActionBars();
		bars.setGlobalActionHandler(ActionFactory.DELETE.getId(),
				removeRootAction);

		// Refresh action (toolbar)
		refreshAction = new Action(Messages.getString("RepositoriesView.refreshRepos"), //$NON-NLS-1$
				MercurialEclipsePlugin.getImageDescriptor("elcl16/refresh.gif")) { //$NON-NLS-1$
			@Override
			public void run() {
				refreshViewer(null, true);
			}
		};
		refreshAction.setToolTipText(Messages.getString("RepositoriesView.refresh"));  //$NON-NLS-1$
		refreshAction.setDisabledImageDescriptor(MercurialEclipsePlugin
				.getImageDescriptor("dlcl16/refresh.gif")); //$NON-NLS-1$
		refreshAction.setHoverImageDescriptor(MercurialEclipsePlugin
				.getImageDescriptor("clcl16/refresh.gif")); //$NON-NLS-1$
		getViewSite().getActionBars().setGlobalActionHandler(
				ActionFactory.REFRESH.getId(), refreshAction);

		refreshPopupAction = new Action(Messages.getString("RepositoriesView.refresh"), MercurialEclipsePlugin //$NON-NLS-1$
				.getImageDescriptor("clcl16/refresh.gif")) { //$NON-NLS-1$
			@Override
			public void run() {
				refreshViewerNode();
			}
		};

		// Collapse action
		collapseAllAction = new Action("RepositoriesView.collapseAll", //$NON-NLS-1$
				MercurialEclipsePlugin
						.getImageDescriptor("elcl16/collapseall.gif")) { //$NON-NLS-1$
			@Override
			public void run() {
				collapseAll();
			}
		};
		collapseAllAction.setToolTipText(Messages.getString("RepositoriesView.collapseAll")); //$NON-NLS-1$
		collapseAllAction.setHoverImageDescriptor(MercurialEclipsePlugin
				.getImageDescriptor("clcl16/collapseall.gif")); //$NON-NLS-1$

		// Create the popup menu
		MenuManager menuMgr = new MenuManager();
		Tree tree = treeViewer.getTree();
		Menu menu = menuMgr.createContextMenu(tree);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				addWorkbenchActions(manager);
			}

		});
		menuMgr.setRemoveAllWhenShown(true);
		tree.setMenu(menu);
		getSite().registerContextMenu(menuMgr, treeViewer);

		// Create the local tool bar
		IToolBarManager tbm = bars.getToolBarManager();
		drillPart.addNavigationActions(tbm);
		tbm.add(refreshAction);
		tbm.add(new Separator());
		tbm.add(collapseAllAction);
		tbm.update(false);

		bars.updateActionBars();
	} // contributeActions

	protected void addWorkbenchActions(IMenuManager manager) {
		// New actions go next

		MenuManager sub = new MenuManager(Messages.getString("RepositoriesView.new"), //$NON-NLS-1$
				IWorkbenchActionConstants.GROUP_ADD);
		sub.add(newAction);
		sub.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		manager.add(sub);

		// File actions go first (view file)
		manager.add(new Separator(IWorkbenchActionConstants.GROUP_FILE));
		// Misc additions
		manager.add(new Separator("historyGroup")); //$NON-NLS-1$
		manager.add(new Separator("checkoutGroup")); //$NON-NLS-1$
		manager.add(new Separator("exportImportGroup")); //$NON-NLS-1$
		manager.add(new Separator("miscGroup")); //$NON-NLS-1$


		IStructuredSelection selection = (IStructuredSelection) getViewer()
				.getSelection();
		boolean singleRepoSelected = selection.size() == 1
			&& selection.getFirstElement() instanceof HgRepositoryLocation;

		if(singleRepoSelected){
			manager.add(cloneAction);
			manager.add(refreshPopupAction);
		}

		removeRootAction.selectionChanged(selection);
		if (removeRootAction.isEnabled()) {
			manager.add(removeRootAction);
		}

		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

		if (singleRepoSelected) {
			manager.add(new Separator());
			manager.add(propertiesAction);
		}
	}

	@Override
	public void createPartControl(Composite parent) {
		treeViewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL
				| SWT.V_SCROLL);
		contentProvider = new RemoteContentProvider();
		treeViewer.setContentProvider(contentProvider);
		treeViewer.setLabelProvider(new WorkbenchLabelProvider());
		getSite().setSelectionProvider(treeViewer);
		root = new AllRootsElement();
		treeViewer.setInput(root);
		treeViewer.setSorter(new RepositorySorter());
		drillPart = new DrillDownAdapter(treeViewer);

		contributeActions();

		initializeListeners();
		MercurialEclipsePlugin.getRepoManager().addRepositoryListener(
				repositoryListener);
	}

	/**
	 * initialize the listeners
	 */
	protected void initializeListeners() {
		getSite().getWorkbenchWindow().getSelectionService()
				.addPostSelectionListener(this);
		treeViewer.addSelectionChangedListener(removeRootAction);

		// when F5 is pressed, refresh this view
		treeViewer.getControl().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent event) {
				if (event.keyCode == SWT.F5) {
					refreshAction.run();
				}
			}
		});

		treeViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent e) {
				handleDoubleClick(e);
			}
		});

	}

	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		String msg = getStatusLineMessage(selection);
		getViewSite().getActionBars().getStatusLineManager().setMessage(msg);
	}

	/**
	 * When selection is changed we update the status line
	 */
	private String getStatusLineMessage(ISelection selection) {
		if (!(selection instanceof IStructuredSelection) || selection.isEmpty()) {
			return ""; //$NON-NLS-1$
		}
		IStructuredSelection s = (IStructuredSelection) selection;

		if (s.size() > 1) {
			return String.valueOf(s.size()) + Messages.getString("RepositoriesView.multiSelected"); //$NON-NLS-1$
		}
		return Messages.getString("RepositoriesView.oneSelected"); //$NON-NLS-1$
	}

	@Override
	public void setFocus() {
		treeViewer.getControl().setFocus();
	}

	protected Shell getShell() {
		return treeViewer.getTree().getShell();
	}

	protected TreeViewer getViewer() {
		return treeViewer;
	}

	/**
	 * this is called whenever a new repository location is added for example or
	 * when user wants to refresh
	 */
	protected void refreshViewer(Object object,
			boolean refreshRepositoriesFolders) {
		if (treeViewer == null || treeViewer.getControl() == null || treeViewer.getControl().isDisposed()) {
			return;
		}
		if (refreshRepositoriesFolders) {
			try {
				MercurialEclipsePlugin.getRepoManager().refreshRepositories(null);
			} catch (HgException e) {
				MercurialEclipsePlugin.logError(e);
			}
		}
		if (object == null) {
			treeViewer.refresh();
		} else {
			treeViewer.refresh(object);
		}
	}

	@SuppressWarnings("unchecked")
	protected void refreshViewerNode() {
		IStructuredSelection selection = (IStructuredSelection) treeViewer
				.getSelection();
		Iterator iter = selection.iterator();
		while (iter.hasNext()) {
			Object object = iter.next();
			if (object instanceof HgRepositoryLocation) {
				refreshAction.run();
				break;
			}
			treeViewer.refresh(object);
		}
	}

	public void collapseAll() {
		if (treeViewer == null) {
			return;
		}
		treeViewer.getControl().setRedraw(false);
		treeViewer.collapseToLevel(treeViewer.getInput(),
				AbstractTreeViewer.ALL_LEVELS);
		treeViewer.getControl().setRedraw(true);
	}

	/**
	 * The mouse has been double-clicked in the tree, perform appropriate
	 * behaviour.
	 */
	private void handleDoubleClick(DoubleClickEvent e) {
		// Only act on single selection
		ISelection selection = e.getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structured = (IStructuredSelection) selection;
			if (structured.size() == 1) {
				propertiesAction.run();
			}
		}
	}

	@Override
	public void dispose() {
		MercurialEclipsePlugin.getRepoManager().removeRepositoryListener(
				repositoryListener);
		super.dispose();
		treeViewer = null;
	}

}
