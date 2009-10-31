/*******************************************************************************
 * Copyright (c) 2003, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 *     Bastian Doetsch              - adaptation
 ******************************************************************************/
package com.vectrace.MercurialEclipse.repository;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

/**
 * provides some static methods to handle repository management 
 * (deletion of remote resources etc ...)
 */
public class RepositoryResourcesManager {
    private static RepositoryResourcesManager instance;
    
    private RepositoryResourcesManager() {     
    }
    
    public static RepositoryResourcesManager getInstance(){
        if (instance == null){
            instance = new RepositoryResourcesManager();
        }
        return instance;
    }
    
    private List<IRepositoryListener> repositoryListeners = new ArrayList<IRepositoryListener>();


    /**
     * Register to receive notification of repository creation and disposal
     */
    public void addRepositoryListener(IRepositoryListener listener) {
        repositoryListeners.add(listener);
    }

    /**
     * De-register a listener
     */
    public void removeRepositoryListener(IRepositoryListener listener) {
        repositoryListeners.remove(listener);
    }

    /**
     * signals all listener that we have removed a repository 
     */
    public void repositoryRemoved(HgRepositoryLocation repository) {
        Iterator<IRepositoryListener> it = repositoryListeners.iterator();
        while (it.hasNext()) {
            IRepositoryListener listener = it.next();
            listener.repositoryRemoved(repository);
        }    
    }

    /**
     * signals all listener that we have removed a repository 
     */
    public void repositoryAdded(HgRepositoryLocation repository) {
        Iterator<IRepositoryListener> it = repositoryListeners.iterator();
        while (it.hasNext()) {
            IRepositoryListener listener = it.next();
            listener.repositoryAdded(repository);
        }    
    }

    /**
     * signals all listener that we have removed a repository 
     */
    public void repositoryModified(HgRepositoryLocation repository) {
        Iterator<IRepositoryListener> it = repositoryListeners.iterator();
        while (it.hasNext()) {
            IRepositoryListener listener = it.next();
            listener.repositoryModified(repository);
        }    
    }


}
