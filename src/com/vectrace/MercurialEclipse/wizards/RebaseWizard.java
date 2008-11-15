/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.operations.RebaseOperation;

/**
 * @author bastian
 *
 */
public class RebaseWizard extends HgWizard {

    private IResource resource;
    private RebasePage rebasePage;

    /**
     * @param windowTitle
     */
    public RebaseWizard(IResource res) {
        super("Rebase wizard");
        this.resource = res;
        setNeedsProgressMonitor(true);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.vectrace.MercurialEclipse.wizards.HgWizard#performFinish()
     */
    @Override
    public boolean performFinish() {        
        int srcRev = -1;
        int baseRev = -1;
        int destRev = -1;
        if (rebasePage.getSourceRevCheckBox().getSelection()) {
            srcRev = rebasePage.getSrcTable().getSelection()
                    .getChangesetIndex();
        }
        if (rebasePage.getBaseRevCheckBox().getSelection()) {
            baseRev = rebasePage.getSrcTable().getSelection()
                    .getChangesetIndex();
        }
        if (rebasePage.getDestRevCheckBox().getSelection()) {
            destRev = rebasePage.getDestTable().getSelection()
                    .getChangesetIndex();
        }
        boolean collapse = rebasePage.getCollapseRevCheckBox().getSelection();
        boolean abort = rebasePage.getAbortRevCheckBox().getSelection();
        boolean cont = rebasePage.getContinueRevCheckBox().getSelection();

        RebaseOperation op = new RebaseOperation(getContainer(), resource,
                srcRev, destRev, baseRev, collapse,
                abort, cont);
        try {
            getContainer().run(true, false, op);            
            if (op.getResult().length() != 0) {
                IWorkbench workbench = PlatformUI.getWorkbench();
                Shell shell = workbench.getActiveWorkbenchWindow().getShell();
                MessageDialog.openInformation(shell, "Rebase output:", op
                        .getResult());

            }            
            return true;
        } catch (Exception e) {
            MercurialEclipsePlugin.logError(e.getCause());
            rebasePage.setErrorMessage(e.getLocalizedMessage());
            return false;
        }        
        
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.wizard.Wizard#addPages()
     */
    @Override
    public void addPages() {
        rebasePage = new RebasePage("RebasePage", "Rebase",
                MercurialEclipsePlugin
                        .getImageDescriptor("wizards/droplets-50.png"),
                "Move changeset (and descendants) to a different branch.",
                resource);

        initPage(rebasePage.getDescription(), rebasePage);
        addPage(rebasePage);        
    }

}
