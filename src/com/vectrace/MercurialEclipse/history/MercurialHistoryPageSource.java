/*******************************************************************************
 * Copyright (c) 2007-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Stefan Groschupf          - logError
 *     Stefan C                  - Code cleanup
 *******************************************************************************/
package com.vectrace.MercurialEclipse.history;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.ui.history.HistoryPageSource;
import org.eclipse.ui.part.Page;

import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;

/**
 * @author zingo
 *
 */
public class MercurialHistoryPageSource extends HistoryPageSource {

    public MercurialHistoryPageSource(MercurialHistoryProvider fileHistoryProvider) {
        super();
    }

    public boolean canShowHistoryFor(Object object) {
        if (!(object instanceof IResource)) {
            return false;
        }
        IResource resource = (IResource) object;
        MercurialStatusCache cache = MercurialStatusCache.getInstance();
        if(resource.exists()) {
            return cache.isSupervised(resource)
                && !(cache.isAdded(resource.getProject(), resource.getLocation()));
        }
        // allow to show history for files which are already deleted and committed
        // (neither in the cache nor on disk)
        return MercurialUtilities.isPossiblySupervised(resource);
    }

    public Page createPage(Object object) {
        return new MercurialHistoryPage((IResource) object);
    }

}
