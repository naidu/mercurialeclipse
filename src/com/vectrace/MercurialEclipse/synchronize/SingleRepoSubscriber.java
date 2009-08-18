/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Andrei	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.team.core.mapping.ISynchronizationScope;

import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

/**
 * Subscriber which is limited to one single repo
 *
 * @author Andrei
 */
public class SingleRepoSubscriber extends MercurialSynchronizeSubscriber {

    private final HgRepositoryLocation repositoryLocation;

    /**
     * @param synchronizationScope must be not null
     * @param repositoryLocation must be not null
     */
    public SingleRepoSubscriber(ISynchronizationScope synchronizationScope, HgRepositoryLocation repositoryLocation) {
        super(synchronizationScope);
        Assert.isNotNull(repositoryLocation);
        this.repositoryLocation = repositoryLocation;
    }

    @Override
    protected HgRepositoryLocation getRepo(IResource resource){
        return repositoryLocation;
    }
}
