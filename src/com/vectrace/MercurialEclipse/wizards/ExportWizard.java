/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Stefan Groschupf          - logError
 *     Stefan C                  - Code cleanup
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.team.ui.TeamOperation;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgImportExportClient;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.ui.LocationChooser.Location;
import com.vectrace.MercurialEclipse.ui.LocationChooser.LocationType;

public class ExportWizard extends HgWizard {

    private ExportPage sourcePage;
    private ArrayList<IResource> resources;
    private Location location;

    /**
     * @param root
     * @param resource
     */
    public ExportWizard(List<IResource> resources, HgRoot root) {
        super(Messages.getString("ExportWizard.WindowTitle")); //$NON-NLS-1$
        setNeedsProgressMonitor(true);
        this.sourcePage = new ExportPage(resources, root);
        addPage(sourcePage);
        this.initPage(Messages.getString("ExportWizard.pageDescription"), //$NON-NLS-1$
                sourcePage);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.wizard.Wizard#performFinish()
     */
    @Override
    public boolean performFinish() {
        sourcePage.finish(null);
        try {
            resources = sourcePage.getCheckedResources();
            location = sourcePage.getLocation();
            if (location.getLocationType() != LocationType.Clipboard
                    && location.getFile().exists())
                if (!MessageDialog
                        .openConfirm(
                                getShell(),
                                Messages
                                        .getString("ExportWizard.OverwriteConfirmTitle"), //$NON-NLS-1$
                                Messages
                                        .getString("ExportWizard.OverwriteConfirmDescription"))) //$NON-NLS-1$
                    return false;
            ExportOperation operation = new ExportOperation(getContainer());
            getContainer().run(true, false, operation);
            if (location.getLocationType() == LocationType.Clipboard) {
                Clipboard cb = new Clipboard(getContainer().getShell()
                        .getDisplay());
                cb.setContents(new Object[] { operation.result },
                        new Transfer[] { TextTransfer.getInstance() });
                cb.dispose();
            }
        } catch (Exception e) {
            MercurialEclipsePlugin.logError(getWindowTitle(), e);
            MercurialEclipsePlugin.showError(e.getCause());
            return false;
        }
        return true;
    }

    class ExportOperation extends TeamOperation {

        public String result;

        public ExportOperation(IRunnableContext context) {
            super(context);
        }

        public void run(IProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException {
            monitor.beginTask(Messages.getString("ExportWizard.pageTitle"), 1); //$NON-NLS-1$
            try {
                result = doExport();
            } catch (Exception e) {
                MercurialEclipsePlugin.logError(Messages
                        .getString("ExportWizard.pageTitle") //$NON-NLS-1$
                        + " failed:", e); //$NON-NLS-1$
            } finally {
                monitor.done();
            }
        }

    }

    public String doExport() throws Exception {
        if (location.getLocationType() == LocationType.Clipboard)
            return HgImportExportClient.exportPatch(resources);
        HgImportExportClient.exportPatch(resources, location.getFile());
        if (location.getLocationType() == LocationType.Workspace)
            location.getWorkspaceFile().refreshLocal(0, null);
        return null;
    }
}
