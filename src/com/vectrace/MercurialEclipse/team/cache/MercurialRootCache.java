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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.preference.IPreferenceStore;

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

	/**
	 * We use an HgRoot[] of size 1 for values because null values are not allowed in ConcurrentHashMaps.
	 */
	private final Map<File, HgRoot[]> byFile = new ConcurrentHashMap<File, HgRoot[]>();
	private final Map<IProject, HgRoot[]> byProject = new ConcurrentHashMap<IProject, HgRoot[]>();
	private final Set<HgRoot> knownRoots = new CopyOnWriteArraySet<HgRoot>();

	private MercurialRootCache(){
		// do we need to listen to resource changes?
		// ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
	}

	public HgRoot getHgRoot(File file) throws HgException {
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
		if(resource instanceof HgRootContainer){
			// special case for HgRootContainers, they already know their HgRoot
			HgRootContainer rootContainer = (HgRootContainer) resource;
			return rootContainer.getHgRoot();
		}

		File file = ResourceUtils.getFileHandle(resource);
		HgRoot root = getHgRoot(file);
		if(root != null && resource instanceof IProject && !byProject.containsKey(resource)){
			byProject.put((IProject)resource, new HgRoot[]{root});
		}
		return root;
	}

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

		if(resource instanceof IProject){
			byProject.remove(resource);
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
