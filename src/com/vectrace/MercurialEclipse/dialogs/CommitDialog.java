/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Software Balm Consulting Inc (Peter Hunnisett <peter_hge at softwarebalm dot com>) - implementation
 *     Bastian Doetsch - Added spellchecking.
 *     StefanC - many updates
 *     Zingo Andersen - some updates
 *******************************************************************************/
package com.vectrace.MercurialEclipse.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.eclipse.ui.texteditor.AnnotationPreference;
import org.eclipse.ui.texteditor.DefaultMarkerAnnotationAccess;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
import org.eclipse.ui.texteditor.spelling.SpellingAnnotation;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.ui.CommitFilesChooser;

/**
 * 
 * A commit dialog box allowing choosing of what files to commit and a commit
 * message for those files. Untracked files may also be chosen.
 * 
 */
public class CommitDialog extends TrayDialog {
    public static final String FILE_MODIFIED = Messages.getString("CommitDialog.modified"); //$NON-NLS-1$
    public static final String FILE_ADDED = Messages.getString("CommitDialog.added"); //$NON-NLS-1$
    public static final String FILE_REMOVED = Messages.getString("CommitDialog.removed"); //$NON-NLS-1$
    public static final String FILE_UNTRACKED = Messages.getString("CommitDialog.untracked"); //$NON-NLS-1$
    public static final String FILE_DELETED = Messages.getString("CommitDialog.deletedInWorkspace"); //$NON-NLS-1$


    private String defaultCommitMessage = Messages.getString("CommitDialog.defaultCommitMessage"); //$NON-NLS-1$
    private Combo oldCommitComboBox;
    private ISourceViewer commitTextBox;
    private Label commitTextLabel;
    private Label commitFilesLabel;
    private CommitFilesChooser commitFilesList;
    private boolean selectableFiles;
    private HgRoot root;
    private List<IResource> resourcesToAdd;
    private List<IResource> resourcesToCommit;
    private List<IResource> resourcesToRemove;
    private String commitMessage;
    private IDocument commitTextDocument;
    private SourceViewerDecorationSupport decorationSupport;
    private List<IResource> inResources;

    /**
     * @param shell
     */
    public CommitDialog(Shell shell, HgRoot root, List<IResource> resources) {
        super(shell);
        setShellStyle(getShellStyle() | SWT.RESIZE | SWT.TITLE);
        this.root = root;
        this.inResources = resources;
        this.selectableFiles = true;
        this.commitTextDocument = new Document();
    }

    public CommitDialog(Shell shell, HgRoot root, ArrayList<IResource> selectedResource,
            String defaultCommitMessage, boolean selectableFiles) {
        this(shell, root, selectedResource);
        this.selectableFiles = selectableFiles;
        this.defaultCommitMessage = defaultCommitMessage;
    }

    public String getCommitMessage() {
        return commitMessage;
    }

    public List<IResource> getResourcesToCommit() {
        return resourcesToCommit;
    }

    public List<IResource> getResourcesToAdd() {
        return resourcesToAdd;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        container.setLayout(new FormLayout());

        commitTextLabel = new Label(container, SWT.NONE);
        commitTextLabel.setText(Messages.getString("CommitDialog.commitTextLabel.text")); //$NON-NLS-1$

        commitTextBox = new SourceViewer(container, null, SWT.V_SCROLL
                | SWT.MULTI | SWT.BORDER | SWT.WRAP);
        commitTextBox.setEditable(true);

        
        final String oldCommits[] = MercurialEclipsePlugin.getCommitMessageManager().getCommitMessages();
        if(oldCommits.length > 0)
        {
            oldCommitComboBox = new Combo(container, SWT.READ_ONLY);
            oldCommitComboBox.add(Messages.getString("CommitDialog.oldCommitMessages")); //$NON-NLS-1$ 
            oldCommitComboBox.setText(Messages.getString("CommitDialog.oldCommitMessages")); //$NON-NLS-1$
            for (int i = 0; i < oldCommits.length; i++) {
                /* Add text to the combo but replace \n with <br> to get a one-liner */
                oldCommitComboBox.add(oldCommits[i].replaceAll("\\n","<br>")); 
            }
            oldCommitComboBox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if(oldCommitComboBox.getSelectionIndex() != 0)
                    {
                        commitTextDocument.set(oldCommits[oldCommitComboBox.getSelectionIndex() - 1]);
                        commitTextBox.setSelectedRange(0, oldCommits[oldCommitComboBox.getSelectionIndex() - 1].length());
                    }

                }
            });
        }
        
        // set up spell-check annotations
        decorationSupport = new SourceViewerDecorationSupport(commitTextBox,
                null, new DefaultMarkerAnnotationAccess(), EditorsUI
                        .getSharedTextColors());

        AnnotationPreference pref = EditorsUI.getAnnotationPreferenceLookup()
                .getAnnotationPreference(SpellingAnnotation.TYPE);

        decorationSupport.setAnnotationPreference(pref);
        decorationSupport.install(EditorsUI.getPreferenceStore());

        commitTextBox.configure(new TextSourceViewerConfiguration(EditorsUI
                .getPreferenceStore()));
        AnnotationModel annotationModel = new AnnotationModel();
        commitTextBox.setDocument(commitTextDocument, annotationModel);
        commitTextBox.getTextWidget().addDisposeListener(new DisposeListener() {

            public void widgetDisposed(DisposeEvent e) {
                decorationSupport.uninstall();
            }

        });

        commitFilesLabel = new Label(container, SWT.NONE);
        commitFilesLabel.setText(Messages.getString("CommitDialog.selectFiles")); //$NON-NLS-1$

        final FormData fd_commitTextLabel = new FormData();
        fd_commitTextLabel.top = new FormAttachment(0, 20);
        fd_commitTextLabel.left = new FormAttachment(0, 9);
        fd_commitTextLabel.right = new FormAttachment(100, -9);
        commitTextLabel.setLayoutData(fd_commitTextLabel);

        final FormData fd_commitTextBox = new FormData();

        if (oldCommitComboBox != null)
        {        
            final FormData fd_oldcommitComboBox = new FormData();
            fd_oldcommitComboBox.top = new FormAttachment(commitTextLabel, 3, SWT.BOTTOM);
            fd_oldcommitComboBox.left = new FormAttachment(0, 9);
            fd_oldcommitComboBox.right = new FormAttachment(100, -9);
            oldCommitComboBox.setLayoutData(fd_oldcommitComboBox);

            fd_commitTextBox.top = new FormAttachment(oldCommitComboBox, 3, SWT.BOTTOM);
        }
        else
        {
            fd_commitTextBox.top = new FormAttachment(commitTextLabel, 3, SWT.BOTTOM);
        }
        fd_commitTextBox.left = new FormAttachment(0, 9);
        fd_commitTextBox.right = new FormAttachment(100, -9);
        fd_commitTextBox.bottom = new FormAttachment(0, 200);
        commitTextBox.getTextWidget().setLayoutData(fd_commitTextBox);

        final FormData fd_commitFilesLabel = new FormData();
        fd_commitFilesLabel.top = new FormAttachment(commitTextBox.getTextWidget(), 3);
        fd_commitFilesLabel.left = new FormAttachment(0, 9);
        fd_commitFilesLabel.right = new FormAttachment(100, -9);
        commitFilesLabel.setLayoutData(fd_commitFilesLabel);

        commitFilesList = new CommitFilesChooser(container, selectableFiles,
                this.inResources, this.root, true);

        final FormData fd_table = new FormData();
        fd_table.top = new FormAttachment(commitFilesLabel, 3);
        fd_table.left = new FormAttachment(0, 9);
        fd_table.right = new FormAttachment(100, -9);
        fd_table.bottom = new FormAttachment(100, -9);
        commitFilesList.setLayoutData(fd_table);

        setupDefaultCommitMessage();
        
        return container;
    }

    /**
     * Override the OK button pressed to capture the info we want first and then
     * call super.
     */
    @Override
    protected void okPressed() {
        resourcesToAdd = commitFilesList.getCheckedResources(FILE_UNTRACKED);
        resourcesToCommit = commitFilesList.getCheckedResources();
        resourcesToRemove = commitFilesList.getCheckedResources(FILE_DELETED);
        commitMessage = commitTextDocument.get();
        /* Store commit message in the database */
        MercurialEclipsePlugin.getCommitMessageManager().saveCommitMessage(commitMessage);
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

    public List<IResource> getResourcesToRemove() {
        return resourcesToRemove;
    }
    
    private void setupDefaultCommitMessage() {
        commitTextDocument.set(defaultCommitMessage);
        commitTextBox.setSelectedRange(0, defaultCommitMessage.length());
    }
}