/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * john	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team.cache;

import java.io.File;
import java.util.concurrent.ExecutionException;

import com.aragost.javahg.BaseRepository;
import com.aragost.javahg.Bundle;
import com.aragost.javahg.Repository;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgClients;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.utils.Pair;

/**
 * Cache for JavaHg Repositories.
 *
 * The simplest approach would be to keep soft references to the the Repository but
 * {@link LocalChangesetCache} may retain JHgChangeSets indefinitely. Instead:
 * <ul>
 * <li><b>For BaseRepositories:</b> Use weak references to the HgRoot. The BaseRepositories will
 * effectively be indefinitely retained.
 * <li><b>For Overlay repositories:</b> Use weak references to the Repository. The changesets for
 * overlay repositories are cached by {@link AbstractRemoteCache} but they seem to be cleaned up
 * quite eagerly.
 * </ul>
 */
public class CommandServerCache {

	private static final CommandServerCache instance = new CommandServerCache();

	private final LoadingCache<HgRoot, BaseRepository> baseCache = CacheBuilder.newBuilder()
			.weakKeys().removalListener(new Listener()).build(new BaseCacheLoader());

	private final LoadingCache<Pair<HgRoot, File>, Repository> overlayCache = CacheBuilder
			.newBuilder().weakValues().removalListener(new Listener())
			.build(new OverlayCacheLoader());

	private CommandServerCache() {
	}

	// operations

	/**
	 * Get a repository, possibly creating one if there's not one currently running.
	 *
	 * Note: caller is responsible for ensuring the hg executable is usable
	 *
	 * @param hgRoot
	 *            The root
	 * @return A new or cached repository object
	 */
	public Repository get(HgRoot hgRoot) {
		try {
			return baseCache.get(hgRoot);
		} catch (ExecutionException e) {
			MercurialEclipsePlugin.logError(e.getCause());
			return null;
		}
	}

	/**
	 * Get a repository, possibly creating one if there's not one currently running.
	 *
	 * Note: caller is responsible for ensuring the hg executable is usable
	 *
	 * @param hgRoot
	 *            The root
	 * @param bundleFile
	 *            The bundle file. May be null
	 * @return A new or cached repository object
	 */
	public Repository get(HgRoot hgRoot, File bundleFile) {
		if (bundleFile == null) {
			return get(hgRoot);
		}

		try {
			return overlayCache.get(new Pair<HgRoot, File>(hgRoot, bundleFile));
		} catch (ExecutionException e) {
			MercurialEclipsePlugin.logError(e.getCause());

			return null;
		}
	}

	/**
	 * Stop all command servers
	 */
	public void invalidateAll() {
		overlayCache.invalidateAll();
		baseCache.invalidateAll();
	}

	public static CommandServerCache getInstance() {
		return instance;
	}

	// inner types

	private final class OverlayCacheLoader extends CacheLoader<Pair<HgRoot, File>, Repository> {
		@Override
		public Repository load(Pair<HgRoot, File> key) throws Exception {
			return new Bundle((BaseRepository) get(key.a), key.b).getOverlayRepository();
		}
	}

	private final class BaseCacheLoader extends CacheLoader<HgRoot, BaseRepository> {
		@Override
		public BaseRepository load(HgRoot root) throws Exception {
			return Repository.open(HgClients.getRepoConfig(), root);
		}
	}

	private final class Listener implements RemovalListener<Object, Repository> {
		public void onRemoval(RemovalNotification<Object, Repository> notification) {
			notification.getValue().close();
		}
	}
}
