/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Administrator	implementation
 *     Andrei Loskutov (Intland) - bug fixes
 *     Zsolt Koppany (Intland)
 *******************************************************************************/
package com.vectrace.MercurialEclipse.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.ResourceNode;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.TableColumnSorter;
import com.vectrace.MercurialEclipse.compare.RevisionNode;
import com.vectrace.MercurialEclipse.dialogs.CommitResource;
import com.vectrace.MercurialEclipse.dialogs.CommitResourceLabelProvider;
import com.vectrace.MercurialEclipse.dialogs.CommitResourceUtil;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.team.MercurialRevisionStorage;
import com.vectrace.MercurialEclipse.utils.CompareUtils;

/**
 * TODO enable tree/flat view switch
 *
 * @author steeven
 * $Id$
 */
public class CommitFilesChooser extends Composite {
	private final UntrackedFilesFilter untrackedFilesFilter;
	private final CommittableFilesFilter committableFilesFilter;
	private final boolean selectable;
	private Button showUntrackedFilesButton;
	private Button selectAllButton;
	private final CheckboxTableViewer viewer;
	private final boolean showUntracked;
	private final boolean missing;
	private final ListenerList stateListeners = new ListenerList();
	protected Control trayButton;
	protected boolean trayClosed = true;
	protected IFile selectedFile;

	public CheckboxTableViewer getViewer() {
		return viewer;
	}

	public CommitFilesChooser(Composite container, boolean selectable,
			List<IResource> resources, boolean showUntracked, boolean showMissing) {
		super(container, container.getStyle());
		this.selectable = selectable;
		this.showUntracked = showUntracked;
		this.missing = showMissing;
		this.untrackedFilesFilter = new UntrackedFilesFilter(missing);
		this.committableFilesFilter = new CommittableFilesFilter();

		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 3;
		layout.horizontalSpacing = 0;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		setLayout(layout);

		setLayoutData(SWTWidgetHelper.getFillGD(200));

		Table table = createTable();
		createOptionCheckbox();

		viewer = new CheckboxTableViewer(table);
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setLabelProvider(new CommitResourceLabelProvider());
		viewer.addFilter(committableFilesFilter);
		if (!showUntracked) {
			viewer.addFilter(untrackedFilesFilter);
		}

		setResources(resources);

		createShowDiffButton(container);
		createFileSelectionListener();

		makeActions();
	}

	private void createFileSelectionListener() {
		getViewer().addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				ISelection selection = event.getSelection();

				if (selection instanceof IStructuredSelection) {
					IStructuredSelection sel = (IStructuredSelection) selection;
					CommitResource commitResource = (CommitResource) sel.getFirstElement();
					if (commitResource != null) {
						IFile oldSelectedFile = selectedFile;
						selectedFile = (IFile) commitResource.getResource();
						if (oldSelectedFile == null || !oldSelectedFile.equals(selectedFile)) {
							trayButton.setEnabled(true);
						}
					}

				}
			}

		});
	}

	private void createShowDiffButton(Composite container) {
		trayButton = SWTWidgetHelper.createPushButton(container, Messages
				.getString("CommitFilesChooser.showDiffButton.text"), //$NON-NLS-1$
				1);
		trayButton.setEnabled(false);
		trayButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				showDiffForSelection();
			}
		});
	}

	private Table createTable() {
		int flags = SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER;
		if (selectable) {
			flags |= SWT.CHECK | SWT.FULL_SELECTION | SWT.MULTI;
		} else {
			flags |= SWT.READ_ONLY | SWT.HIDE_SELECTION;
		}

		Table table = new Table(this, flags);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		table.setLayoutData(data);

		TableColumn col;

		// File name
		col = new TableColumn(table, SWT.LEFT);
		col.setResizable(true);
		col.setText(Messages.getString("Common.ColumnStatus")); //$NON-NLS-1$
		col.setWidth(90);
		col.setMoveable(true);

		// File status
		col = new TableColumn(table, SWT.LEFT);
		col.setResizable(true);
		col.setText(Messages.getString("Common.ColumnFile")); //$NON-NLS-1$
		col.setWidth(400);
		col.setMoveable(true);
		return table;
	}

	private void createOptionCheckbox() {
		if (!selectable) {
			return;
		}
		selectAllButton = new Button(this, SWT.CHECK);
		selectAllButton.setText(Messages.getString("Common.SelectOrUnselectAll")); //$NON-NLS-1$
		selectAllButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		if (!showUntracked) {
			return;
		}
		showUntrackedFilesButton = new Button(this, SWT.CHECK);
		showUntrackedFilesButton.setText(Messages.getString("Common.ShowUntrackedFiles")); //$NON-NLS-1$
		showUntrackedFilesButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}

	protected CompareEditorInput getCompareEditorInput() {

		if (selectedFile == null) {
			return null;
		}
		MercurialRevisionStorage iStorage = new MercurialRevisionStorage(selectedFile);
		ResourceNode right = new RevisionNode(iStorage);
		ResourceNode left = new ResourceNode(selectedFile);
		return CompareUtils.getCompareInput(left, right, false);
	}

	private void makeActions() {
		getViewer().addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				showDiffForSelection();
			}
		});
		getViewer().addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				fireStateChanged();
			}
		});
		if (selectable) {
			selectAllButton.setSelection(false); // Start not selected
		}
		if (selectable) {
			selectAllButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (selectAllButton.getSelection()) {
						getViewer().setAllChecked(true);
					} else {
						getViewer().setAllChecked(false);
					}
					fireStateChanged();
				}
			});
		}

		if (selectable && showUntracked) {
			showUntrackedFilesButton.setSelection(true); // Start selected.
			showUntrackedFilesButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (showUntrackedFilesButton.getSelection()) {
						getViewer().removeFilter(untrackedFilesFilter);
					} else {
						getViewer().addFilter(untrackedFilesFilter);
					}
					getViewer().refresh(true);
					fireStateChanged();
				}
			});
		}

		final Table table = getViewer().getTable();
		TableColumn[] columns = table.getColumns();
		for (int ci = 0; ci < columns.length; ci++) {
			TableColumn column = columns[ci];
			final int colIdx = ci;
			new TableColumnSorter(getViewer(), column) {
				@Override
				protected int doCompare(Viewer v, Object e1, Object e2) {
					StructuredViewer v1 = (StructuredViewer) v;
					ITableLabelProvider lp = ((ITableLabelProvider) v1.getLabelProvider());
					String t1 = lp.getColumnText(e1, colIdx);
					String t2 = lp.getColumnText(e2, colIdx);
					return t1.compareTo(t2);
				}
			};
		}
	}

	/**
	 * Set the resources, and from those select resources, which are tracked by Mercurial
	 * @param resources
	 */
	public void setResources(List<IResource> resources) {
		List<CommitResource> commitResources = createCommitResources(resources);
		getViewer().setInput(commitResources.toArray());

		List<CommitResource> tracked = new CommitResourceUtil().filterForTracked(commitResources);
		getViewer().setCheckedElements(tracked.toArray());
		if (!showUntracked) {
			selectAllButton.setSelection(true);
		}
	}

	/**
	 * Create the Commit-resources' for a set of resources
	 * @param res
	 * @return
	 */
	private List<CommitResource> createCommitResources(List<IResource> res) {
		try {
			CommitResource[] result = new CommitResourceUtil().getCommitResources(res.toArray(new IResource[0]));
			return Arrays.asList(result);
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
		}
		return new ArrayList<CommitResource>(0);
	}

	/**
	 * Set the selected resources. Obviously this will only select those resources, which are displayed...
	 * @param resources
	 */
	public void setSelectedResources(List<IResource> resources) {
		CommitResource[] allCommitResources = (CommitResource[]) getViewer().getInput();
		if (allCommitResources == null) {
			return;
		}
		List<CommitResource> selected = new CommitResourceUtil().filterForResources(Arrays.asList(allCommitResources), resources);
		getViewer().setCheckedElements(selected.toArray());
	}

	public List<IResource> getCheckedResources(String... status) {
		return getViewerResources(true, status);
	}

	public List<IResource> getUncheckedResources(String... status) {
		return getViewerResources(false, status);
	}

	public List<IResource> getViewerResources(boolean checked, String... status) {
		TableItem[] children = getViewer().getTable().getItems();
		List<IResource> list = new ArrayList<IResource>(children.length);
		for (int i = 0; i < children.length; i++) {
			TableItem item = children[i];
			if (item.getChecked() == checked && item.getData() instanceof CommitResource) {
				CommitResource resource = (CommitResource) item.getData();
				if (status == null || status.length == 0) {
					list.add(resource.getResource());
				} else {
					for (String stat : status) {
						if (resource.getStatusMessage().equals(stat)) {
							list.add(resource.getResource());
							break;
						}
					}
				}
			}
		}
		return list;
	}

	public void addStateListener(Listener listener) {
		stateListeners.add(listener);
	}

	protected void fireStateChanged() {
		for (Object obj : stateListeners.getListeners()) {
			((Listener) obj).handleEvent(null);
		}
	}

	private void showDiffForSelection() {
		if (selectedFile != null) {
			MercurialRevisionStorage iStorage = new MercurialRevisionStorage(selectedFile);
			ResourceNode right = new RevisionNode(iStorage);
			ResourceNode left = new ResourceNode(selectedFile);
			CompareUtils.openEditor(left, right, true, false);
		}
	}
}
