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

    public MercurialSyncInfo(IResource local, IResourceVariant base,
            IResourceVariant remote, IResourceVariantComparator comparator) {
        super(local, base, remote, comparator);
    }

    @Override
    protected int calculateKind() throws TeamException {
        int description = super.calculateKind();
        return description;
    }
}
