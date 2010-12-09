/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * npiguet	implementation
 * John Peberdy refactoring
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team.cache;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.preference.IPreferenceStore;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgRootClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.HgRootContainer;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * This handles the known roots cache. The caching for an individual resources is handled as a
 * session property on the resource.
 *
 * @author npiguet
 */
public class MercurialRootCache extends AbstractCache {

	// constants

	private static final QualifiedName SESSION_KEY = new QualifiedName(MercurialEclipsePlugin.ID, "MercurialRootCacheKey");

	private static final Object NO_ROOT = new String("No Mercurial root");

	// associations

	private final ConcurrentHashMap<HgRoot, HgRoot> knownRoots = new ConcurrentHashMap<HgRoot, HgRoot>(16, 0.75f, 4);

	// constructor

	private MercurialRootCache(){
	}

	// operations

	private HgRoot calculateHgRoot(File file) {
		if (file instanceof HgRoot) {
			return (HgRoot) file;
		}

		// TODO: possible optimization: try to look for the parent in the cache, or load the whole hierarchy in cache
		//       or something else like that, so we don't need to call HgRootClient for each file in a directory
		HgRoot root;

		try {
			root = HgRootClient.getHgRoot(file);
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
			// no root found at all
			root = null;
		}

		if (root != null) {
			HgRoot prev = knownRoots.putIfAbsent(root, root);

			if (prev != null)
			{
				root = prev;
			}
		}

		return root;
	}

	/**
	 * Find the hgroot for the given resource.
	 *
	 * @param resource The resource, not null.
	 * @return The hgroot, or null if an error occurred or not found
	 */
	public HgRoot getHgRoot(IResource resource) {
		if (resource instanceof HgRootContainer) {
			// special case for HgRootContainers, they already know their HgRoot
			return ((HgRootContainer) resource).getHgRoot();
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
			root = calculateHgRoot(ResourceUtils.getFileHandle(resource));

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

	public Collection<HgRoot> getKnownHgRoots(){
		return new ArrayList<HgRoot>(this.knownRoots.values());
	}

	@Override
	protected void configureFromPreferences(IPreferenceStore store) {
		// nothing to do
	}

	/**
	 * @see com.vectrace.MercurialEclipse.team.cache.AbstractCache#projectDeletedOrClosed(org.eclipse.core.resources.IProject)
	 */
	@Override
	protected void projectDeletedOrClosed(IProject project) {
		IPath projPath = project.getLocation();

		if (projPath != null) {
			for (Iterator<HgRoot> it = knownRoots.values().iterator(); it.hasNext();) {
				if (projPath.isPrefixOf(it.next().getIPath())) {
					it.remove();
				}
			}
		}
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
