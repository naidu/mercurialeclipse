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
import java.util.concurrent.TimeUnit;

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
 * Cache for JavaHg Repositories
 */
public class CommandServerCache {
	private static final CommandServerCache instance = new CommandServerCache();

	private final LoadingCache<Key, Value> cache = CacheBuilder.newBuilder()
			.expireAfterAccess(5, TimeUnit.MINUTES).removalListener(new Listener())
			.build(new Loader());

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
		return get(hgRoot, null);
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
		try {
			return cache.get(new Key(hgRoot, bundleFile)).get();
		} catch (ExecutionException e) {
			MercurialEclipsePlugin.logError(e.getCause());

			return null;
		}
	}

	protected Value create(Key key) {
		if (key.getFile() == null) {
			return new RepoValue(Repository.open(HgClients.getRepoConfig(), key.getRoot()));
		}

		return new BundleValue(new Bundle((BaseRepository) get(key.getRoot(), null), key.getFile()));
	}

	/**
	 * Stop all command servers
	 */
	public void invalidateAll() {
		cache.invalidateAll();
	}

	public static CommandServerCache getInstance() {
		return instance;
	}

	// inner types

	private final class Loader extends CacheLoader<Key, Value> {
		@Override
		public Value load(Key key) {
			return create(key);
		}
	}

	private static class Listener implements RemovalListener<Key, Value> {
		public void onRemoval(RemovalNotification<Key, Value> notification) {
			notification.getValue().dispose();
		}
	}

	private static class Key extends Pair<HgRoot, File> {

		public Key(HgRoot root, File file) {
			super(root, file);
		}

		public HgRoot getRoot() {
			return a;
		}

		public File getFile() {
			return b;
		}
	}

	private interface Value {
		public Repository get();

		public void dispose();
	}

	private static class RepoValue implements Value {

		private final Repository repo;

		public RepoValue(Repository repo) {
			this.repo = repo;
		}

		/**
		 * @see com.vectrace.MercurialEclipse.team.cache.CommandServerCache.Value#get()
		 */
		public Repository get() {
			return repo;
		}

		/**
		 * @see com.vectrace.MercurialEclipse.team.cache.CommandServerCache.Value#dispose()
		 */
		public void dispose() {
			repo.close();
		}
	}

	private static class BundleValue implements Value {

		private final Bundle bundle;

		public BundleValue(Bundle bundle) {
			this.bundle = bundle;
		}

		/**
		 * @see com.vectrace.MercurialEclipse.team.cache.CommandServerCache.Value#get()
		 */
		public Repository get() {
			return bundle.getOverlayRepository();
		}

		/**
		 * @see com.vectrace.MercurialEclipse.team.cache.CommandServerCache.Value#dispose()
		 */
		public void dispose() {
			bundle.close();
		}
	}
}
