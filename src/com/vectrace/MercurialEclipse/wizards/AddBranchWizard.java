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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.commands.HgBranchClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

/**
 * @author bastian
 * 
 */
public class AddBranchWizard extends HgWizard {
    private AddBranchPage branchPage;
    private IResource resource;

    private class AddBranchOperation extends HgOperation {

        /**
         * @param context
         */
        public AddBranchOperation(IRunnableContext context) {
            super(context);
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.vectrace.MercurialEclipse.actions.HgOperation#getActionDescription()
         */
        @Override
        protected String getActionDescription() {
            return Messages.getString("AddBranchWizard.AddBranchOperation.actionDescription"); //$NON-NLS-1$
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.vectrace.MercurialEclipse.actions.HgOperation#run(org.eclipse.core.runtime.IProgressMonitor)
         */
        @Override
        public void run(IProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException {
            try {
                monitor.beginTask(Messages.getString("AddBranchWizard.AddBranchOperation.taskName"), 1); //$NON-NLS-1$
                String result = HgBranchClient.addBranch(resource, branchPage
                        .getBranchNameTextField().getText(), MercurialUtilities
                        .getHGUsername(), branchPage.getForceCheckBox()
                        .getSelection());
                monitor.worked(1);                
                MessageDialog.openInformation(getShell(), Messages.getString("AddBranchWizard.AddBranchOperation.output.title"), //$NON-NLS-1$
                        result);
            } catch (HgException e) {
                MercurialEclipsePlugin.logError(e);
                throw new InvocationTargetException(e, e.getLocalizedMessage());
            }
            monitor.done();
        }
    }

    /**
     * @param windowtitle
     */
    public AddBranchWizard(IResource resource) {
        super(Messages.getString("AddBranchWizard.windowTitle")); //$NON-NLS-1$
        this.resource = resource;
        setNeedsProgressMonitor(true);
        branchPage = new AddBranchPage(Messages.getString("AddBranchWizard.branchPage.name"), //$NON-NLS-1$
                Messages.getString("AddBranchWizard.branchPage.title"), MercurialEclipsePlugin.getImageDescriptor("wizards/newstream_wizban.gif"), //$NON-NLS-1$
                Messages.getString("AddBranchWizard.branchPage.description")); //$NON-NLS-1$
        addPage(branchPage);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vectrace.MercurialEclipse.wizards.HgWizard#performFinish()
     */
    @Override
    public boolean performFinish() {
        AddBranchOperation op = new AddBranchOperation(getContainer());
        try {
            getContainer().run(true, false, op);
        } catch (Exception e) {
            branchPage.setErrorMessage(e.getLocalizedMessage());
            return false;
        }
        return super.performFinish();
    }
}
