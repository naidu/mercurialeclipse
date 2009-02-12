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
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.commands.extensions.mq.HgQDeleteClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.Patch;
import com.vectrace.MercurialEclipse.views.PatchQueueView;
import com.vectrace.MercurialEclipse.wizards.HgWizard;

/**
 * @author bastian
 * 
 */
public class QDeleteWizard extends HgWizard {
    private QDeletePage page = null;

    private class DeleteOperation extends HgOperation {

        /**
         * @param context
         */
        public DeleteOperation(IRunnableContext context) {
            super(context);
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.vectrace.MercurialEclipse.actions.HgOperation#getActionDescription()
         */
        @Override
        protected String getActionDescription() {
            return Messages.getString("QDeleteWizard.deleteAction.description"); //$NON-NLS-1$
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.vectrace.MercurialEclipse.actions.HgOperation#run(org.eclipse.core.runtime.IProgressMonitor)
         */
        @SuppressWarnings("unchecked") //$NON-NLS-1$
        @Override
        public void run(IProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException {
            monitor.beginTask(Messages.getString("QDeleteWizard.deleteAction.beginTask"), 2); //$NON-NLS-1$
            monitor.worked(1);
            monitor.subTask(Messages.getString("QDeleteWizard.subTask.callMercurial")); //$NON-NLS-1$

            try {
                IStructuredSelection selection = (IStructuredSelection) page
                        .getPatchViewer().getSelection();
                List<Patch> patches = selection.toList();
                HgQDeleteClient.delete(resource, false, page
                        .getChangesetTable().getSelection(), patches);
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
    public QDeleteWizard(IResource resource) {
        super(Messages.getString("QDeleteWizard.title")); //$NON-NLS-1$
        this.resource = resource;
        setNeedsProgressMonitor(true);
        page = new QDeletePage(Messages.getString("QDeleteWizard.pageName"), Messages.getString("QDeleteWizard.pageTitle"), null, //$NON-NLS-1$ //$NON-NLS-2$
                Messages.getString("QDeleteWizard.pageDescription"), resource); //$NON-NLS-1$
        initPage(Messages.getString("QDeleteWizard.pageDescription"), //$NON-NLS-1$
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
        DeleteOperation delOperation = new DeleteOperation(getContainer());
        try {
            getContainer().run(false, false, delOperation);
            PatchQueueView.getView().populateTable();
        } catch (Exception e) {
            MercurialEclipsePlugin.logError(e);
            page.setErrorMessage(e.getLocalizedMessage());
            return false;
        }
        return true;
    }

}
