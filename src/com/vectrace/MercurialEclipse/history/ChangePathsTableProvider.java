/*******************************************************************************
 * Copyright (c) 2004, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package com.vectrace.MercurialEclipse.history;

import java.io.IOException;
import java.util.Map;
import java.util.SortedSet;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgLogClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.FileStatus;
import com.vectrace.MercurialEclipse.team.IStorageMercurialRevision;
import com.vectrace.MercurialEclipse.utils.CompareUtils;
import com.vectrace.MercurialEclipse.wizards.Messages;

public class ChangePathsTableProvider extends TableViewer {

    public ChangePathsTableProvider(Composite parent,
            IContentProvider contentProvider) {
        super(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);

        TableLayout layout = new TableLayout();
        GridData data = new GridData(GridData.FILL_BOTH);

        final Table table = (Table) getControl();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayoutData(data);
        table.setLayout(layout);

        this.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent event) {
                IStructuredSelection sel = (IStructuredSelection) event
                        .getSelection();
                FileStatus clickedFileStatus = (FileStatus) sel
                        .getFirstElement();
                MercurialRevision rev = (MercurialRevision) getInput();
                if (rev != null && clickedFileStatus != null) {
                    ChangeSet cs = rev.getChangeSet();
                    String[] parents = cs.getParents();

                    IPath hgRoot;
                    try {
                        hgRoot = new Path(cs.getHgRoot().getCanonicalPath());
                        IPath fileRelPath = new Path(clickedFileStatus
                                .getPath());
                        IPath fileAbsPath = hgRoot.append(fileRelPath);
                        IResource file = rev.getResource().getWorkspace().getRoot()
                            .getFileForLocation(fileAbsPath);
                        IStorageMercurialRevision thisRev = new IStorageMercurialRevision(file, cs.getChangeset());
                        IStorageMercurialRevision parentRev = new IStorageMercurialRevision(file, parents[0]);
                        CompareUtils.openEditor(thisRev, parentRev, false, false);
                    } catch (IOException e) {
                        MercurialEclipsePlugin.logError(e);
                    }
                }
            }
        });

        createColumns(table, layout);

        setLabelProvider(new ChangePathLabelProvider());
        setContentProvider(contentProvider);
    }

    public ChangePathsTableProvider(Composite parent, ChangedPathsPage page) {
        this(parent, new ChangePathsTableContentProvider(page));
    }

    private void createColumns(Table table, TableLayout layout) {
        // action
        TableColumn col = new TableColumn(table, SWT.NONE);
        col.setResizable(true);
        col.setText(Messages.getString("ChangePathsTableProvider.action")); //$NON-NLS-1$
        layout.addColumnData(new ColumnWeightData(10, true));

        // path
        col = new TableColumn(table, SWT.NONE);
        col.setResizable(true);
        col.setText(Messages.getString("ChangePathsTableProvider.path")); //$NON-NLS-1$
        layout.addColumnData(new ColumnWeightData(45, true));
        table.setSortColumn(col);
    }

    // column constants
    private static final int COL_ACTION = 0;
    private static final int COL_PATH = 1;

    private class ChangePathLabelProvider extends LabelProvider implements
            ITableLabelProvider {
        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }

        public String getColumnText(Object element, int columnIndex) {
            FileStatus changePath = (FileStatus) element;
            if (changePath == null) {
                return ""; //$NON-NLS-1$
            }
            switch (columnIndex) {
            case COL_ACTION:
                return "" + changePath.getAction(); //$NON-NLS-1$
            case COL_PATH:
                return changePath.getPath();
            }
            return ""; //$NON-NLS-1$
        }
    }

    private static final FileStatus[] EMPTY_CHANGE_PATHS = new FileStatus[0];

    private static class ChangePathsTableContentProvider implements
            IStructuredContentProvider {

        private final ChangedPathsPage page;

        public ChangePathsTableContentProvider(ChangedPathsPage page) {
            this.page = page;
        }

        public Object[] getElements(Object inputElement) {
            if (!this.page.isShowChangePaths()) {
                return EMPTY_CHANGE_PATHS;
            }

            MercurialRevision rev = ((MercurialRevision) inputElement);
            Map<IPath, SortedSet<ChangeSet>> map;
            try {
                map = HgLogClient.getProjectLog(rev.getResource(),
                        1, rev.getChangeSet().getChangesetIndex(), true);
            } catch (HgException e) {
                return EMPTY_CHANGE_PATHS;
            }
            return map.get(rev.getResource().getLocation()).first().getChangedFiles();
        }

        public void dispose() {
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        }
    }
}
