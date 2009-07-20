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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.core.mapping.ISynchronizationScopeChangeListener;
import org.eclipse.team.internal.ui.Utils;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;

/**
 * @author Andrei
 */
public class RepositorySynchronizationScope implements ISynchronizationScope {

    private final IResource[] roots;
    private final ListenerList listeners;

    public RepositorySynchronizationScope(IResource[] roots) {
        this.roots = roots != null? roots : MercurialStatusCache.getInstance()
                .getAllManagedProjects();
        listeners = new ListenerList(ListenerList.IDENTITY);
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.core.mapping.ISynchronizationScope#addScopeChangeListener(org.eclipse.team.core.mapping.ISynchronizationScopeChangeListener)
     */
    public void addScopeChangeListener(ISynchronizationScopeChangeListener listener) {
        listeners.add(listener);
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.core.mapping.ISynchronizationScope#asInputScope()
     */
    public ISynchronizationScope asInputScope() {
        return this;
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.core.mapping.ISynchronizationScope#contains(org.eclipse.core.resources.IResource)
     */
    public boolean contains(IResource resource) {
        ResourceTraversal[] traversals = getTraversals();
        if(traversals == null){
            return false;
        }
        for (ResourceTraversal traversal : traversals) {
            if (traversal.contains(resource)) {
                return true;
            }
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.core.mapping.ISynchronizationScope#getContext()
     */
    public ResourceMappingContext getContext() {
        // TODO unclear
        return ResourceMappingContext.LOCAL_CONTEXT;
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.core.mapping.ISynchronizationScope#getInputMappings()
     */
    public ResourceMapping[] getInputMappings() {
        return Utils.getResourceMappings(getRoots());
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.core.mapping.ISynchronizationScope#getMapping(java.lang.Object)
     */
    public ResourceMapping getMapping(Object modelObject) {
        ResourceMapping[] mappings = getMappings();
        for (ResourceMapping mapping : mappings) {
            if (mapping.getModelObject().equals(modelObject)) {
                return mapping;
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.core.mapping.ISynchronizationScope#getMappings()
     */
    public ResourceMapping[] getMappings() {
        return getInputMappings();
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.core.mapping.ISynchronizationScope#getMappings(java.lang.String)
     */
    public ResourceMapping[] getMappings(String modelProviderId) {
        Set<ResourceMapping> result = new HashSet<ResourceMapping>();
        ResourceMapping[] mappings = getMappings();
        for (ResourceMapping mapping : mappings) {
            if (mapping.getModelProviderId().equals(modelProviderId)) {
                result.add(mapping);
            }
        }
        return result.toArray(new ResourceMapping[result.size()]);
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.core.mapping.ISynchronizationScope#getModelProviders()
     */
    public ModelProvider[] getModelProviders() {
        Set<ModelProvider> result = new HashSet<ModelProvider>();
        ResourceMapping[] mappings = getMappings();
        for (ResourceMapping mapping : mappings) {
            ModelProvider modelProvider = mapping.getModelProvider();
            if (modelProvider != null) {
                result.add(modelProvider);
            }
        }
        return result.toArray(new ModelProvider[result.size()]);
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.core.mapping.ISynchronizationScope#getProjects()
     */
    public IProject[] getProjects() {
        Set<IProject> projects = new HashSet<IProject>();
        for (IResource res : roots) {
            projects.add(res.getProject());
        }
        return projects.toArray(new IProject[projects.size()]);
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.core.mapping.ISynchronizationScope#getRoots()
     */
    public IResource[] getRoots() {
        return roots;
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.core.mapping.ISynchronizationScope#getTraversals()
     */
    public ResourceTraversal[] getTraversals() {
        return new ResourceTraversal[] {
                new ResourceTraversal(getRoots(), IResource.DEPTH_INFINITE, IContainer.EXCLUDE_DERIVED) };
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.core.mapping.ISynchronizationScope#getTraversals(org.eclipse.core.resources.mapping.ResourceMapping)
     */
    public ResourceTraversal[] getTraversals(ResourceMapping mapping) {
        try {
            return mapping.getTraversals(getContext(), new NullProgressMonitor());
        } catch (CoreException e) {
            MercurialEclipsePlugin.logError(e);
            return null;
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.core.mapping.ISynchronizationScope#getTraversals(java.lang.String)
     */
    public ResourceTraversal[] getTraversals(String modelProviderId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.core.mapping.ISynchronizationScope#hasAdditionalMappings()
     */
    public boolean hasAdditionalMappings() {
        return false;
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.core.mapping.ISynchronizationScope#hasAdditonalResources()
     */
    public boolean hasAdditonalResources() {
        return false;
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.core.mapping.ISynchronizationScope#refresh(org.eclipse.core.resources.mapping.ResourceMapping[])
     */
    public void refresh(ResourceMapping[] mappings) {
        if(!listeners.isEmpty()){
            Object[] objects = listeners.getListeners();
            for (Object object : objects) {
                ((ISynchronizationScopeChangeListener)object).scopeChanged(this, mappings, getTraversals());
            }
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.core.mapping.ISynchronizationScope#removeScopeChangeListener(org.eclipse.team.core.mapping.ISynchronizationScopeChangeListener)
     */
    public void removeScopeChangeListener(ISynchronizationScopeChangeListener listener) {
        listeners.remove(listener);
    }

}
