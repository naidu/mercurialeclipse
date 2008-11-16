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

import org.eclipse.compare.ResourceNode;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import com.vectrace.MercurialEclipse.TableColumnSorter;
import com.vectrace.MercurialEclipse.compare.RevisionNode;
import com.vectrace.MercurialEclipse.dialogs.CommitDialog;
import com.vectrace.MercurialEclipse.dialogs.CommitResource;
import com.vectrace.MercurialEclipse.dialogs.CommitResourceLabelProvider;
import com.vectrace.MercurialEclipse.dialogs.CommitResourceUtil;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.IStorageMercurialRevision;
import com.vectrace.MercurialEclipse.utils.CompareUtils;
import com.vectrace.MercurialEclipse.wizards.Messages;

/**
 * TODO enable tree/flat view switch
 * 
 * @author steeven
 * 
 */
public class CommitFilesChooser extends Composite {
    private UntrackedFilesFilter untrackedFilesFilter;
    private CommittableFilesFilter committableFilesFilter;
    private final HgRoot root;
    private final boolean selectable;
    private Button showUntrackedFilesButton;
    private Button selectAllButton;
    private CheckboxTableViewer viewer;
    private final boolean untracked;

    /**
     * @return the viewer
     */
    public CheckboxTableViewer getViewer() {
        return viewer;
    }

    public CommitFilesChooser(Composite container, boolean selectable,
            List<IResource> resources, HgRoot hgRoot, boolean showUntracked) {
        super(container, SWT.None);

        this.selectable = selectable;
        this.root = hgRoot;
        this.untracked = showUntracked;
        this.untrackedFilesFilter = new UntrackedFilesFilter();
        this.committableFilesFilter = new CommittableFilesFilter();

        GridLayout layout = new GridLayout();
        layout.verticalSpacing = 3;
        layout.horizontalSpacing = 0;
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        setLayout(layout);

        Table table = createTable();
        createOptionCheckbox();

        viewer = new CheckboxTableViewer(table);
        viewer.setContentProvider(new ArrayContentProvider());
        viewer.setLabelProvider(new CommitResourceLabelProvider());
        viewer.addFilter(committableFilesFilter);
        if (!showUntracked)
            viewer.addFilter(untrackedFilesFilter);

        setResources(resources);
        makeActions();
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
        TableLayout layout = new TableLayout();

        TableColumn col;

        // Check mark
        col = new TableColumn(table, SWT.NONE | SWT.BORDER);
        col.setResizable(false);
        col.setText(""); //$NON-NLS-1$
        layout.addColumnData(new ColumnPixelData(20, false));
        // File name
        col = new TableColumn(table, SWT.NONE);
        col.setResizable(true);
        col.setText(Messages.getString("Common.ColumnFile")); //$NON-NLS-1$
        layout.addColumnData(new ColumnPixelData(320, true));

        // File status
        col = new TableColumn(table, SWT.NONE);
        col.setResizable(true);
        col.setText(Messages.getString("Common.ColumnStatus")); //$NON-NLS-1$
        layout.addColumnData(new ColumnPixelData(100, true));

        table.setLayout(layout);

        table.setLayoutData(new GridData(GridData.FILL_BOTH));
        return table;
    }

    private void createOptionCheckbox() {
        if (!selectable)
            return;
        selectAllButton = new Button(this, SWT.CHECK);
        selectAllButton.setText(Messages.getString("Common.SelectOrUnselectAll")); //$NON-NLS-1$
        selectAllButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        if (!untracked)
            return;
        showUntrackedFilesButton = new Button(this, SWT.CHECK);
        showUntrackedFilesButton.setText(Messages.getString("Common.ShowUntrackedFiles")); //$NON-NLS-1$
        showUntrackedFilesButton.setLayoutData(new GridData(
                GridData.FILL_HORIZONTAL));
    }

    private void makeActions() {
        getViewer().addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent event) {
                IStructuredSelection sel = (IStructuredSelection) getViewer()
                        .getSelection();
                if (sel.getFirstElement() instanceof CommitResource) {
                    CommitResource resource = (CommitResource) sel
                            .getFirstElement();

                    // workspace version
                    ResourceNode leftNode = new ResourceNode(resource
                            .getResource());

                    // mercurial version
                    RevisionNode rightNode = new RevisionNode(
                            new IStorageMercurialRevision(resource
                                    .getResource()));

                    CompareUtils.openCompareDialog(leftNode, rightNode, false);
                }
            }
        });
        if (selectable)
            selectAllButton.setSelection(false); // Start not selected
        if (selectable)
            selectAllButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (selectAllButton.getSelection()) {
                        getViewer().setAllChecked(true);
                    } else {
                        getViewer().setAllChecked(false);
                    }
                }
            });

        if (selectable && untracked) {
            showUntrackedFilesButton.setSelection(true); // Start selected.
            showUntrackedFilesButton
                    .addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            if (showUntrackedFilesButton.getSelection()) {
                                getViewer().removeFilter(untrackedFilesFilter);
                            } else {
                                getViewer().addFilter(untrackedFilesFilter);
                            }
                            getViewer().refresh(true);
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
                    ITableLabelProvider lp = ((ITableLabelProvider) v1
                            .getLabelProvider());
                    String t1 = lp.getColumnText(e1, colIdx);
                    String t2 = lp.getColumnText(e2, colIdx);
                    return t1.compareTo(t2);
                }
            };
        }
    }

    public void setResources(List<IResource> resources) {
        IResource[] res = resources.toArray(new IResource[0]);
        CommitResource[] commitResources = new CommitResourceUtil(root)
                .getCommitResources(res);
        getViewer().setInput(commitResources);
        // auto-check all tracked elements
        List<CommitResource> tracked = new ArrayList<CommitResource>();
        for (CommitResource commitResource : commitResources) {
            if (commitResource.getStatus() != CommitDialog.FILE_UNTRACKED) {
                tracked.add(commitResource);
            }
        }
        getViewer().setCheckedElements(tracked.toArray());
        if (!untracked)
            showUntrackedFilesButton.setSelection(true);
    }

    public ArrayList<IResource> getCheckedResources(String... status) {
        ArrayList<IResource> list = new ArrayList<IResource>();
        for (Object res : getViewer().getCheckedElements()) {
            if (res instanceof CommitResource != true)
                return null;
            CommitResource resource = (CommitResource) res;
            if (status == null || status.length == 0)
                list.add(resource.getResource());
            else {
                for (String stat : status)
                    if (resource.getStatus().equals(stat)) {
                        list.add(resource.getResource());
                        break;
                    }
            }
        }
        return list;
    }

}
