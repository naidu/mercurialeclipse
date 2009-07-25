/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Administrator	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.ui;

import java.util.ArrayList;
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
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.TableColumnSorter;
import com.vectrace.MercurialEclipse.compare.RevisionNode;
import com.vectrace.MercurialEclipse.dialogs.CommitResource;
import com.vectrace.MercurialEclipse.dialogs.CommitResourceLabelProvider;
import com.vectrace.MercurialEclipse.dialogs.CommitResourceUtil;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.IStorageMercurialRevision;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.utils.CompareUtils;

/**
 * TODO enable tree/flat view switch
 * 
 * @author steeven
 * 
 */
public class CommitFilesChooser extends Composite {
    private final UntrackedFilesFilter untrackedFilesFilter;
    private final CommittableFilesFilter committableFilesFilter;
    private final HgRoot root;
    private final boolean selectable;
    private Button showUntrackedFilesButton;
    private Button selectAllButton;
    private final CheckboxTableViewer viewer;
    private final boolean untracked;
    private final boolean missing;
    private final ListenerList stateListeners = new ListenerList();
    protected Control trayButton;
    protected boolean trayClosed = true;
    protected IFile selectedFile;

    private Label rightSeparator;
    private Label leftSeparator;
    private Control trayControl;
    private Sash sash;
    private DiffTray tray;

    /**
     * @return the viewer
     */
    public CheckboxTableViewer getViewer() {
        return viewer;
    }

    public CommitFilesChooser(Composite container, boolean selectable,
            List<IResource> resources, HgRoot hgRoot, boolean showUntracked, boolean showMissing) {
        super(container, container.getStyle());
        this.selectable = selectable;
        this.root = hgRoot;
        this.untracked = showUntracked;
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

    /**
     * 
     */
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
                            if (!trayClosed) {
                                closeSash();
                                openSash();
                            }
                        }
                    }

                }
            }

        });
    }

    /**
     * @param container
     */
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

    /**
     * 
     */
    private void openSash() {
        DiffTray t = new DiffTray(getCompareEditorInput());
        final Shell shell = getShell();
        leftSeparator = new Label(shell, SWT.SEPARATOR | SWT.VERTICAL);
        leftSeparator.setLayoutData(new GridData(GridData.FILL_VERTICAL));
        sash = new Sash(shell, SWT.VERTICAL);
        sash.setLayoutData(new GridData(GridData.FILL_VERTICAL));
        rightSeparator = new Label(shell, SWT.SEPARATOR | SWT.VERTICAL);
        rightSeparator.setLayoutData(new GridData(GridData.FILL_VERTICAL));
        trayControl = t.createContents(shell);
        Rectangle clientArea = shell.getClientArea();
        final GridData data = new GridData(GridData.FILL_VERTICAL);
        data.widthHint = trayControl.computeSize(SWT.DEFAULT, clientArea.height).x;
        trayControl.setLayoutData(data);
        int trayWidth = leftSeparator.computeSize(SWT.DEFAULT, clientArea.height).x
        + sash.computeSize(SWT.DEFAULT, clientArea.height).x
        + rightSeparator.computeSize(SWT.DEFAULT, clientArea.height).x + data.widthHint;
        Rectangle bounds = shell.getBounds();
        shell.setBounds(bounds.x - ((Window.getDefaultOrientation() == SWT.RIGHT_TO_LEFT) ? trayWidth : 0), bounds.y,
                bounds.width + trayWidth, bounds.height);
        sash.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event event) {
                if (event.detail != SWT.DRAG) {
                    Rectangle rect = shell.getClientArea();
                    int newWidth = rect.width - event.x - (sash.getSize().x + rightSeparator.getSize().x);
                    if (newWidth != data.widthHint) {
                        data.widthHint = newWidth;
                        shell.layout();
                    }
                }
            }
        });
        this.tray = t;
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
        col = new TableColumn(table, SWT.NONE);
        col.setResizable(true);
        col.setText(Messages.getString("Common.ColumnFile")); //$NON-NLS-1$
        col.setWidth(400);
        col.setMoveable(true);

        // File status
        col = new TableColumn(table, SWT.NONE);
        col.setResizable(true);
        col.setText(Messages.getString("Common.ColumnStatus")); //$NON-NLS-1$
        col.setWidth(70);
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

        if (!untracked) {
            return;
        }
        showUntrackedFilesButton = new Button(this, SWT.CHECK);
        showUntrackedFilesButton.setText(Messages.getString("Common.ShowUntrackedFiles")); //$NON-NLS-1$
        showUntrackedFilesButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    }

    /**
     * @return
     */
    protected CompareEditorInput getCompareEditorInput() {

        if (selectedFile == null) {
            return null;
        }
        IStorageMercurialRevision iStorage = new IStorageMercurialRevision(selectedFile);
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

        if (selectable && untracked) {
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

    public void setResources(List<IResource> resources) {
        IResource[] res = resources.toArray(new IResource[0]);
        CommitResource[] commitResources = new CommitResourceUtil(root).getCommitResources(res);
        getViewer().setInput(commitResources);
        // auto-check all tracked elements
        List<CommitResource> tracked = new ArrayList<CommitResource>();
        for (CommitResource commitResource : commitResources) {
            if (MercurialStatusCache.CHAR_UNKNOWN != commitResource.getStatus()) {
                tracked.add(commitResource);
            }
        }
        getViewer().setCheckedElements(tracked.toArray());
        if (!untracked) {
            selectAllButton.setSelection(true);
        }
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

    /**
     * @param exportPage
     */
    public void addStateListener(Listener listener) {
        stateListeners.add(listener);
    }

    protected void fireStateChanged() {
        for (Object obj : stateListeners.getListeners()) {
            ((Listener) obj).handleEvent(null);
        }
    }

    /**
     * 
     */
    private void closeSash() {
        if (tray == null) {
            throw new IllegalStateException("Tray was not open"); //$NON-NLS-1$
        }
        int trayWidth = trayControl.getSize().x + leftSeparator.getSize().x + sash.getSize().x + rightSeparator.getSize().x;
        trayControl.dispose();
        trayControl = null;
        tray = null;
        leftSeparator.dispose();
        leftSeparator = null;
        rightSeparator.dispose();
        rightSeparator = null;
        sash.dispose();
        sash = null;
        Shell shell = getShell();
        Rectangle bounds = shell.getBounds();
        shell.setBounds(bounds.x + ((Window.getDefaultOrientation() == SWT.RIGHT_TO_LEFT) ? trayWidth : 0), bounds.y, bounds.width - trayWidth, bounds.height);
    }

    /**
     * 
     */
    private void showDiffForSelection() {
        if (trayClosed && selectedFile != null) {
            try {
                openSash();
                trayClosed = false;
            } catch (Exception e1) {
                MercurialEclipsePlugin.logError(e1);
            }
        } else {
            closeSash();
            trayClosed = true;
        }
    }
}
