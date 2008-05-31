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

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.commands.mq.HgQRefreshClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.wizards.HgWizard;

/**
 * @author bastian
 * 
 */
public class QRefreshWizard extends HgWizard {
    private QNewWizardPage page = null;

    private class RefreshOperation extends HgOperation {

        /**
         * @param context
         */
        public RefreshOperation(IRunnableContext context) {
            super(context);
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.vectrace.MercurialEclipse.actions.HgOperation#getActionDescription()
         */
        @Override
        protected String getActionDescription() {
            return "Refreshing queue...";
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.vectrace.MercurialEclipse.actions.HgOperation#run(org.eclipse.core.runtime.IProgressMonitor)
         */
        @Override
        public void run(IProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException {
            monitor.beginTask("Initializing queue repository", 2);
            monitor.worked(1);
            monitor.subTask("Calling Mercurial...");

            try {
                HgQRefreshClient
                        .refresh(resource, page.getCommitMessageTextField()
                                .getText(), page.getForceCheckBox()
                                .getSelection(), page.getGitCheckBox()
                                .getSelection(), page.getIncludeTextField()
                                .getText(), page.getExcludeTextField()
                                .getText(), page.getUserTextField().getText(),
                                page.getDate().getText());
                monitor.worked(1);
                IFolder hgRoot = AbstractClient.getHgRoot(resource);
                MercurialStatusCache.getInstance().refreshStatus(hgRoot,
                        monitor);
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
    public QRefreshWizard(IResource resource) {
        super("Mercurial Queues Refresh Wizard");
        this.resource = resource;
        setNeedsProgressMonitor(true);
        page = new QNewWizardPage("QRefreshPage", "Update the current patch",
                null, null, resource, false);

        initPage("", page);
        addPage(page);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vectrace.MercurialEclipse.wizards.HgWizard#performFinish()
     */
    @Override
    public boolean performFinish() {
        RefreshOperation initOperation = new RefreshOperation(getContainer());
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
