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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.team.ui.TeamOperation;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgPatchClient;
import com.vectrace.MercurialEclipse.ui.LocationChooser.Location;
import com.vectrace.MercurialEclipse.ui.LocationChooser.LocationType;

public class ImportWizard extends HgWizard {

    private ImportPage sourcePage;
    private Location location;
    private IProject project;
    private String result;

    /**
     * @param root
     * @param resource
     */
    public ImportWizard(IResource selection) {
        super(Messages.getString("ImportWizard.WizardTitle")); //$NON-NLS-1$
        setNeedsProgressMonitor(true);
        project = selection.getProject();
        this.sourcePage = new ImportPage(project);
        addPage(sourcePage);
        this.initPage(Messages.getString("ImportWizard.pageDescription"), //$NON-NLS-1$
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
            location = sourcePage.getLocation();
            result = null;
            ImportOperation operation = new ImportOperation(getContainer());
            getContainer().run(true, false, operation);
            if (result != null){
                MessageDialog.openError(getShell(), Messages.getString("ImportWizard.PatchError"), //$NON-NLS-1$
                        result);
                return false;
            }
        } catch (Exception e) {
            MercurialEclipsePlugin.logError(getWindowTitle(), e);
            MercurialEclipsePlugin.showError(e.getCause());
            return false;
        }
        return true;
    }

    class ImportOperation extends TeamOperation {

        public ImportOperation(IRunnableContext context) {
            super(context);
        }

        public void run(IProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException {
            monitor.beginTask(Messages.getString("ExportWizard.pageTitle"), 1); //$NON-NLS-1$
            try {
                performOperation();
            } catch (Exception e) {
                result = e.getLocalizedMessage();
                MercurialEclipsePlugin.logError(Messages
                        .getString("ExportWizard.pageTitle") //$NON-NLS-1$
                        + " failed:", e); //$NON-NLS-1$
            } finally {
                monitor.done();
            }
        }

    }

    public void performOperation() throws Exception {
        if (location.getLocationType() == LocationType.Clipboard)
            HgPatchClient.importPatch(project);
        else
            HgPatchClient.importPatch(project, location.getFile());
        project.refreshLocal(0, null);
    }
}
