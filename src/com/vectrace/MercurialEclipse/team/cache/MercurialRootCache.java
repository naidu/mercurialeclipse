/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		npiguet				- implementation
 *		John Peberdy 		- refactoring
 *		Andrei Loskutov     - bug fixes
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
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.team.core.RepositoryProvider;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgRootClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.HgRootContainer;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * This handles the known roots cache. The caching for an individual resources is handled as a
 * session property on the resource.
 *
 * @author npiguet
 */
public class MercurialRootCache extends AbstractCache {

	private static final QualifiedName SESSION_KEY = new QualifiedName(MercurialEclipsePlugin.ID,
			"MercurialRootCacheKey");

	/**
	 * The current sentinel for no root
	 */
	private String noRoot = "No Mercurial root";

	private final ConcurrentHashMap<HgRoot, HgRoot> knownRoots = new ConcurrentHashMap<HgRoot, HgRoot>(
			16, 0.75f, 4);

	private MercurialRootCache() {
	}

	private HgRoot calculateHgRoot(File file, boolean reportNotFoundRoot) {
		if (file instanceof HgRoot) {
			return (HgRoot) file;
		}

		// TODO: possible optimization: try to look for the parent in the cache, or load the whole hierarchy in cache
		//       or something else like that, so we don't need to call HgRootClient for each file in a directory
		HgRoot root;

		try {
			root = HgRootClient.getHgRoot(file);
		} catch (HgException e) {
			if(reportNotFoundRoot) {
				MercurialEclipsePlugin.logError(e);
			}
			// no root found at all
			root = null;
		}

		if (root != null) {
			HgRoot prev = knownRoots.putIfAbsent(root, root);

			if (prev != null) {
				root = prev;
			}
		}

		return root;
	}

	/**
	 * Find the hgroot for the given resource. If the root could not be found,
	 * no error would be reported.
	 *
	 * @param resource The resource, not null.
	 * @return The hgroot, or null if an error occurred or root was not found.
	 */
	public HgRoot hasHgRoot(IResource resource) {
		return getHgRoot(resource, false);
	}

	/**
	 * Find the hgroot for the given resource.
	 *
	 * @param resource The resource, not null.
	 * @return The hgroot, or null if an error occurred or not found
	 */
	public HgRoot getHgRoot(IResource resource, boolean reportNotFoundRoot) {
		if (resource instanceof HgRootContainer) {
			// special case for HgRootContainers, they already know their HgRoot
			return ((HgRootContainer) resource).getHgRoot();
		}

		IProject project = resource.getProject();
		if(!project.isAccessible()) {
			return null;
		}

		// The call to RepositoryProvider is needed to trigger configure(project) on
		// MercurialTeamProvider if it doesn't happen before. Additionally, we avoid the
		// case if the hg root is there but project is NOT configured for MercurialEclipse
		// as team provider. See issue 13448.
		RepositoryProvider provider = RepositoryProvider.getProvider(project,
				MercurialTeamProvider.ID);
		if (!(provider instanceof MercurialTeamProvider)) {
			return null;
		}

		// As an optimization only cache for containers not files
		if (resource instanceof IFile) {
			resource = resource.getParent();
		}

		boolean cacheResult = true;
		try {
			Object cachedRoot = resource.getSessionProperty(SESSION_KEY);
			if(cachedRoot instanceof HgRoot) {
				return (HgRoot) cachedRoot;
			}
			if (cachedRoot == noRoot) {
				return null;
			}
		} catch (CoreException e) {
			// Possible reasons:
			// - This resource does not exist.
			// - This resource is not local.
			cacheResult = false;
		}

		// cachedRoot can be only null or an obsolete noRoot object
		HgRoot root = calculateHgRoot(ResourceUtils.getFileHandle(resource), reportNotFoundRoot);
		if (cacheResult) {
			try {
				resource.setSessionProperty(SESSION_KEY, root == null ? noRoot : root);
			} catch (CoreException e) {
				// Possible reasons:
				// - 2 reasons above, or
				// - Resource changes are disallowed during certain types of resource change event
				// notification. See IResourceChangeEvent for more details.
				MercurialEclipsePlugin.logError(e);
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
		return getHgRoot(resource, true);
	}

	public Collection<HgRoot> getKnownHgRoots(){
		return new ArrayList<HgRoot>(this.knownRoots.values());
	}

	@Override
	protected void configureFromPreferences(IPreferenceStore store) {
		// nothing to do
	}

	@Override
	public void projectDeletedOrClosed(IProject project) {
		IPath projPath = ResourceUtils.getPath(project);

		if (!projPath.isEmpty()) {
			for (Iterator<HgRoot> it = knownRoots.values().iterator(); it.hasNext();) {
				if (projPath.isPrefixOf(it.next().getIPath())) {
					it.remove();
				}
			}
		}
	}

	/**
	 * When there are changes done outside of eclipse the root of a resource may go away or change.
	 *
	 * @param resource
	 *            The resource to evict.
	 */
	public void uncache(IResource resource) {
		// A different more efficient approach would be to mark all contained hgroots (or just all
		// known root) as obsolete and then when a resource is queried we can detect this and
		// discard the cached result thereby making the invalidation lazy. But that would make
		// things more complex so use brute force for now:
		try {
			resource.accept(new IResourceVisitor() {
				public boolean visit(IResource res) throws CoreException {
					res.setSessionProperty(SESSION_KEY, null);
					return true;
				}
			});
		} catch (CoreException e) {
			// CoreException - if this method fails. Reasons include:
			// - This resource does not exist.
			// - The visitor failed with this exception.
			MercurialEclipsePlugin.logError(e);
		}
	}

	/**
	 * When a new repository is created previous negative cached results should be discarded.
	 */
	public void uncacheAllNegative() {
		noRoot = new String(noRoot); // equals but not ==
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
