/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeWorkspaceJob;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;

public class RefreshStatusHandler extends SingleResourceHandler {

    @Override
    protected void run(final IResource resource) throws Exception {
        new SafeWorkspaceJob(Messages.getString("RefreshStatusHandler.refreshingResource")+resource.getName()+"...") { //$NON-NLS-1$ //$NON-NLS-2$

            @Override
            protected IStatus runSafe(IProgressMonitor monitor) {
                try {
                    MercurialStatusCache.getInstance().refreshStatus(resource, monitor);
                } catch (HgException e) {
                    MercurialEclipsePlugin.logError(e);
                    return e.getStatus();
                }
                return super.runSafe(monitor);
            }
        }.schedule();
    }


}
