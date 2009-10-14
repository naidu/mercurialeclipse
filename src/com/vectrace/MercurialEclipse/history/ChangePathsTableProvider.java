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

import java.util.WeakHashMap;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgLogClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.FileStatus;
import com.vectrace.MercurialEclipse.team.MercurialRevisionStorage;
import com.vectrace.MercurialEclipse.team.NullRevision;
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
                if (rev == null || clickedFileStatus == null) {
                    return;
                }
                ChangeSet cs = rev.getChangeSet();
                String[] parents = cs.getParents();

                IPath hgRoot = new Path(cs.getHgRoot().getPath());
                IPath fileRelPath = clickedFileStatus.getPath();
                IPath fileAbsPath = hgRoot.append(fileRelPath);
                IResource file = ResourcesPlugin.getWorkspace().getRoot()
                    .getFileForLocation(fileAbsPath);
                MercurialRevisionStorage thisRev = new MercurialRevisionStorage(file, cs.getChangeset());
                MercurialRevisionStorage parentRev ;
                if(cs.getRevision().getRevision() == 0 || parents.length == 0){
                    parentRev = new NullRevision(file, cs);
                } else {
                    parentRev = new MercurialRevisionStorage(file, parents[0]);
                }
                CompareUtils.openEditor(thisRev, parentRev, false, false);
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

    private static class ChangePathLabelProvider extends LabelProvider implements
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
                return changePath.getPath().toOSString();
            }
            return ""; //$NON-NLS-1$
        }
    }

    private static final FileStatus[] EMPTY_CHANGE_PATHS = new FileStatus[0];

    private static class ChangePathsTableContentProvider implements
            IStructuredContentProvider {

        private final WeakHashMap<MercurialRevision, FileStatus[]> revToFiles;
        private final ChangedPathsPage page;
        private Viewer viewer;
        private boolean disposed;

        public ChangePathsTableContentProvider(ChangedPathsPage page) {
            this.page = page;
            revToFiles = new WeakHashMap<MercurialRevision, FileStatus[]>();
        }

        public Object[] getElements(Object inputElement) {
            if (!this.page.isShowChangePaths()) {
                return EMPTY_CHANGE_PATHS;
            }

            MercurialRevision rev = ((MercurialRevision) inputElement);
            FileStatus[] fileStatus;
            synchronized(revToFiles){
                fileStatus = revToFiles.get(rev);
            }
            if(fileStatus != null){
                return fileStatus;
            }
            fetchPaths(rev);
            // but sometimes hg returns a null version map...
            return EMPTY_CHANGE_PATHS;
        }

        private void fetchPaths(final MercurialRevision rev) {
            final MercurialHistory history = page.getMercurialHistory();
            final ChangeSet [] cs = new ChangeSet[1];
            Job pathJob = new Job("Retrieving affected paths for " + rev.getChangeSet()){
                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    synchronized(revToFiles){
                        if(revToFiles.get(rev) != null){
                            return Status.OK_STATUS;
                        }
                    }
                    try {
                        cs[0] = HgLogClient.getLogWithBranchInfo(rev, history, monitor);
                    } catch (HgException e) {
                        MercurialEclipsePlugin.logError(e);
                        return e.getStatus();
                    }
                    return Status.OK_STATUS;
                }
            };
            pathJob.setRule(new ExclusiveHistoryRule());
            pathJob.addJobChangeListener(new JobChangeAdapter(){
                @Override
                public void done(IJobChangeEvent event) {
                    FileStatus[] changedFiles = EMPTY_CHANGE_PATHS;
                    if(cs[0] != null) {
                        changedFiles = cs[0].getChangedFiles();
                        if(changedFiles == null || changedFiles.length == 0){
                            changedFiles = EMPTY_CHANGE_PATHS;
                        }
                    }
                    synchronized(revToFiles){
                        if(!revToFiles.containsKey(rev)) {
                            revToFiles.put(rev, changedFiles);
                        }
                    }
                    if(disposed){
                        return;
                    }
                    Runnable refresh = new Runnable() {
                        public void run() {
                            if(!disposed && viewer != null) {
                                viewer.refresh();
                            }
                        }
                    };
                    Display.getDefault().asyncExec(refresh);
                }
            });
            if(!disposed) {
                page.getHistoryPage().scheduleInPage(pathJob);
            }
        }

        public void dispose() {
            disposed = true;
            synchronized(revToFiles){
                revToFiles.clear();
            }
        }

        public void inputChanged(Viewer viewer1, Object oldInput, Object newInput) {
            this.viewer = viewer1;
        }
    }
}
