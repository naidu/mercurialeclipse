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

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;

/**
 * @author bastian
 * 
 */
public class MercurialSyncInfo extends SyncInfo {
//    private final static Differencer DIFFERENCER = new Differencer();

    /**
     * @param local
     * @param base
     * @param remote
     * @param comparator
     */
    public MercurialSyncInfo(IResource local, IResourceVariant base,
            IResourceVariant remote, IResourceVariantComparator comparator) {
        super(local, base, remote, comparator);
    }

    @Override
    protected int calculateKind() throws TeamException {
        int result = super.calculateKind();

        // if it's an incoming change, we gotta find out, if it's a conflict
        if (result == (INCOMING | CHANGE) || result == (CONFLICTING | CHANGE)) {
            // FIXME: This always shows a conflict, as it compares 
            // each character. I'd like something like automerge detection.
            
//            RevisionNode baseRev = new RevisionNode(
//                    ((MercurialResourceVariant) getBase()).getRev());
//            RevisionNode remoteRev = new RevisionNode(
//                    ((MercurialResourceVariant) getRemote()).getRev());
//
//            Object o = MercurialSyncInfo.DIFFERENCER.findDifferences(true,
//                    null, null, null, baseRev, remoteRev);
//
//            if (o != null && o instanceof DiffNode) {
//                DiffNode node = (DiffNode) o;
//                int kind = node.getKind();
//                return kind;
//            }
        }
        return result;
    }

}
