/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * npiguet	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team.cache;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.preference.IPreferenceStore;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgRootClient;
import com.vectrace.MercurialEclipse.commands.Messages;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.HgRootContainer;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * @author npiguet
 *
 */
public class MercurialRootCache extends AbstractCache {

	// constants

	private static final QualifiedName SESSION_KEY = new QualifiedName(MercurialEclipsePlugin.ID, "MercurialRootCacheKey");

	private static final Object NO_ROOT = new String("No Mercurial root");

	// associations

	/**
	 * We use an HgRoot[] of size 1 for values because null values are not allowed in ConcurrentHashMaps.
	 *
	 * TODO: Can we eliminate this map? It is currently leaking.
	 * @deprecated
	 */
	@Deprecated
	private final Map<File, HgRoot[]> byFile = new ConcurrentHashMap<File, HgRoot[]>();

	private final Set<HgRoot> knownRoots = new CopyOnWriteArraySet<HgRoot>();

	// constructor

	private MercurialRootCache(){
		// do we need to listen to resource changes?
		// ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
	}

	// operations

	/**
	 * @deprecated Use {@link #getHgRoot(IResource)}
	 */
	@Deprecated
	public HgRoot getHgRoot(File file) throws HgException {
		if (file instanceof HgRoot) {
			return (HgRoot) file;
		}

		// TODO: possible optimization: try to look for the parent in the cache, or load the whole hierarchy in cache
		//       or something else like that, so we don't need to call HgRootClient for each file in a directory

		// get the root from the cache
		HgRoot[] root = byFile.get(file);
		if(root != null){
			return root[0];
		}

		// not in the cache, try again with canonical version of the file
		File canonical = null;
		try {
			canonical = file.getCanonicalFile();
		} catch (IOException e) {
			throw new HgException(Messages.getString("HgRootClient.error.cannotGetCanonicalPath")+file.getName());
		}
		root = byFile.get(canonical);
		if(root != null){
			// root found, cache it also for the non canonical file
			byFile.put(file, root);
			return root[0];
		}

		// not in the cache at all, get it from the HgRootClient
		try{
			root = new HgRoot[]{HgRootClient.getHgRoot(canonical)};
		}catch(HgException hge){
			// no root found at all
			root = new HgRoot[1];
		}

		// store it in the cache whether the result is null or not. Negative results are also good to cache
		byFile.put(file, root);
		byFile.put(canonical, root);
		if(root[0] != null){
			knownRoots.add(root[0]);
		}

		return root[0];
	}

	public HgRoot getHgRoot(IResource resource) throws HgException {
		if (resource instanceof HgRootContainer) {
			// special case for HgRootContainers, they already know their HgRoot
			HgRootContainer rootContainer = (HgRootContainer) resource;
			return rootContainer.getHgRoot();
		}

		// As an optimization only cache for containers not files
		if (resource instanceof IFile)
		{
			IResource parent = ((IFile)resource).getParent();
			resource = (parent == null) ? resource : parent;
		}

		boolean cacheResult = true;
		Object result = null;
		HgRoot root;

		try {
			result = resource.getSessionProperty(SESSION_KEY);
		} catch (CoreException e) {
			// Possible reasons:
			// - This resource does not exist.
			// - This resource is not local.
			// - This resource is a project that is not open.
			cacheResult = false;
		}

		if (result == NO_ROOT) {
			root = null;
		} else if (result == null) {
			root = getHgRoot(ResourceUtils.getFileHandle(resource));

			if (cacheResult) {
				try {
					resource.setSessionProperty(SESSION_KEY, root == null ? NO_ROOT : root);
				} catch (CoreException e) {
					// Possible reasons:
					// - 3 reasons above, or
					// - Resource changes are disallowed during certain types of resource change event
					// notification. See IResourceChangeEvent for more details.
					MercurialEclipsePlugin.logError(e);
				}
			}
		} else {
			root = (HgRoot) result;
		}

		return root;
	}

	/**
	 * Calls {@link #getHgRoot(IResource)} but returns null rather than throwing an exception.
	 *
	 * @param resource The resource to get the root for
	 * @return The root, or null if an error occurred
	 */
	public HgRoot hasHgRoot(IResource resource) {
		try{
			return getHgRoot(resource);
		}catch(HgException hge){
			return null;
		}
	}

	public void evict(IResource resource){
		// remove both the simple file and the canonical file
		File file = resource.getLocation().toFile();
		byFile.remove(file);
		try{
			File canonical = file.getCanonicalFile();
			byFile.remove(canonical);
		}catch(IOException ioe){
			// if that happens now, it is unlikely that the canonical file was in the cache to begin with...
		}
	}

	public SortedSet<HgRoot> getKnownHgRoots(){
		return new TreeSet<HgRoot>(this.knownRoots);
	}

	@Override
	protected void configureFromPreferences(IPreferenceStore store) {
		// nothing to do

	}
	@Override
	protected void projectDeletedOrClosed(IProject project) {
		this.evict(project);
		// TODO: maybe we need to clear all the paths inside the project
		// TODO: maybe not...
	}

	public static MercurialRootCache getInstance(){
		return MercurialRootCacheHolder.INSTANCE;
	}

	/**
	 * Initialization On Demand Holder idiom, thread-safe and instance will not be created until getInstance is called
	 * in the outer class.
	 */
	private static final class MercurialRootCacheHolder {
		private static final MercurialRootCache INSTANCE = new MercurialRootCache();
		private MercurialRootCacheHolder(){}
	}
}
