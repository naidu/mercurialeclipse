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

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.team.MercurialRevisionStorage;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;

public class MercurialResourceVariantComparator implements
        IResourceVariantComparator {

    private static MercurialStatusCache statusCache = MercurialStatusCache
            .getInstance();
    private ChangeSet csAtRoot;

    public MercurialResourceVariantComparator() {
    }

    public boolean compare(IResource local, IResourceVariant repoRevision) {
        try {
            // XXX this is either a big mess or I can't figure out how it should work..
            // why do we fetch changeset for ONE specific resource only, IF the comparator
            // is used for ALL resources???
            if (csAtRoot == null) {
                csAtRoot = LocalChangesetCache.getInstance().getChangesetByRootId(local);
            }
        } catch (HgException e) {
            MercurialEclipsePlugin.logError(e);
            return false;
        }

        if (!statusCache.isClean(local)) {
            return false;
        }
        if(repoRevision == null){
            return true;
        }

        MercurialRevisionStorage remoteIStorage;
        try {
            remoteIStorage = (MercurialRevisionStorage) repoRevision.getStorage(null);
        } catch (TeamException e) {
            MercurialEclipsePlugin.logError(e);
            return false;
        }

        ChangeSet cs = remoteIStorage.getChangeSet();

        // if this is outgoing or incoming, it can't be equal to any other changeset
        Direction direction = cs.getDirection();
        if ((direction == Direction.INCOMING || direction == Direction.OUTGOING)
                && csAtRoot!= null && cs.getBranch().equals(csAtRoot.getBranch())) {
            return false;
        }
        // resource is clean and we compare against our local repository
        return true;
    }

    public boolean compare(IResourceVariant base, IResourceVariant remote) {
        MercurialResourceVariant mrv = (MercurialResourceVariant) remote;
        if (csAtRoot != null && mrv.getRev().getChangeSet().getBranch().equals(
                csAtRoot.getBranch())) {
            return base.getContentIdentifier().equals(
                    remote.getContentIdentifier());
        }
        return true;
    }

    public boolean isThreeWay() {
        return true;
    }

}
