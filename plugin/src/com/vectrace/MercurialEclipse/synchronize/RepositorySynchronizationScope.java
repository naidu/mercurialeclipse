/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov - implementation
 *     Martin Olsen (Schantz) 	 - Synchronization of Multiple repositories
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.core.mapping.ISynchronizationScopeChangeListener;
import org.eclipse.team.internal.core.mapping.AbstractResourceMappingScope;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.ui.IMemento;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.synchronize.cs.HgChangeSetModelProvider;
import com.vectrace.MercurialEclipse.utils.Pair;

/**
 * @author Andrei
 */
@SuppressWarnings("restriction")
public class RepositorySynchronizationScope extends AbstractResourceMappingScope {

	private final ListenerList listeners;
	private final RepositoryLocationMap repositoryLocations;
	private MercurialSynchronizeSubscriber subscriber;
	private HgChangeSetModelProvider provider;

	public RepositorySynchronizationScope(RepositoryLocationMap repos) {
		Assert.isNotNull(repos);
		this.repositoryLocations = repos;

		listeners = new ListenerList(ListenerList.IDENTITY);
	}

	@Override
	public void addScopeChangeListener(ISynchronizationScopeChangeListener listener) {
		listeners.add(listener);
	}

	public ISynchronizationScope asInputScope() {
		return this;
	}

	@Override
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

	@Override
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

	@Override
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

	private static boolean isSupportedModelProvider(String modelProviderId) {
		return ModelProvider.RESOURCE_MODEL_PROVIDER_ID.equals(modelProviderId)
			|| HgChangeSetModelProvider.ID.equals(modelProviderId);
	}

	@Override
	public ModelProvider[] getModelProviders() {
		Set<ModelProvider> result = new HashSet<ModelProvider>();

		ResourceMapping[] mappings = getMappings();
		for (ResourceMapping mapping : mappings) {
			ModelProvider modelProvider = mapping.getModelProvider();
			if (modelProvider != null && isSupportedModelProvider(modelProvider.getId())) {
				result.add(modelProvider);
			}
		}
		result.add(getChangesetProvider());
		return result.toArray(new ModelProvider[result.size()]);
	}

	public HgChangeSetModelProvider getChangesetProvider() {
		if (provider == null) {
			try {
				provider = (HgChangeSetModelProvider) ModelProvider.getModelProviderDescriptor(HgChangeSetModelProvider.ID)
						.getModelProvider();
			} catch (CoreException e) {
				MercurialEclipsePlugin.logError(e);
			}
		}
		return provider;
	}

	/**
	 * @see org.eclipse.team.core.mapping.ISynchronizationScope#getProjects()
	 */
	public IProject[] getProjects() {
		return repositoryLocations.getProjects();
	}

	/**
	 * @see org.eclipse.team.internal.core.subscribers.AbstractSynchronizationScope#getRoots()
	 */
	@Override
	public IProject[] getRoots() {
		return getProjects();
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
				((ISynchronizationScopeChangeListener) object).scopeChanged(this, mappings, getTraversals());
			}
		}
	}

	@Override
	public void removeScopeChangeListener(ISynchronizationScopeChangeListener listener) {
		listeners.remove(listener);
	}

	public IHgRepositoryLocation getRepositoryLocation(IResource res) {
		return repositoryLocations.getRepositoryLocation(res.getProject());
	}

	public void setSubscriber(MercurialSynchronizeSubscriber mercurialSynchronizeSubscriber) {
		this.subscriber = mercurialSynchronizeSubscriber;
	}

	public MercurialSynchronizeSubscriber getSubscriber() {
		return subscriber;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RepositorySynchronizationScope [");
		if (repositoryLocations != null) {
			builder.append("repo=");
			builder.append(repositoryLocations);
			builder.append(", ");
		}
		if (repositoryLocations != null) {
			builder.append("locations=");
			builder.append(repositoryLocations.toString());
		}
		builder.append("]");
		return builder.toString();
	}

	public static class RepositoryLocationMap  {

		private final Map<IHgRepositoryLocation, Pair<HgRoot, IProject[]>> map;

		public RepositoryLocationMap(int size) {
			map =  new HashMap<IHgRepositoryLocation, Pair<HgRoot, IProject[]>>(size);
		}

		public RepositoryLocationMap(IMemento m) throws HgException, IOException {
			this(4);

			IWorkspaceRoot workspace = ResourcesPlugin.getWorkspace().getRoot();

			for (IMemento l : m.getChildren("location")) {
				IHgRepositoryLocation location = MercurialEclipsePlugin.getRepoManager().getRepoLocation(l.getString("uri"));
				List<IProject> list = new ArrayList<IProject>();
				Set<IProject> repoProjects = MercurialEclipsePlugin.getRepoManager().getAllRepoLocationProjects(location);
				HgRoot root = HgRoot.get(new File(l.getString("root")));

				for (IMemento p : l.getChildren("project")) {
					String sName = p.getTextData();

					if(sName.length() == 0){
						continue;
					}
					IProject project = workspace.getProject(sName);

					if(project != null && (repoProjects.contains(project) || (!project.isOpen() && project.exists()))){
						list.add(project);
					}
				}

				add(location, root, list.toArray(new IProject[list.size()]));
			}
		}

		public void serialize(IMemento m) {
			for (IHgRepositoryLocation loc : map.keySet()) {
				IMemento l = m.createChild("location");
				Pair<HgRoot, IProject[]> p = map.get(loc);

				l.putString("uri", loc.getLocation());
				l.putString("root", p.a.getLocation());

				for (IProject proj : p.b) {
					(l.createChild("project")).putTextData(proj.getName());
				}
			}
		}

		public IHgRepositoryLocation getRepositoryLocation(IProject proj) {
			for (IHgRepositoryLocation loc : map.keySet()) {
				for (IProject curProj : map.get(loc).b) {
					if (proj.equals(curProj)) {
						return loc;
					}
				}
			}
			return null;
		}

		public IHgRepositoryLocation getLocation(HgRoot root) {
			for (Entry<IHgRepositoryLocation, Pair<HgRoot, IProject[]>> loc : map.entrySet()) {
				if (loc.getValue().a.equals(root)) {
					return loc.getKey();
				}
			}
			return null;
		}

		public IProject[] getProjects() {
			Set<IProject> projects = new HashSet<IProject>();

			for (Pair<HgRoot, IProject[]> projs : map.values()) {
				projects.addAll(Arrays.asList(projs.b));
			}

			return projects.toArray(new IProject[projects.size()]);
		}

		public IProject[] getProjects(IHgRepositoryLocation repoLocation) {
			return map.get(repoLocation).b;
		}

		public void add(IHgRepositoryLocation repo, HgRoot hgRoot, IProject[] array) {
			map.put(repo, new Pair<HgRoot, IProject[]>(hgRoot, array));
		}

		public Set<IHgRepositoryLocation> getLocations() {
			return map.keySet();
		}

		public HgRoot getRoot(IHgRepositoryLocation repoLocation) {
			return map.get(repoLocation).a;
		}

		public String toString() {
			return "RLM: " + map.toString();
		}
	}
}
