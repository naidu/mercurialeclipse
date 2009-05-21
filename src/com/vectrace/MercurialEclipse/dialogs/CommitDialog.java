/*******************************************************************************
 * Copyright (c) 2006-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eclipse.org - see CommitWizardCommitPage
 *     Software Balm Consulting Inc (Peter Hunnisett <peter_hge at softwarebalm dot com>) - implementation
 *     Bastian Doetsch - Added spellchecking and some other stuff
 *     StefanC - many updates
 *     Zingo Andersen - some updates
 *******************************************************************************/
package com.vectrace.MercurialEclipse.dialogs;

import static com.vectrace.MercurialEclipse.ui.SWTWidgetHelper.getFillGD;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSet;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSetManager;
import org.eclipse.team.internal.core.subscribers.ChangeSet;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.eclipse.ui.texteditor.AnnotationPreference;
import org.eclipse.ui.texteditor.DefaultMarkerAnnotationAccess;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
import org.eclipse.ui.texteditor.spelling.SpellingAnnotation;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeWorkspaceJob;
import com.vectrace.MercurialEclipse.commands.HgAddClient;
import com.vectrace.MercurialEclipse.commands.HgClients;
import com.vectrace.MercurialEclipse.commands.HgCommitClient;
import com.vectrace.MercurialEclipse.commands.HgRemoveClient;
import com.vectrace.MercurialEclipse.menu.CommitMergeHandler;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.ActionRevert;
import com.vectrace.MercurialEclipse.team.cache.RefreshJob;
import com.vectrace.MercurialEclipse.ui.CommitFilesChooser;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;

/**
 * 
 * A commit dialog box allowing choosing of what files to commit and a commit message for those files. Untracked files
 * may also be chosen.
 * 
 */
public class CommitDialog extends TitleAreaDialog {
    public static final String FILE_MODIFIED = Messages.getString("CommitDialog.modified"); //$NON-NLS-1$
    public static final String FILE_ADDED = Messages.getString("CommitDialog.added"); //$NON-NLS-1$
    public static final String FILE_REMOVED = Messages.getString("CommitDialog.removed"); //$NON-NLS-1$
    public static final String FILE_UNTRACKED = Messages.getString("CommitDialog.untracked"); //$NON-NLS-1$
    public static final String FILE_DELETED = Messages.getString("CommitDialog.deletedInWorkspace"); //$NON-NLS-1$

    private String defaultCommitMessage = Messages.getString("CommitDialog.defaultCommitMessage"); //$NON-NLS-1$
    private Combo oldCommitComboBox;
    private ISourceViewer commitTextBox;
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
    private Text userTextField;
    private String user;
    private Button revertCheckBox;
    private ActiveChangeSetManager csManager = MercurialEclipsePlugin.getDefault().getChangeSetManager();

    /**
     * @param shell
     */
    public CommitDialog(Shell shell, HgRoot root, List<IResource> resources) {
        super(shell);
        setShellStyle(getShellStyle() | SWT.RESIZE | SWT.TITLE);
        setBlockOnOpen(false);
        this.root = root;
        this.inResources = resources;
        this.selectableFiles = true;
        this.commitTextDocument = new Document();
    }

    public CommitDialog(Shell shell, HgRoot root, ArrayList<IResource> selectedResource, String defaultCommitMessage,
            boolean selectableFiles) {
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

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse .swt.widgets.Composite)
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = SWTWidgetHelper.createComposite(parent, 1);
        GridData gd = getFillGD(400);
        gd.minimumWidth = 500;
        container.setLayoutData(gd);
        super.createDialogArea(parent);

        container.addKeyListener(new KeyListener() {
            public void keyReleased(KeyEvent e) {
                if (e.character == SWT.CR && e.stateMask == SWT.MOD1) {
                    okPressed();
                    e.doit = false;
                }
            }

            public void keyPressed(KeyEvent e) {
            }
        });

        createCommitTextBox(container);
        createOldCommitCombo(container);
        createUserCommitCombo(container);
        createFilesList(container);
        createRevertCheckBox(container);
        setupDefaultCommitMessage();

        commitTextBox.getTextWidget().setFocus();
        setTitle(Messages.getString("CommitDialog.title")); //$NON-NLS-1$");
        setMessage(Messages.getString("CommitDialog.message")); //$NON-NLS-1$");
        return container;
    }

    /**
     * @param container
     */
    private void createRevertCheckBox(Composite container) {
        this.revertCheckBox = SWTWidgetHelper.createCheckBox(container, Messages.getString("CommitDialog.revertCheckBoxLabel.revertUncheckedResources")); //$NON-NLS-1$
    }

    /**
     * @param container
     */
    private void createFilesList(Composite container) {
        SWTWidgetHelper.createLabel(container, Messages.getString("CommitDialog.selectFiles")); //$NON-NLS-1$
        commitFilesList = new CommitFilesChooser(container, selectableFiles, this.inResources, this.root, true, true);
    }

    /**
     * @param container
     */
    private void createUserCommitCombo(Composite container) {
        Composite comp = SWTWidgetHelper.createComposite(container, 2);
        SWTWidgetHelper.createLabel(comp, Messages.getString("CommitDialog.userLabel.text")); //$NON-NLS-1$
        this.userTextField = SWTWidgetHelper.createTextField(comp);
        if (user == null || user.length() == 0) {
            user = HgClients.getDefaultUserName();
        }
        this.userTextField.setText(user);
    }

    /**
     * @param container
     */
    private void createCommitTextBox(Composite container) {
        setMessage(Messages.getString("CommitDialog.commitTextLabel.text")); //$NON-NLS-1$

        commitTextBox = new SourceViewer(container, null, SWT.V_SCROLL | SWT.MULTI | SWT.BORDER | SWT.WRAP);
        commitTextBox.setEditable(true);
        commitTextBox.getTextWidget().setLayoutData(getFillGD(150));

        // set up spell-check annotations
        decorationSupport = new SourceViewerDecorationSupport(commitTextBox, null, new DefaultMarkerAnnotationAccess(),
                EditorsUI.getSharedTextColors());

        AnnotationPreference pref = EditorsUI.getAnnotationPreferenceLookup().getAnnotationPreference(
                SpellingAnnotation.TYPE);

        decorationSupport.setAnnotationPreference(pref);
        decorationSupport.install(EditorsUI.getPreferenceStore());

        commitTextBox.configure(new TextSourceViewerConfiguration(EditorsUI.getPreferenceStore()));
        AnnotationModel annotationModel = new AnnotationModel();
        commitTextBox.setDocument(commitTextDocument, annotationModel);
        commitTextBox.getTextWidget().addDisposeListener(new DisposeListener() {

            public void widgetDisposed(DisposeEvent e) {
                decorationSupport.uninstall();
            }

        });
        
        commitTextBox.getTextWidget().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseUp(MouseEvent e) {
                if (commitTextDocument.get().equals(defaultCommitMessage)) {
                    commitTextBox.setSelectedRange(0, defaultCommitMessage.length());
                }
            }
        });
    }

    /**
     * @param container
     */
    private void createOldCommitCombo(Composite container) {
        final String oldCommits[] = MercurialEclipsePlugin.getCommitMessageManager().getCommitMessages();
        if (oldCommits.length > 0) {
            oldCommitComboBox = SWTWidgetHelper.createCombo(container);
            oldCommitComboBox.add(Messages.getString("CommitDialog.oldCommitMessages")); //$NON-NLS-1$ 
            oldCommitComboBox.setText(Messages.getString("CommitDialog.oldCommitMessages")); //$NON-NLS-1$
            for (int i = 0; i < oldCommits.length; i++) {
                /*
                 * Add text to the combo but replace \n with <br> to get a one-liner
                 */
                oldCommitComboBox.add(oldCommits[i].replaceAll("\\n", "<br>")); //$NON-NLS-1$ //$NON-NLS-2$
            }
            oldCommitComboBox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (oldCommitComboBox.getSelectionIndex() != 0) {
                        commitTextDocument.set(oldCommits[oldCommitComboBox.getSelectionIndex() - 1]);
                        commitTextBox.setSelectedRange(0, oldCommits[oldCommitComboBox.getSelectionIndex() - 1]
                                .length());
                    }

                }
            });

        }
    }

    /**
     * Override the OK button pressed to capture the info we want first and then call super.
     */
    @Override
    protected void okPressed() {
        try {
            resourcesToAdd = commitFilesList.getCheckedResources(FILE_UNTRACKED);
            resourcesToCommit = commitFilesList.getCheckedResources();
            resourcesToRemove = commitFilesList.getCheckedResources(FILE_DELETED);
            commitMessage = commitTextDocument.get();
            /* Store commit message in the database if not the default message */
            if (!commitMessage.equals(defaultCommitMessage)) {
                MercurialEclipsePlugin.getCommitMessageManager().saveCommitMessage(commitMessage);
            }
            this.user = userTextField.getText();

            // add new resources
            HgAddClient.addResources(resourcesToAdd, null);

            // remove deleted resources
            HgRemoveClient.removeResources(resourcesToRemove);

            // commit all
            String messageToCommit = this.getCommitMessage();
            if (user == null || user.length() == 0) {
                user = HgClients.getDefaultUserName();
            }

            if (!selectableFiles) {
                // commit merge
                CommitMergeHandler.commitMerge(inResources.get(0), messageToCommit);
            } else {
                HgCommitClient.commitResources(resourcesToCommit, user, messageToCommit, new NullProgressMonitor());
            }

            if (inResources.size() > 0) {
                new RefreshJob(Messages.getString("CommitDialog.refreshing"), null, //$NON-NLS-1$
                        inResources.get(0).getProject()).schedule();
            }

            if (revertCheckBox.getSelection()) {
                final List<IResource> revertResources = commitFilesList.getUncheckedResources(FILE_ADDED, FILE_DELETED,
                        FILE_MODIFIED, FILE_REMOVED);
                new SafeWorkspaceJob(Messages.getString("CommitDialog.revertJob.RevertingFiles")) { //$NON-NLS-1$
                    /*
                     * (non-Javadoc)
                     * 
                     * @see com.vectrace.MercurialEclipse.SafeWorkspaceJob#runSafe
                     * (org.eclipse.core.runtime.IProgressMonitor)
                     */
                    @Override
                    protected IStatus runSafe(IProgressMonitor monitor) {
                        ActionRevert action = new ActionRevert();
                        action.doRevert(monitor, revertResources);
                        return super.runSafe(monitor);
                    }
                }.schedule();

            }
            super.okPressed();
        } catch (CoreException e) {
            MercurialEclipsePlugin.logError(e);
            setErrorMessage(e.getLocalizedMessage());
        }

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
        String msg = getProposedComment(inResources.toArray(new IResource[inResources.size()]));
        if (msg != null && msg.length() > 0) {
            commitTextDocument.set(msg);
        } else {
            commitTextDocument.set(defaultCommitMessage);
            commitTextBox.setSelectedRange(0, defaultCommitMessage.length());    
        }
    }

    /**
     * @return the user
     */
    public String getUser() {
        return user;
    }
    
    /*
     * Get a proposed comment by looking at the active change sets
     */
    private String getProposedComment(IResource[] resourcesToCommit) {
        StringBuffer comment = new StringBuffer();
        ChangeSet[] sets = csManager.getSets();
      
        int numMatchedSets = 0;
        for (int i = 0; i < sets.length; i++) {
            ChangeSet set = sets[i];
            if (isUserSet(set) && containsOne(set, resourcesToCommit)) {
                if (numMatchedSets > 0) {
                    comment.append(System.getProperty("line.separator")); //$NON-NLS-1$
                }
                comment.append(set.getComment());
                numMatchedSets++;
            }
        }
        return comment.toString();
    }

    private boolean isUserSet(ChangeSet set) {
        if (set instanceof ActiveChangeSet) {
            ActiveChangeSet acs = (ActiveChangeSet) set;
            return acs.isUserCreated();
        }
        return false;
    }

    private boolean containsOne(ChangeSet set, IResource[] resourcesToCommit) {
        for (int j = 0; j < resourcesToCommit.length; j++) {
            IResource resource = resourcesToCommit[j];
            if (set.contains(resource)) {
                return true;
            }
            if (set instanceof ActiveChangeSet) {
                ActiveChangeSet acs = (ActiveChangeSet) set;
                if (acs.getDiffTree().members(resource).length > 0) {
                    return true;
                }
            }
        }
        return false;
    }

}