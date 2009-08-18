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

    public void addScopeChangeListener(ISynchronizationScopeChangeListener listener) {
        listeners.add(listener);
    }

    public ISynchronizationScope asInputScope() {
        return this;
    }

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

    public ResourceMappingContext getContext() {
        // TODO unclear
        return ResourceMappingContext.LOCAL_CONTEXT;
    }

    public ResourceMapping[] getInputMappings() {
        return Utils.getResourceMappings(getRoots());
    }

    public ResourceMapping getMapping(Object modelObject) {
        ResourceMapping[] mappings = getMappings();
        for (ResourceMapping mapping : mappings) {
            if (mapping.getModelObject().equals(modelObject)) {
                return mapping;
            }
        }
        return null;
    }

    public ResourceMapping[] getMappings() {
        return getInputMappings();
    }

    public ResourceMapping[] getMappings(String modelProviderId) {
        if(!isSupportedModelProvider(modelProviderId)){
            return null;
        }
        Set<ResourceMapping> result = new HashSet<ResourceMapping>();
        ResourceMapping[] mappings = getMappings();
        for (ResourceMapping mapping : mappings) {
            if (mapping.getModelProviderId().equals(modelProviderId)) {
                result.add(mapping);
            }
        }
        return result.toArray(new ResourceMapping[result.size()]);
    }

    private boolean isSupportedModelProvider(String modelProviderId) {
        return ModelProvider.RESOURCE_MODEL_PROVIDER_ID.equals(modelProviderId);
    }

    public ModelProvider[] getModelProviders() {
        Set<ModelProvider> result = new HashSet<ModelProvider>();
        ResourceMapping[] mappings = getMappings();
        for (ResourceMapping mapping : mappings) {
            ModelProvider modelProvider = mapping.getModelProvider();
            if (modelProvider != null && isSupportedModelProvider(modelProvider.getId())) {
                result.add(modelProvider);
            }
        }
        return result.toArray(new ModelProvider[result.size()]);
    }

    public IProject[] getProjects() {
        Set<IProject> projects = new HashSet<IProject>();
        for (IResource res : roots) {
            projects.add(res.getProject());
        }
        return projects.toArray(new IProject[projects.size()]);
    }

    public IResource[] getRoots() {
        return roots;
    }

    public ResourceTraversal[] getTraversals() {
        return new ResourceTraversal[] {
                new ResourceTraversal(getRoots(), IResource.DEPTH_INFINITE, IContainer.EXCLUDE_DERIVED) };
    }

    public ResourceTraversal[] getTraversals(ResourceMapping mapping) {
        try {
            return mapping.getTraversals(getContext(), new NullProgressMonitor());
        } catch (CoreException e) {
            MercurialEclipsePlugin.logError(e);
            return null;
        }
    }

    public ResourceTraversal[] getTraversals(String modelProviderId) {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean hasAdditionalMappings() {
        return false;
    }

    public boolean hasAdditonalResources() {
        return false;
    }

    public void refresh(ResourceMapping[] mappings) {
        if(!listeners.isEmpty()){
            Object[] objects = listeners.getListeners();
            for (Object object : objects) {
                ((ISynchronizationScopeChangeListener)object).scopeChanged(this, mappings, getTraversals());
            }
        }
    }

    public void removeScopeChangeListener(ISynchronizationScopeChangeListener listener) {
        listeners.remove(listener);
    }

}
