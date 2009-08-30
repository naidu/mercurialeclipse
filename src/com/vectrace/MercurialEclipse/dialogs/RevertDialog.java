/*******************************************************************************
 * Copyright (c) 2007-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     StefanC - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.dialogs;

import static com.vectrace.MercurialEclipse.ui.SWTWidgetHelper.getFillGD;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.vectrace.MercurialEclipse.ui.CommitFilesChooser;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;

public class RevertDialog extends TitleAreaDialog {

    private List<IResource> resources;
    private CommitFilesChooser selectFilesList;
    private List<IResource> selection;

    public static final String FILE_MODIFIED = Messages.getString("CommitDialog.modified"); //$NON-NLS-1$
    public static final String FILE_ADDED = Messages.getString("CommitDialog.added"); //$NON-NLS-1$
    public static final String FILE_REMOVED = Messages.getString("CommitDialog.removed"); //$NON-NLS-1$
    public static final String FILE_UNTRACKED = Messages.getString("CommitDialog.untracked"); //$NON-NLS-1$
    public static final String FILE_DELETED = Messages.getString("CommitDialog.deletedInWorkspace"); //$NON-NLS-1$

    /**
     * Create the dialog
     *
     * @param parentShell
     */
    public RevertDialog(Shell parentShell) {
        super(parentShell);
        setShellStyle(getShellStyle() | SWT.RESIZE);
    }

    /**
     * Create contents of the dialog
     *
     * @param parent
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = SWTWidgetHelper.createComposite(parent, 1);
        GridData gd = getFillGD(400);
        container.setLayoutData(gd);
        super.createDialogArea(parent);
        createFilesList(container);
        setTitle(Messages.getString("RevertDialog.title")); //$NON-NLS-1$
        setMessage(Messages.getString("RevertDialog.message")); //$NON-NLS-1$
        return container;
    }

    private void createFilesList(Composite container) {
        selectFilesList = new CommitFilesChooser(container, true, resources, false, true);
    }

    public void setFiles(List<IResource> resources) {
        this.resources = resources;

    }

    @Override
    protected void okPressed() {
        this.selection = selectFilesList.getCheckedResources(FILE_ADDED, FILE_DELETED, FILE_MODIFIED, FILE_REMOVED);
        super.okPressed();

    }

    public void setFiles(IResource[] commitResources) {
        setFiles(Arrays.asList(commitResources));
    }

    public List<IResource> getSelection() {
        return selection;
    }
}
