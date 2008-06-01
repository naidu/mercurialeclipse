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
package com.vectrace.MercurialEclipse.wizards.mq;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.commands.mq.HgQNewClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.wizards.HgWizard;

/**
 * @author bastian
 * 
 */
public class QNewWizard extends HgWizard {
    private QNewWizardPage page = null;

    private class NewOperation extends HgOperation {

        /**
         * @param context
         */
        public NewOperation(IRunnableContext context) {
            super(context);
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.vectrace.MercurialEclipse.actions.HgOperation#getActionDescription()
         */
        @Override
        protected String getActionDescription() {
            return Messages.getString("QNewWizard.actionDescription"); //$NON-NLS-1$
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.vectrace.MercurialEclipse.actions.HgOperation#run(org.eclipse.core.runtime.IProgressMonitor)
         */
        @Override
        public void run(IProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException {
            monitor.beginTask(Messages.getString("QNewWizard.beginTask"), 2); //$NON-NLS-1$
            monitor.worked(1);
            monitor.subTask(Messages.getString("QNewWizard.subTask.callMercurial")); //$NON-NLS-1$

            try {
                HgQNewClient.createNewPatch(resource, page
                        .getCommitMessageTextField().getText(), page
                        .getForceCheckBox().getSelection(), page
                        .getGitCheckBox().getSelection(), page
                        .getIncludeTextField().getText(), page
                        .getExcludeTextField().getText(), page
                        .getUserTextField().getText(),
                        page.getDate().getText(), page.getPatchNameTextField()
                                .getText());
                monitor.worked(1);
                monitor.done();
            } catch (HgException e) {
                throw new InvocationTargetException(e, e.getLocalizedMessage());
            }
        }

    }

    private IResource resource;

    /**
     * @param windowTitle
     */
    public QNewWizard(IResource resource) {
        super(Messages.getString("QNewWizard.title")); //$NON-NLS-1$
        this.resource = resource;
        setNeedsProgressMonitor(true);
        page = new QNewWizardPage(Messages.getString("QNewWizard.pageName"), Messages.getString("QNewWizard.pageTitle"), null, null, //$NON-NLS-1$ //$NON-NLS-2$
                resource, true);
        initPage(Messages.getString("QNewWizard.pageDescription"), //$NON-NLS-1$
                page);
        addPage(page);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vectrace.MercurialEclipse.wizards.HgWizard#performFinish()
     */
    @Override
    public boolean performFinish() {
        NewOperation initOperation = new NewOperation(getContainer());
        try {
            getContainer().run(false, false, initOperation);
        } catch (Exception e) {
            MercurialEclipsePlugin.logError(e);
            page.setErrorMessage(e.getLocalizedMessage());
            return false;
        }
        return true;
    }

}
