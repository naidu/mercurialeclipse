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
package com.vectrace.MercurialEclipse.operations;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;

import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.commands.HgCloneClient;
import com.vectrace.MercurialEclipse.commands.extensions.HgSvnClient;
import com.vectrace.MercurialEclipse.commands.extensions.forest.HgFcloneClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.wizards.Messages;

public class CloneOperation extends HgOperation {

    private final String parentDirectory;
    private final HgRepositoryLocation repo;
    private final boolean noUpdate;
    private final boolean pull;
    private final boolean uncompressed;
    private final boolean timeout;
    private final String rev;
    private final String cloneName;
    private final boolean forest;
    private final boolean svn;

    public CloneOperation(IRunnableContext context, String parentDirectory,
            HgRepositoryLocation repo, boolean noUpdate, boolean pull,
            boolean uncompressed, boolean timeout, String rev,
            String cloneName, boolean forest, boolean svn) {
        super(context);
        this.parentDirectory = parentDirectory;
        this.repo = repo;
        this.noUpdate = noUpdate;
        this.pull = pull;
        this.uncompressed = uncompressed;
        this.timeout = timeout;
        this.rev = rev;
        this.cloneName = cloneName;
        this.forest = forest;
        this.svn = svn;
    }

    @Override
    public void run(IProgressMonitor m) throws InvocationTargetException,
            InterruptedException {

        m.beginTask(Messages.getString("CloneRepoWizard.operation.name"), 50); //$NON-NLS-1$

        m.subTask(Messages
                        .getString("CloneRepoWizard.subTaskParentDirectory.name") + parentDirectory); //$NON-NLS-1$
        m.worked(1);

        m.subTask(Messages
                        .getString("CloneRepoWizard.subTaskCloneDirectory.name") + cloneName); //$NON-NLS-1$
        m.worked(1);

        try {
            m.subTask(Messages
                    .getString("CloneRepoWizard.subTask.invokingMercurial")); //$NON-NLS-1$
            if (svn) {
                HgSvnClient.clone(new File(parentDirectory), repo, timeout, cloneName);
            } else if (!forest) {
                HgCloneClient.clone(parentDirectory, repo, noUpdate, pull,
                        uncompressed, timeout, rev, cloneName);
            } else {
                HgFcloneClient.fclone(parentDirectory, repo, noUpdate, pull,
                        uncompressed, timeout, rev, cloneName);
            }
            m.worked(1);
        } catch (HgException e) {
            throw new InvocationTargetException(e);
        }
    }

    @Override
    protected String getActionDescription() {
        return Messages.getString("CloneRepoWizard.actionDescription.1") + repo + Messages.getString("CloneRepoWizard.actionDescription.2") + cloneName; //$NON-NLS-1$ //$NON-NLS-2$
    }

    public String getParentDirectory() {
        return parentDirectory;
    }

}