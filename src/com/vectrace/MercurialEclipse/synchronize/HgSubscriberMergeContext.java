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
package com.vectrace.MercurialEclipse.synchronize;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.core.subscribers.Subscriber;

/**
 * @author bastian
 *
 */
public class HgSubscriberMergeContext extends
        org.eclipse.team.core.subscribers.SubscriberMergeContext {

    /**
     * @param subscriber
     * @param manager
     */
    public HgSubscriberMergeContext(Subscriber subscriber,
            ISynchronizationScopeManager manager) {
        super(subscriber, manager);
        initialize();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.team.core.mapping.provider.MergeContext#makeInSync(org.eclipse
     * .team.core.diff.IDiff, org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    protected void makeInSync(IDiff diff, IProgressMonitor monitor)
            throws CoreException {
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.team.core.mapping.IMergeContext#markAsMerged(org.eclipse.
     * team.core.diff.IDiff, boolean, org.eclipse.core.runtime.IProgressMonitor)
     */
    public void markAsMerged(IDiff node, boolean inSyncHint,
            IProgressMonitor monitor) throws CoreException {
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.team.core.mapping.IMergeContext#reject(org.eclipse.team.core
     * .diff.IDiff, org.eclipse.core.runtime.IProgressMonitor)
     */
    public void reject(IDiff diff, IProgressMonitor monitor)
            throws CoreException {
    }

}
