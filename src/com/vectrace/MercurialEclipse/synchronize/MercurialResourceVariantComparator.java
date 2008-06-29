/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian Doetsch	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize;

import java.util.BitSet;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.team.IStorageMercurialRevision;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;

public class MercurialResourceVariantComparator implements
        IResourceVariantComparator {

    private static MercurialStatusCache statusCache = MercurialStatusCache
            .getInstance();

    public MercurialResourceVariantComparator() {
    }

    public boolean compare(IResource local, IResourceVariant repoRevision) {
        BitSet bitSet = statusCache.getStatus(local);
        if (bitSet != null) {
            int status = bitSet.length() - 1;
            if (status == MercurialStatusCache.BIT_CLEAN) {
               try {
                    IStorageMercurialRevision remoteIStorage = (IStorageMercurialRevision) repoRevision
                            .getStorage(null);
                    ChangeSet cs = remoteIStorage.getChangeSet();                   

                    // if this is outgoing or incoming, it can't be equal to any other
                    // changeset
                    if (cs.getDirection() == Direction.OUTGOING
                            || cs.getDirection() == Direction.INCOMING) {
                        return false;
                    }
                    
                    // resource is clean and we compare against our local repository
                    return true;
                    
                } catch (HgException e) {
                    MercurialEclipsePlugin.logError(e);
                } catch (TeamException e) {
                    MercurialEclipsePlugin.logError(e);
                }                                              
            }
        }
        return false;

    }

    public boolean compare(IResourceVariant base, IResourceVariant remote) {
        return base.getContentIdentifier()
                .equals(remote.getContentIdentifier());
    }

    public boolean isThreeWay() {
        return true;
    }

}
