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

	public Repository get(HgRoot hgRoot) {
		return get(hgRoot, null);
	}

	public Repository get(HgRoot hgRoot, File bundleFile) {
		try {
			return cache.get(new Key(hgRoot, bundleFile)).get();
		} catch (ExecutionException e) {
			MercurialEclipsePlugin.logError(e.getCause());

			return null;
		}
	}

	protected Value create(Key key) {
		if (key.file == null) {
			return new RepoValue(Repository.open(HgClients.getRepoConfig(), key.root));
		}

		return new BundleValue(new Bundle((BaseRepository) get(key.root, null), key.file));
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

	private static class Key {
		public HgRoot root;
		public File file;

		public Key(HgRoot root, File file) {
			this.root = root;
			this.file = file;
		}

		/**
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((file == null) ? 0 : file.hashCode());
			result = prime * result + ((root == null) ? 0 : root.hashCode());
			return result;
		}

		/**
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			Key other = (Key) obj;
			if (file == null) {
				if (other.file != null) {
					return false;
				}
			} else if (!file.equals(other.file)) {
				return false;
			}
			if (root == null) {
				if (other.root != null) {
					return false;
				}
			} else if (!root.equals(other.root)) {
				return false;
			}
			return true;
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
