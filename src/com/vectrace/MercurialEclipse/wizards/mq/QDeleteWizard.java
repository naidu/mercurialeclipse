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
import com.vectrace.MercurialEclipse.commands.mq.HgQDeleteClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.Patch;
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
            return "Creating patch...";
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.vectrace.MercurialEclipse.actions.HgOperation#run(org.eclipse.core.runtime.IProgressMonitor)
         */
        @SuppressWarnings("unchecked")
        @Override
        public void run(IProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException {
            monitor.beginTask("Initializing queue repository", 2);
            monitor.worked(1);
            monitor.subTask("Calling Mercurial...");

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
        super("Mercurial Queue New Patch Wizard");
        this.resource = resource;
        setNeedsProgressMonitor(true);
        page = new QDeletePage("QDeletePage", "Delete patch from queue", null,
                "Stop managing the patch with Mercurial Queues", resource);
        initPage("Creates a new patch on top of the currently-applied patch",
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
        } catch (Exception e) {
            MercurialEclipsePlugin.logError(e);
            page.setErrorMessage(e.getLocalizedMessage());
            return false;
        }
        return true;
    }

}
