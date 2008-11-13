/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     steeven                    - implementation 
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import java.io.File;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.SaveAsDialog;

import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;

/*
 * A wizard page which will allow the user to choose location to export patch.
 * 
 */

public class ExportPage extends HgWizardPage implements Listener {

    public final static int CLIPBOARD = 1;
    public final static int FILESYSTEM = 2;
    public final static int WORKSPACE = 3;

    protected final List<IResource> resources;

    private Button btnClipboard;

    private Button btnFilesystem;
    private Text txtSystemFile;
    private Button btnBrowseFileSystem;

    private Button btnWorkspace;
    private Text txtWorkspaceFile;
    private Button btnBrowseWorkspace;

    public ExportPage(List<IResource> resources) {
        super(Messages.getString("ExportWizard.pageName"), Messages
                .getString("ExportWizard.pageTitle"), null); // TODO icon
        this.resources = resources;
        setPageComplete(false);
    }

    @Override
    public boolean isPageComplete() {
        return validatePage();
    }

    protected boolean validatePage() {
        boolean valid = false;
        switch (getLocationType()) {
        case WORKSPACE:
            valid = isValidWorkSpaceLocation(getWorkspaceFile());
            break;
        case FILESYSTEM:
            valid = isValidFile(getPatchFile());
            break;
        case CLIPBOARD:
            valid = true;
            break;
        }

        if (valid) {
            setMessage(null);
            setErrorMessage(null);
        } else {
            setErrorMessage("Please input valid file name or choose clipperboard");
        }
        // setPageComplete(valid && getSelectedResources().size() > 0);
        return valid;
    }

    public List<IResource> getSelectedResources() {
        return resources;
    }

    private boolean isValidFile(File file) {
        if (!file.isAbsolute())
            return false;
        if (file.isDirectory())
            return false;
        File parent = file.getParentFile();
        if (parent == null)
            return false;
        if (!parent.exists())
            return false;
        if (!parent.isDirectory())
            return false;
        return true;
    }

    public File getPatchFile() {
        switch (getLocationType()) {
        case FILESYSTEM:
            return btnFilesystem.getSelection() ? new File(txtSystemFile
                    .getText()) : null;
        case CLIPBOARD:
            return null;
        case WORKSPACE:
            IFile file = getWorkspaceFile();
            return file == null ? null : file.getLocation().toFile();
        default:
            return null;
        }
    }

    public IFile getWorkspaceFile() {
        if (!btnWorkspace.getSelection() || txtWorkspaceFile.getText() == null
                || txtWorkspaceFile.getText().length() == 0)
            return null;
        IPath parentToWorkspace = new Path(txtWorkspaceFile.getText());
        return ResourcesPlugin.getWorkspace().getRoot().getFile(
                parentToWorkspace);
    }

    private boolean isValidWorkSpaceLocation(IFile file) {
        return file != null && file.getParent().exists();
    }

    public int getLocationType() {
        if (btnClipboard.getSelection())
            return CLIPBOARD;
        else if (btnFilesystem.getSelection())
            return FILESYSTEM;
        else if (btnWorkspace.getSelection())
            return WORKSPACE;
        return 0;
    }

    public void createControl(Composite parent) {
        Composite composite = SWTWidgetHelper.createComposite(parent, 1);
        // TODO help
        Group group = SWTWidgetHelper.createGroup(composite, "Patch Location");
        System.out.println(group);
        createLocationControl(group);
        setControl(composite);
    }

    protected void createLocationControl(Group group) {
        Composite composite = SWTWidgetHelper.createComposite(group, 3);

        btnClipboard = SWTWidgetHelper.createRadioButton(composite,
                "&Clipboard", 3);
        btnClipboard.addListener(SWT.Selection, this);

        btnFilesystem = SWTWidgetHelper.createRadioButton(composite,
                "&File System", 1);
        btnFilesystem.addListener(SWT.Selection, this);
        txtSystemFile = SWTWidgetHelper.createTextField(composite);
        txtSystemFile.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                validatePage();
            }
        });
        txtSystemFile.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                ((Text) e.getSource()).selectAll();
            }
        });
        btnBrowseFileSystem = SWTWidgetHelper.createPushButton(composite,
                "...", 1);
        btnBrowseFileSystem.addListener(SWT.Selection, this);

        btnWorkspace = SWTWidgetHelper.createRadioButton(composite,
                "&Workspace", 1);
        btnWorkspace.addListener(SWT.Selection, this);
        txtWorkspaceFile = SWTWidgetHelper.createTextField(composite);
        txtWorkspaceFile.setEditable(false);
        btnBrowseWorkspace = SWTWidgetHelper.createPushButton(composite, "...",
                1);
        btnBrowseWorkspace.addListener(SWT.Selection, this);
    }

    public void handleEvent(Event event) {
        if (event.widget == btnBrowseFileSystem) {
            FileDialog dialog = new FileDialog(getShell(), SWT.PRIMARY_MODAL
                    | SWT.SAVE);
            dialog.setText(getTitle());
            dialog.setFileName(txtSystemFile.getText());
            String file = dialog.open();
            if (file != null)
                txtSystemFile.setText(new Path(file).toOSString());
        } else if (event.widget == btnBrowseWorkspace) {
            SaveAsDialog dialog = new SaveAsDialog(getShell());
            dialog.setOriginalFile(getWorkspaceFile());
            dialog.setTitle(getTitle());
            if (dialog.open() == Window.OK)
                txtWorkspaceFile.setText(dialog.getResult().toPortableString());
        } else if (event.widget == btnClipboard
                || event.widget == btnFilesystem
                || event.widget == btnWorkspace) {
            validatePage();
            updateBtnStatus();
        }
    }

    private void updateBtnStatus() {
        int type = getLocationType();
        txtSystemFile.setEnabled(type == FILESYSTEM);
        btnBrowseFileSystem.setEnabled(type == FILESYSTEM);
        btnBrowseWorkspace.setEnabled(type == WORKSPACE);
    }
}
