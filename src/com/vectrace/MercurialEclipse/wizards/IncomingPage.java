package com.vectrace.MercurialEclipse.wizards;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgIncomingClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.FileStatus;
import com.vectrace.MercurialEclipse.model.FileStatus.Action;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.cache.IncomingChangesetCache;
import com.vectrace.MercurialEclipse.ui.ChangeSetLabelProvider;
import com.vectrace.MercurialEclipse.utils.CompareUtils;

final class IncomingPage extends WizardPage {

    private TableViewer changeSetViewer;
    private TableViewer fileStatusViewer;

    protected IncomingPage(String pageName) {
        super(pageName);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            changeSetViewer.setInput(getIncoming());
        }
    }

    private SortedSet<ChangeSet> getIncoming() {

        ChangeSet a = new ChangeSet(0, "00000000", "dummy", "2008-01-01"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        ChangeSet b = new ChangeSet(1, "11111111", "dummy 2 ", "2008-01-02"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        List<FileStatus> st = new ArrayList<FileStatus>();

        st.add(new FileStatus(Action.ADDED, "/foo/bar/path")); //$NON-NLS-1$
        st.add(new FileStatus(Action.MODIFIED, "/foo/bar/path2")); //$NON-NLS-1$
        st.add(new FileStatus(Action.REMOVED, "/foo/bar/path3")); //$NON-NLS-1$
        a.setChangedFiles(st.toArray(new FileStatus[st.size()]));

        TreeSet<ChangeSet> set = new TreeSet<ChangeSet>();
        set.add(a);
        set.add(b);

        // if(true) return set;

        PullRepoWizard wiz = getPullWizard();
        HgRepositoryLocation remote = wiz.getLocation();
        try {
            File bundleFile = HgIncomingClient.getBundleFile(wiz.project,
                    remote);
            System.out.println(bundleFile);
            SortedSet<ChangeSet> incoming = IncomingChangesetCache
                    .getInstance().getIncomingChangeSets(wiz.project,
                            remote.getUrl());
            return incoming;
        } catch (HgException e) {
            MercurialEclipsePlugin.showError(e);
        }
        return new TreeSet<ChangeSet>();
    }

    private PullRepoWizard getPullWizard() {
        PullRepoWizard wiz = (PullRepoWizard) getWizard();
        return wiz;
    }

    public void createControl(Composite parent) {

        Composite container = new Composite(parent, SWT.NULL);
        setControl(container);
        container.setLayout(new FillLayout(SWT.VERTICAL));

        changeSetViewer = new TableViewer(container, SWT.FULL_SELECTION
                | SWT.BORDER);
        changeSetViewer.setContentProvider(new ArrayContentProvider());
        changeSetViewer.setLabelProvider(new ChangeSetLabelProvider());
        Table table = changeSetViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        String[] titles = { "Rev", "Global", "Date", "Author" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        int[] widths = { 50, 150, 150, 100 };
        for (int i = 0; i < titles.length; i++) {
            TableColumn column = new TableColumn(table, SWT.NONE);
            column.setText(titles[i]);
            column.setWidth(widths[i]);
        }

        fileStatusViewer = new TableViewer(container, SWT.FULL_SELECTION
                | SWT.BORDER);
        fileStatusViewer.setContentProvider(new ArrayContentProvider());
        fileStatusViewer.setLabelProvider(new FileStatusLabelProvider());

        table = fileStatusViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        titles = new String[] {
                Messages
                        .getString("IncomingPage.fileStatusTable.columnTitle.status"), Messages.getString("IncomingPage.fileStatusTable.columnTitle.path") }; //$NON-NLS-1$ //$NON-NLS-2$
        widths = new int[] { 80, 400 };
        for (int i = 0; i < titles.length; i++) {
            TableColumn column = new TableColumn(table, SWT.NONE);
            column.setText(titles[i]);
            column.setWidth(widths[i]);
        }
        makeActions();
    }

    ChangeSet getSelectedChangeSet() {
        IStructuredSelection sel = (IStructuredSelection) changeSetViewer
                .getSelection();
        Object firstElement = sel.getFirstElement();
        if (firstElement instanceof ChangeSet) {
            return (ChangeSet) firstElement;
        }
        return null;
    }

    private void makeActions() {
        changeSetViewer
                .addSelectionChangedListener(new ISelectionChangedListener() {
                    public void selectionChanged(SelectionChangedEvent event) {
                        ChangeSet change = getSelectedChangeSet();
                        if (change != null) {
                            fileStatusViewer.setInput(change.getChangedFiles());
                        } else {
                            fileStatusViewer.setInput(new Object[0]);
                        }
                    }
                });

        fileStatusViewer.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent event) {
                ChangeSet change = getSelectedChangeSet();
                IStructuredSelection sel = (IStructuredSelection) event
                        .getSelection();
                FileStatus clickedFileStatus = (FileStatus) sel
                        .getFirstElement();
                if (change != null && clickedFileStatus != null) {
                    org.eclipse.core.resources.IResource file = getPullWizard().project
                            .findMember(clickedFileStatus.getPath());
                    CompareUtils.openEditor(file, change, true);
                }
            }
        });
    }

    private static class FileStatusLabelProvider extends LabelProvider
            implements ITableLabelProvider {

        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }

        public String getColumnText(Object element, int columnIndex) {
            if (!(element instanceof FileStatus)) {
                return Messages.getString("IncomingPage.unknownElement") + element; //$NON-NLS-1$
            }
            FileStatus status = (FileStatus) element;
            switch (columnIndex) {
            case 0:
                return status.getAction().name();
            case 1:
                return status.getPath();
            }
            return Messages.getString("IncomingPage.notApplicable"); //$NON-NLS-1$
        }
    }
}
