/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Software Balm Consulting Inc (Peter Hunnisett <peter_hge at softwarebalm dot com>) - implementation
 *     StefanC - many updates
 *     VecTrace (Zingo Andersen) - some updates
 *******************************************************************************/
package com.vectrace.MercurialEclipse.dialogs;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.ResourceNode;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
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
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AnnotationPreference;
import org.eclipse.ui.texteditor.DefaultMarkerAnnotationAccess;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
import org.eclipse.ui.texteditor.spelling.SpellingAnnotation;
import org.eclipse.ui.texteditor.spelling.SpellingContext;
import org.eclipse.ui.texteditor.spelling.SpellingService;

import com.vectrace.MercurialEclipse.TableColumnSorter;
import com.vectrace.MercurialEclipse.compare.RevisionNode;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.IStorageMercurialRevision;
import com.vectrace.MercurialEclipse.ui.TextSpellingProblemCollector;
import com.vectrace.MercurialEclipse.utils.CompareUtils;

/**
 * 
 * A commit dialog box allowing choosing of what files to commit and a commit
 * message for those files. Untracked files may also be chosen.
 * 
 */
public class CommitDialog extends TrayDialog {
    public static final String FILE_MODIFIED = "Modified";
    public static final String FILE_ADDED = "Added";
    public static final String FILE_REMOVED = "Removed";
    public static final String FILE_UNTRACKED = "Untracked";
    public static final String FILE_DELETED = "Already Deleted";

    private class CommittableFilesFilter extends ViewerFilter {
        public CommittableFilesFilter() {
            super();
        }

        /**
         * Filter out un commitable files (i.e. ! -> deleted but still tracked)
         */
        @Override
        public boolean select(Viewer viewer, Object parentElement,
                Object element) {
            if (element instanceof CommitResource) {
                return true;
            }
            return true;
        }
    }

    private String defaultCommitMessage = "(no commit message)";

    private ISourceViewer commitTextBox;
    private Label commitTextLabel;
    private Label commitFilesLabel;
    private CheckboxTableViewer commitFilesList;
    private boolean selectableFiles;
    private Button showUntrackedFilesButton;
    private Button selectAllButton;
    private UntrackedFilesFilter untrackedFilesFilter;
    private CommittableFilesFilter committableFilesFilter;
    private HgRoot root;
    private File[] filesToAdd;
    private List<IResource> resourcesToAdd;
    private File[] filesToCommit;
    private IResource[] resourcesToCommit;
    private String commitMessage;
    private IResource[] inResources;
    private File[] filesToRemove;
    private List<IResource> resourcesToRemove;
    private IDocument commitTextDocument;
    private SourceViewerDecorationSupport decorationSupport;

    /**
     * @param shell
     */
    public CommitDialog(Shell shell, HgRoot root, IResource[] inResources) {
        super(shell);
        setShellStyle(getShellStyle() | SWT.RESIZE | SWT.TITLE);
        this.root = root;
        this.inResources = inResources;
        this.untrackedFilesFilter = new UntrackedFilesFilter();
        this.committableFilesFilter = new CommittableFilesFilter();
        this.selectableFiles = true;
        this.commitTextDocument = new Document();
    }

    public CommitDialog(Shell shell, HgRoot root, IResource[] inResources,
            String defaultCommitMessage, boolean selectableFiles) {
        this(shell, root, inResources);
        this.selectableFiles = selectableFiles;
        this.defaultCommitMessage = defaultCommitMessage;
    }

    public String getCommitMessage() {
        return commitMessage;
    }

    public File[] getFilesToCommit() {
        return filesToCommit;
    }

    public IResource[] getResourcesToCommit() {
        return resourcesToCommit;
    }

    public File[] getFilesToAdd() {
        return filesToAdd;
    }

    public List<IResource> getResourcesToAdd() {
        return resourcesToAdd;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        container.setLayout(new FormLayout());

        commitTextLabel = new Label(container, SWT.NONE);
        commitTextLabel.setText("Commit comments");

        // commitTextBox = new Text(container, SWT.V_SCROLL | SWT.MULTI
        // | SWT.BORDER | SWT.WRAP);

        commitTextBox = new SourceViewer(container, null, SWT.V_SCROLL
                | SWT.MULTI | SWT.BORDER | SWT.WRAP);
        commitTextBox.setEditable(true);

        // set up spell-check annotations
        
        AnnotationModel annotationModel = new AnnotationModel();
        commitTextBox.setDocument(commitTextDocument, annotationModel);        
        
        decorationSupport = new SourceViewerDecorationSupport(commitTextBox,
                null, new DefaultMarkerAnnotationAccess(), EditorsUI
                        .getSharedTextColors());
                
        AnnotationPreference pref = new AnnotationPreference();
        pref.setAnnotationType(SpellingAnnotation.TYPE);
        pref.setColorPreferenceKey("spellingIndicationColor");
        pref.setHighlightPreferenceKey("spellingIndicationHighlighting");                
        pref.setTextPreferenceKey("spellingIndication");
        
        decorationSupport.setAnnotationPreference(pref);
        decorationSupport.install(EditorsUI.getPreferenceStore());                       

        ITextListener textListener = new ITextListener() {

            private SpellingService spellService;
            private SpellingContext spellContext;
            private TextSpellingProblemCollector collector;

            public void textChanged(TextEvent event) {
                // reset foreground color
                commitTextBox.setTextColor(PlatformUI.getWorkbench()
                        .getDisplay().getSystemColor(
                                SWT.COLOR_WIDGET_FOREGROUND), 0,
                        commitTextDocument.getLength(), false);

                // connect to spell service if necessary
                if (spellService == null) {
                    spellService = EditorsUI.getSpellingService();
                }

                if (spellContext == null) {
                    spellContext = new SpellingContext();
                    IContentType contentType = Platform.getContentTypeManager()
                            .getContentType(IContentTypeManager.CT_TEXT);
                    spellContext.setContentType(contentType);
                }

                if (collector == null) {
                    collector = new TextSpellingProblemCollector(commitTextBox);
                }

                // check and highlight errors
                spellService.check(commitTextDocument, spellContext, collector,
                        null);
            }
        };
        commitTextBox.addTextListener(textListener);

        commitFilesLabel = new Label(container, SWT.NONE);
        commitFilesLabel.setText("Select Files:");

        commitFilesList = createFilesList(container, selectableFiles);

        final FormData fd_commitTextLabel = new FormData();
        fd_commitTextLabel.top = new FormAttachment(0, 20);
        fd_commitTextLabel.left = new FormAttachment(0, 9);
        fd_commitTextLabel.right = new FormAttachment(100, -9);
        commitTextLabel.setLayoutData(fd_commitTextLabel);

        final FormData fd_commitTextBox = new FormData();
        fd_commitTextBox.top = new FormAttachment(commitTextLabel, 3,
                SWT.BOTTOM);
        fd_commitTextBox.left = new FormAttachment(0, 9);
        fd_commitTextBox.bottom = new FormAttachment(0, 200);
        fd_commitTextBox.right = new FormAttachment(100, -9);
        commitTextBox.getTextWidget().setLayoutData(fd_commitTextBox);

        final FormData fd_commitFilesLabel = new FormData();
        fd_commitFilesLabel.top = new FormAttachment(commitTextBox
                .getTextWidget(), 3);
        fd_commitFilesLabel.left = new FormAttachment(0, 9);
        fd_commitFilesLabel.right = new FormAttachment(100, -9);
        commitFilesLabel.setLayoutData(fd_commitFilesLabel);

        Table table = commitFilesList.getTable();
        final FormData fd_table = new FormData();
        fd_table.top = new FormAttachment(commitFilesLabel, 3);
        fd_table.left = new FormAttachment(0, 9);
        fd_table.right = new FormAttachment(100, -9);
        fd_table.bottom = new FormAttachment(100, -9);
        table.setLayoutData(fd_table);

        if (selectableFiles) {

            selectAllButton = new Button(container, SWT.CHECK);
            selectAllButton.setText("Select/unselect all");

            showUntrackedFilesButton = new Button(container, SWT.CHECK);
            showUntrackedFilesButton.setText("Show added/removed files");

            fd_table.bottom = new FormAttachment(selectAllButton, -9);

            final FormData fd_selectAllButton = new FormData();
            fd_selectAllButton.bottom = new FormAttachment(
                    showUntrackedFilesButton);
            fd_selectAllButton.left = new FormAttachment(0, 9);
            fd_selectAllButton.right = new FormAttachment(100, -9);
            selectAllButton.setLayoutData(fd_selectAllButton);

            final FormData fd_showUntrackedFilesButton = new FormData();
            fd_showUntrackedFilesButton.bottom = new FormAttachment(100, -34);
            fd_showUntrackedFilesButton.right = new FormAttachment(100, -9);
            fd_showUntrackedFilesButton.left = new FormAttachment(0, 9);
            showUntrackedFilesButton.setLayoutData(fd_showUntrackedFilesButton);
        }
        makeActions();
        return container;
    }

    private void makeActions() {
        // commitTextBox.setCapture(true);
        commitFilesList.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent event) {
                IStructuredSelection sel = (IStructuredSelection) commitFilesList
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
        if (selectableFiles) {
            selectAllButton.setSelection(false); // Start not selected
            showUntrackedFilesButton.setSelection(true); // Start selected.

            showUntrackedFilesButton
                    .addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            if (showUntrackedFilesButton.getSelection()) {
                                commitFilesList
                                        .removeFilter(untrackedFilesFilter);
                            } else {
                                commitFilesList.addFilter(untrackedFilesFilter);
                            }
                            commitFilesList.refresh(true);
                        }
                    });
            selectAllButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (selectAllButton.getSelection()) {
                        commitFilesList.setAllChecked(true);
                    } else {
                        commitFilesList.setAllChecked(false);
                    }
                }
            });

        }
        setupDefaultCommitMessage();

        final Table table = commitFilesList.getTable();
        TableColumn[] columns = table.getColumns();
        for (int ci = 0; ci < columns.length; ci++) {
            TableColumn column = columns[ci];
            final int colIdx = ci;
            new TableColumnSorter(commitFilesList, column) {
                @Override
                protected int doCompare(Viewer v, Object e1, Object e2) {
                    StructuredViewer viewer = (StructuredViewer) v;
                    ITableLabelProvider lp = ((ITableLabelProvider) viewer
                            .getLabelProvider());
                    String t1 = lp.getColumnText(e1, colIdx);
                    String t2 = lp.getColumnText(e2, colIdx);
                    return t1.compareTo(t2);
                }
            };
        }
    }

    private void setupDefaultCommitMessage() {
        commitTextDocument.set(defaultCommitMessage);
        commitTextBox.setSelectedRange(0, defaultCommitMessage.length());
    }

    private CheckboxTableViewer createFilesList(Composite container,
            boolean selectable) {
        int flags = SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER;
        if (selectable) {
            flags |= SWT.CHECK | SWT.FULL_SELECTION | SWT.MULTI;
        } else {
            flags |= SWT.READ_ONLY | SWT.HIDE_SELECTION;
        }
        Table table = new Table(container, flags);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        TableLayout layout = new TableLayout();

        TableColumn col;

        // Check mark
        col = new TableColumn(table, SWT.NONE | SWT.BORDER);
        col.setResizable(false);
        col.setText("");
        layout.addColumnData(new ColumnPixelData(20, false));
        // File name
        col = new TableColumn(table, SWT.NONE);
        col.setResizable(true);
        col.setText("File");
        layout.addColumnData(new ColumnPixelData(320, true));

        // File status
        col = new TableColumn(table, SWT.NONE);
        col.setResizable(true);
        col.setText("Status");
        layout.addColumnData(new ColumnPixelData(100, true));

        table.setLayout(layout);
        commitFilesList = new CheckboxTableViewer(table);

        commitFilesList.setContentProvider(new ArrayContentProvider());

        commitFilesList.setLabelProvider(new CommitResourceLabelProvider());

        CommitResource[] commitResources = new CommitResourceUtil(getRoot())
                .getCommitResources(inResources);
        commitFilesList.setInput(commitResources);
        commitFilesList.addFilter(committableFilesFilter);
        // auto-check all tracked elements
        List<CommitResource> tracked = new ArrayList<CommitResource>();
        for (CommitResource commitResource : commitResources) {
            if (commitResource.getStatus() != CommitDialog.FILE_UNTRACKED) {
                tracked.add(commitResource);
            }
        }
        commitFilesList.setCheckedElements(tracked.toArray());
        return commitFilesList;
    }

    private File[] convertToFiles(Object[] objs) {
        ArrayList<File> list = new ArrayList<File>();

        for (int res = 0; res < objs.length; res++) {
            if (objs[res] instanceof CommitResource != true) {
                return null;
            }

            CommitResource resource = (CommitResource) objs[res];
            list.add(resource.getPath());
        }

        return list.toArray(new File[0]);
    }

    private IResource[] convertToResource(Object[] objs) {
        ArrayList<IResource> list = new ArrayList<IResource>();

        for (int res = 0; res < objs.length; res++) {
            if (objs[res] instanceof CommitResource != true) {
                return null;
            }

            CommitResource resource = (CommitResource) objs[res];
            IResource thisResource = resource.getResource();
            if (thisResource != null) {
                list.add(thisResource);
            }
        }

        return list.toArray(new IResource[0]);
    }

    private File[] getToAddList(Object[] objs) {
        ArrayList<File> list = new ArrayList<File>();

        for (int res = 0; res < objs.length; res++) {
            if (objs[res] instanceof CommitResource != true) {
                return null;
            }

            CommitResource resource = (CommitResource) objs[res];
            if (resource.getStatus().equals(CommitDialog.FILE_UNTRACKED)) {
                list.add(resource.getPath());
            }
        }

        return list.toArray(new File[0]);
    }

    private File[] getToRemoveList(Object[] objs) {
        ArrayList<File> list = new ArrayList<File>();

        for (int res = 0; res < objs.length; res++) {
            if (objs[res] instanceof CommitResource != true) {
                return null;
            }

            CommitResource resource = (CommitResource) objs[res];
            if (resource.getStatus().equals(CommitDialog.FILE_DELETED)) {
                list.add(resource.getPath());
            }
        }

        return list.toArray(new File[0]);
    }

    private List<IResource> getToAddResourceList(Object[] objs) {
        ArrayList<IResource> list = new ArrayList<IResource>();

        for (int res = 0; res < objs.length; res++) {
            if (objs[res] instanceof CommitResource != true) {
                return null;
            }

            CommitResource resource = (CommitResource) objs[res];
            if (resource.getStatus().equals(CommitDialog.FILE_UNTRACKED)) {
                list.add(resource.getResource());
            }
        }

        return list;
    }

    private List<IResource> getToRemoveResourceList(Object[] objs) {
        ArrayList<IResource> list = new ArrayList<IResource>();

        for (int res = 0; res < objs.length; res++) {
            if (objs[res] instanceof CommitResource != true) {
                return null;
            }

            CommitResource resource = (CommitResource) objs[res];
            if (resource.getStatus().equals(CommitDialog.FILE_DELETED)) {
                list.add(resource.getResource());
            }
        }

        return list;
    }

    /**
     * Override the OK button pressed to capture the info we want first and then
     * call super.
     */
    @Override
    protected void okPressed() {
        filesToAdd = getToAddList(commitFilesList.getCheckedElements());
        resourcesToAdd = getToAddResourceList(commitFilesList
                .getCheckedElements());

        filesToCommit = convertToFiles(commitFilesList.getCheckedElements());
        resourcesToCommit = convertToResource(commitFilesList
                .getCheckedElements());

        filesToRemove = getToRemoveList(commitFilesList.getCheckedElements());
        resourcesToRemove = getToRemoveResourceList(commitFilesList
                .getCheckedElements());
        commitMessage = commitTextDocument.get();

        super.okPressed();
    }

    @Override
    protected Point getInitialSize() {
        return new Point(477, 562);
    }

    protected void setRoot(HgRoot root) {
        this.root = root;
    }

    protected HgRoot getRoot() {
        return root;
    }

    public File[] getFilesToRemove() {
        return filesToRemove;
    }

    public List<IResource> getResourcesToRemove() {
        return resourcesToRemove;
    }
}