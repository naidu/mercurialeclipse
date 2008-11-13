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

import java.util.List;

import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgImportExportClient;

public class ExportWizard extends HgWizard {

    private ExportPage sourcePage;

    /**
     * @param resource
     */
    public ExportWizard(List<IResource> resources) {
        super(Messages.getString("ExportWizard.WindowTitle")); //$NON-NLS-1$
        setNeedsProgressMonitor(true);
        this.sourcePage = new ExportPage(resources); //$NON-NLS-1$ //$NON-NLS-2$ 
        addPage(sourcePage);
        this.initPage(Messages.getString("ExportWizard.pageDescription"),
                sourcePage);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.wizard.Wizard#performFinish()
     */
    @Override
    public boolean performFinish() {
        try {
            if (sourcePage.getLocationType() == ExportPage.CLIPBOARD)
                HgImportExportClient.exportPatch(sourcePage
                        .getSelectedResources());
            else
                HgImportExportClient.exportPatch(sourcePage
                        .getSelectedResources(), sourcePage.getPatchFile());
            if (sourcePage.getLocationType() == ExportPage.WORKSPACE)
                sourcePage.getWorkspaceFile().refreshLocal(0, null);// TODO
            // progress
        } catch (Exception e) {
            MercurialEclipsePlugin.logError(Messages
                    .getString("ExportWizard.exportOperationFailed"), e); //$NON-NLS-1$
        }
        return true;
    }
}
