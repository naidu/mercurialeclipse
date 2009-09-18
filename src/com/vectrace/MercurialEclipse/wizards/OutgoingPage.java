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
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.swt.widgets.Composite;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.cache.OutgoingChangesetCache;

/**
 * @author bastian
 *
 */
public class OutgoingPage extends IncomingPage {
    private boolean svn;
    private class GetOutgoingOperation extends HgOperation {

        public GetOutgoingOperation(IRunnableContext context) {
            super(context);
        }

        @Override
        protected String getActionDescription() {
            return Messages.getString("OutgoingPage.getOutgoingOperation.description"); //$NON-NLS-1$
        }

        @Override
        public void run(IProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException {
            monitor.beginTask(Messages.getString("OutgoingPage.getOutgoingOperation.beginTask"), 1); //$NON-NLS-1$
            monitor.subTask(Messages.getString("OutgoingPage.getOutgoingOperation.call")); //$NON-NLS-1$
            setChangesets(getOutgoingInternal());
            monitor.worked(1);
            monitor.done();
        }

        private SortedSet<ChangeSet> getOutgoingInternal() {
            if (isSvn()) {
                return new TreeSet<ChangeSet>();
            }
            HgRepositoryLocation remote = getLocation();
            try {
                SortedSet<ChangeSet> changesets = OutgoingChangesetCache
                        .getInstance().getChangeSets(getProject(), remote);
                SortedSet<ChangeSet> revertedSet = new TreeSet<ChangeSet>(Collections.reverseOrder());
                revertedSet.addAll(changesets);
                return revertedSet;
            } catch (HgException e) {
                MercurialEclipsePlugin.showError(e);
                return new TreeSet<ChangeSet>();
            }
        }

    }

    protected OutgoingPage(String pageName) {
        super(pageName);
        this.setTitle(Messages.getString("OutgoingPage.title")); //$NON-NLS-1$
        this
                .setDescription(Messages.getString("OutgoingPage.description1") //$NON-NLS-1$
                        + Messages.getString("OutgoingPage.description2")); //$NON-NLS-1$
    }

    @Override
    public void setChangesets(SortedSet<ChangeSet> outgoingInternal) {
        super.setChangesets(outgoingInternal);
    }

    @Override
    public SortedSet<ChangeSet> getChangesets() {
        return super.getChangesets();
    }

    @Override
    protected void getInputForPage() throws InvocationTargetException,
            InterruptedException {
        getContainer().run(true, false,
                new GetOutgoingOperation(getContainer()));
    }

    @Override
    public void createControl(Composite parent) {
        super.createControl(parent);
        getRevisionCheckBox().setText(Messages.getString("OutgoingPage.option.pushUpTo")); //$NON-NLS-1$
    }

    @Override
    public boolean isSvn() {
        return svn;
    }

    @Override
    public void setSvn(boolean svn) {
        this.svn = svn;
    }
}
