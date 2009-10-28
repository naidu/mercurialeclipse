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
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
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
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.mylyn.MylynFacadeFactory;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.ActionRevert;
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
    public static final String FILE_CLEAN = Messages.getString("CommitDialog.clean"); //$NON-NLS-1$

    protected String defaultCommitMessage = Messages.getString("CommitDialog.defaultCommitMessage"); //$NON-NLS-1$
    private Combo oldCommitComboBox;
    private ISourceViewer commitTextBox;
    protected CommitFilesChooser commitFilesList;
    private List<IResource> resourcesToAdd;
    private List<IResource> resourcesToCommit;
    private List<IResource> resourcesToRemove;
    private String commitMessage;
    private final IDocument commitTextDocument;
    private SourceViewerDecorationSupport decorationSupport;
    private final List<IResource> inResources;
    private Text userTextField;
    private String user;
    private Button revertCheckBox;

    public CommitDialog(Shell shell, List<IResource> resources) {
        super(shell);
        setShellStyle(getShellStyle() | SWT.RESIZE | SWT.TITLE);
        setBlockOnOpen(false);
        inResources = resources;
        commitTextDocument = new Document();
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

        final String initialCommitMessage = MylynFacadeFactory.getMylynFacade().getCurrentTaskComment(inResources == null ? null : inResources.toArray(new IResource[0]));
        setCommitMessage(initialCommitMessage);

        commitTextBox.getTextWidget().setFocus();
        setTitle(Messages.getString("CommitDialog.title")); //$NON-NLS-1$");
        setMessage(Messages.getString("CommitDialog.message")); //$NON-NLS-1$");
        return container;
    }

    protected void createRevertCheckBox(Composite container) {
        revertCheckBox = SWTWidgetHelper.createCheckBox(container, Messages.getString("CommitDialog.revertCheckBoxLabel.revertUncheckedResources")); //$NON-NLS-1$
    }

    protected void createFilesList(Composite container) {
        SWTWidgetHelper.createLabel(container, Messages.getString("CommitDialog.selectFiles")); //$NON-NLS-1$
        commitFilesList = new CommitFilesChooser(container, true, inResources, true, true);

        IResource[] mylynTaskResources = MylynFacadeFactory.getMylynFacade().getCurrentTaskResources();
        if (mylynTaskResources != null) {
            commitFilesList.setSelectedResources(Arrays.asList(mylynTaskResources));
        }
    }

    private void createUserCommitCombo(Composite container) {
        Composite comp = SWTWidgetHelper.createComposite(container, 2);
        SWTWidgetHelper.createLabel(comp, Messages.getString("CommitDialog.userLabel.text")); //$NON-NLS-1$
        this.userTextField = SWTWidgetHelper.createTextField(comp);
        // TODO provide an option to either use default commit name OR project specific one
        // See issue #10240: Wrong author is used in synchronization commit message
//        if (user == null || user.length() == 0) {
//            user = getInitialCommitUserName();
//        }
        if (user == null || user.length() == 0) {
            user = HgClients.getDefaultUserName();
        }
        this.userTextField.setText(user);
    }

    protected String getInitialCommitUserName() {
        if(inResources.isEmpty()){
            return null;
        }
        IProject project = inResources.get(0).getProject();
        return getDefaultCommitName(project);
    }

    protected static String getDefaultCommitName(IProject project) {
        // TODO see issue 10150: get the name from project properties, not from repo
        // but for now it will at least work for projects with one repo
        HgRepositoryLocation repoLocation = MercurialEclipsePlugin.getRepoManager()
                .getDefaultProjectRepoLocation(project);
        if(repoLocation == null){
            return null;
        }
        return repoLocation.getUser();
    }

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
            String messageToCommit = getCommitMessage();
            if (user == null || user.length() == 0) {
                user = getInitialCommitUserName();
            }

            performCommit(messageToCommit);

            // revertCheckBox can be null if this is a merge dialog
            if (revertCheckBox != null && revertCheckBox.getSelection()) {
                revertResources();
            }
            super.okPressed();
        } catch (CoreException e) {
            MercurialEclipsePlugin.logError(e);
            setErrorMessage(e.getLocalizedMessage());
        }
    }

    protected void performCommit(String messageToCommit) throws CoreException {
        HgCommitClient.commitResources(resourcesToCommit, user, messageToCommit, new NullProgressMonitor());
    }

    private void revertResources() {
        final List<IResource> revertResources = commitFilesList.getUncheckedResources(FILE_ADDED, FILE_DELETED,
                FILE_MODIFIED, FILE_REMOVED);
        new SafeWorkspaceJob(Messages.getString("CommitDialog.revertJob.RevertingFiles")) { //$NON-NLS-1$
            @Override
            protected IStatus runSafe(IProgressMonitor monitor) {
                ActionRevert action = new ActionRevert();
                try {
                    action.doRevert(monitor, revertResources, new ArrayList<IResource>(), false);
                } catch (HgException e) {
                    MercurialEclipsePlugin.logError(e);
                    return e.getStatus();
                }
                return super.runSafe(monitor);
            }
        }.schedule();
    }

    public List<IResource> getResourcesToRemove() {
        return resourcesToRemove;
    }

    private void setCommitMessage(String msg) {
        if (msg == null) {
            msg = defaultCommitMessage;
        }
        commitTextDocument.set(msg);
        commitTextBox.setSelectedRange(0, msg.length());
    }

    public String getUser() {
        return user;
    }

}