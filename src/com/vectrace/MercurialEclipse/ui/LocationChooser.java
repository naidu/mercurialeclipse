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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ResourceListSelectionDialog;
import org.eclipse.ui.dialogs.SaveAsDialog;

import com.vectrace.MercurialEclipse.wizards.Messages;

/**
 * control for choose location: clipboard, file or workspace file
 * 
 * @author Administrator
 * 
 */
public class LocationChooser extends Composite implements Listener {

    public enum LocationType {
        Clipboard, FileSystem, Workspace
    }

    private Button btnClipboard;

    private Button btnFilesystem;
    private Text txtSystemFile;
    private Button btnBrowseFileSystem;

    private Button btnWorkspace;
    private Text txtWorkspaceFile;
    private Button btnBrowseWorkspace;

    private final boolean save;

    private ListenerList stateListeners = new ListenerList();

    private IDialogSettings settings;

    public LocationChooser(Composite parent, boolean save,
            IDialogSettings settings) {
        super(parent, SWT.None);
        GridLayout layout = new GridLayout();
        layout.numColumns = 3;
        setLayout(layout);
        createLocationControl();
        this.save = save;
        this.settings = settings;
        restoreSettings();
    }

    protected void createLocationControl() {
        btnClipboard = SWTWidgetHelper.createRadioButton(this, Messages
                .getString("ExportWizard.Clipboard"), 3); //$NON-NLS-1$
        btnClipboard.addListener(SWT.Selection, this);

        btnFilesystem = SWTWidgetHelper.createRadioButton(this, Messages
                .getString("ExportWizard.FileSystem"), //$NON-NLS-1$
                1);
        btnFilesystem.addListener(SWT.Selection, this);
        txtSystemFile = SWTWidgetHelper.createTextField(this);
        txtSystemFile.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                fireStateChanged();
            }
        });
        txtSystemFile.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                ((Text) e.getSource()).selectAll();
            }
        });
        btnBrowseFileSystem = SWTWidgetHelper.createPushButton(this, "...", 1); //$NON-NLS-1$
        btnBrowseFileSystem.addListener(SWT.Selection, this);

        btnWorkspace = SWTWidgetHelper.createRadioButton(this, Messages
                .getString("ExportWizard.Workspace"), 1); //$NON-NLS-1$
        btnWorkspace.addListener(SWT.Selection, this);
        txtWorkspaceFile = SWTWidgetHelper.createTextField(this);
        txtWorkspaceFile.setEditable(false);
        btnBrowseWorkspace = SWTWidgetHelper.createPushButton(this, "...", 1); //$NON-NLS-1$
        btnBrowseWorkspace.addListener(SWT.Selection, this);
    }

    public void handleEvent(Event event) {
        if (event.widget == btnBrowseFileSystem) {
            FileDialog dialog = new FileDialog(getDisplay().getActiveShell(),
                    SWT.PRIMARY_MODAL | (save ? SWT.SAVE : SWT.OPEN));
            // dialog.setText("Choose file to save");
            dialog.setFileName(txtSystemFile.getText());
            String file = dialog.open();
            if (file != null)
                txtSystemFile.setText(new Path(file).toOSString());
        } else if (event.widget == btnBrowseWorkspace) {
            if (save) {
                SaveAsDialog dialog = new SaveAsDialog(getDisplay()
                        .getActiveShell());
                dialog.setOriginalFile(getWorkspaceFile());
                // dialog.setText(txtWorkspaceFile.getText());
                // dialog.setTitle(getTitle());
                if (dialog.open() == Window.OK)
                    txtWorkspaceFile.setText(dialog.getResult().toString());
            } else {
                ResourceListSelectionDialog dialog = new ResourceListSelectionDialog(
                        getShell(), null, 0);
                List<String> list = new ArrayList<String>(1);
                list.add(txtWorkspaceFile.getText());
                dialog.setInitialElementSelections(list);
                Object[] result = dialog.getResult();
                if (result != null && result.length > 0)
                    txtWorkspaceFile.setText(result[0].toString());
            }
        } else if (event.widget == btnClipboard
                || event.widget == btnFilesystem
                || event.widget == btnWorkspace) {
            updateBtnStatus();
        }
        fireStateChanged();
    }

    public String validate() {
        boolean valid = false;
        LocationType type = getLocationType();
        if (type == null)
            return null;
        switch (type) {
        case Workspace:
            // valid = isValidWorkSpaceLocation(getWorkspaceFile());
            // break;
        case FileSystem:
            valid = isValidSystemFile(getPatchFile());
            break;
        case Clipboard:
            valid = validateClipboard();
            break;
        }
        if (valid)
            return null;
        return Messages.getString("ExportWizard.InvalidFileName"); //$NON-NLS-1$
    }

    private boolean validateClipboard() {
        if (save)
            return true;
        Clipboard cb = new Clipboard(getDisplay());
        String contents = (String) cb.getContents(TextTransfer.getInstance());
        cb.dispose();
        return contents != null && contents.trim().length() > 0;
    }

    private boolean isValidSystemFile(File file) {
        if (file == null)
            return false;
        if (!file.isAbsolute())
            return false;
        if (file.isDirectory())
            return false;
        if (save) {
            File parent = file.getParentFile();
            if (parent == null)
                return false;
            if (!parent.exists())
                return false;
            if (!parent.isDirectory())
                return false;
        } else {
            if (file.exists())
                return false;
        }
        return true;
    }

    public File getPatchFile() {
        switch (getLocationType()) {
        case FileSystem:
            return btnFilesystem.getSelection() ? new File(txtSystemFile
                    .getText()) : null;
        case Clipboard:
            return null;
        case Workspace:
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

    // private boolean isValidWorkSpaceLocation(IFile file) {
    // if (save)
    // return file != null && file.getParent().exists();
    // return file != null && file.exists();
    // }

    public LocationType getLocationType() {
        if (btnClipboard.getSelection())
            return LocationType.Clipboard;
        else if (btnFilesystem.getSelection())
            return LocationType.FileSystem;
        else if (btnWorkspace.getSelection())
            return LocationType.Workspace;
        return null;
    }

    private void updateBtnStatus() {
        LocationType type = getLocationType();
        txtSystemFile.setEnabled(type == LocationType.FileSystem);
        btnBrowseFileSystem.setEnabled(type == LocationType.FileSystem);
        btnBrowseWorkspace.setEnabled(type == LocationType.Workspace);
    }

    /**
     * @param exportPage
     */
    public void addStateListener(Listener listener) {
        stateListeners.add(listener);
    }

    protected void fireStateChanged() {
        for (Object obj : stateListeners.getListeners())
            ((Listener) obj).handleEvent(null);
    }

    /**
     * @return
     */
    public Location getCheckedLocation() {
        return new Location(getLocationType(), getPatchFile(),
                getWorkspaceFile());
    }

    public static class Location {

        private final LocationType locationType;

        /**
         * @return the locationType
         */
        public LocationType getLocationType() {
            return locationType;
        }

        /**
         * @return the file
         */
        public File getFile() {
            return file;
        }

        /**
         * @return the workspaceFile
         */
        public IFile getWorkspaceFile() {
            return workspaceFile;
        }

        private final File file;
        private final IFile workspaceFile;

        public Location(LocationType locationType, File file,
                IFile workspaceFile) {
            this.locationType = locationType;
            this.file = file;
            this.workspaceFile = workspaceFile;
        }

    }

    protected void restoreSettings() {
        if (settings == null)
            return;
        String val = settings.get("LocationType"); //$NON-NLS-1$
        if (val != null)
            setLocationType(LocationType.valueOf(val));
    }

    public void saveSettings() {
        if (settings == null)
            return;
        settings.put("LocationType", getLocationType().name()); //$NON-NLS-1$
    }

    private void setLocationType(LocationType type) {
        switch (type) {
        case Clipboard:
            btnClipboard.setSelection(true);
            break;
        case FileSystem:
            btnFilesystem.setSelection(true);
            break;
        case Workspace:
            btnWorkspace.setSelection(true);
            break;
        }
        updateBtnStatus();
    }
}
