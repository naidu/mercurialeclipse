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

import java.util.BitSet;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;

import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;

/**
 * @author bastian
 *
 */
public class MercurialSyncInfo extends SyncInfo {

    public MercurialSyncInfo(IResource local, IResourceVariant base,
            IResourceVariant remote, IResourceVariantComparator comparator) {
        super(local, base, remote, comparator);
    }

    @Override
    protected int calculateKind() throws TeamException {
        int description = super.calculateKind();
        if(description == IN_SYNC){
            // TODO don't trust our sync info implementation. Re-check with the cache.

            IResource local = getLocal();
            if(local == null){
                return description;
            }
            BitSet status = MercurialStatusCache.getInstance().getStatus(local);
            if(status == null){
                return description;
            }
            if(status.get(MercurialStatusCache.BIT_CONFLICT)){
                return CONFLICTING;
            }
            if(status.get(MercurialStatusCache.BIT_ADDED)){
                return ADDITION;
            }
            if(status.get(MercurialStatusCache.BIT_REMOVED) || status.get(MercurialStatusCache.BIT_DELETED)){
                return DELETION;
            }
            if(status.get(MercurialStatusCache.BIT_MODIFIED)){
                return CHANGE;
            }
        }

        // this lead to the conflict propagation to the resources, but the conflict
        // does not exist yet, it only can happen later during the update/merge.
        // here we are in the synchronization view, so all changes we see are in the future
        /*
        if (description == (CONFLICTING | CHANGE)) {
            // add resource conflict to status cache
            MercurialStatusCache.getInstance().addConflict(getLocal());
        }
        */
        return description;
    }
}
