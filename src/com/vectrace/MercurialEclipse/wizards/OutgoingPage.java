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
        
        /**
         * @param context
         */
        public GetOutgoingOperation(IRunnableContext context) {
            super(context);

        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * com.vectrace.MercurialEclipse.actions.HgOperation#getActionDescription
         * ()
         */
        @Override
        protected String getActionDescription() {
            return "Retrieving outoing changesets...";
        }

        @Override
        public void run(IProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException {
            monitor.beginTask("Getting outgoing changesets...", 1);
            monitor.subTask("Calling Mercurial...");
            setChangeSets(getOutgoingInternal());
            monitor.worked(1);
            monitor.done();
        }

        private SortedSet<ChangeSet> getOutgoingInternal() {
            try {
                if (!isSvn()) {
                    HgRepositoryLocation remote = getLocation();
                    SortedSet<ChangeSet> changesets = OutgoingChangesetCache
                            .getInstance().getOutgoingChangeSets(getProject(),
                                    remote);
                    if (changesets != null) {
                        SortedSet<ChangeSet> revertedSet = new TreeSet<ChangeSet>(
                                Collections.reverseOrder());
                        revertedSet.addAll(changesets);
                        return revertedSet;
                    }
                }
            } catch (HgException e) {
                MercurialEclipsePlugin.showError(e);
            }
            return new TreeSet<ChangeSet>();
        }

    }

    /**
     * @param pageName
     */
    protected OutgoingPage(String pageName) {
        super(pageName);
        this.setTitle("Outgoing changesets");
        this
                .setDescription("Click on a changeset to see changed files. "
                        + "Double-click on a file to compare against workspace revision.");
    }

    /**
     * @param outgoingInternal
     */
    public void setChangeSets(SortedSet<ChangeSet> outgoingInternal) {
        super.setChangesets(outgoingInternal);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vectrace.MercurialEclipse.wizards.IncomingPage#getChangesets()
     */
    @Override
    public SortedSet<ChangeSet> getChangesets() {
        return super.getChangesets();
    }

    /**
     * @throws InvocationTargetException
     * @throws InterruptedException
     */
    @Override
    protected void getInputForPage() throws InvocationTargetException,
            InterruptedException {
        getContainer().run(true, false,
                new GetOutgoingOperation(getContainer()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.vectrace.MercurialEclipse.wizards.IncomingPage#createControl(org.
     * eclipse.swt.widgets.Composite)
     */
    @Override
    public void createControl(Composite parent) {
        super.createControl(parent);
        getRevisionCheckBox().setText("Push changes up to selected revision");
    }

    /**
     * @return the svn
     */
    @Override
    public boolean isSvn() {
        return svn;
    }

    /**
     * @param svn
     *            the svn to set
     */
    @Override
    public void setSvn(boolean svn) {
        this.svn = svn;
    }
}
