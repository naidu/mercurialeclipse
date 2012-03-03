/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Andrei Loskutov         - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team.cache;

import java.io.File;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.aragost.javahg.Changeset;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.model.FileStatus;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.model.JHgChangeSet;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * Branch specific collection of remote data (changesets) for one remote repository.
 * This data can be queried by project.
 * <p>
 * Additionally, we should think if it may contain project unbound repository data (e.g.
 * files which are not located under any Eclipse project area).
 * @author Andrei
 */
public class RemoteData {

	private final Direction direction;
	private final SortedSet<ChangeSet> changesets;
	private final RemoteKey key;

	/**
	 * @param changesets this map contains AT LEAST a key corresponding to the hgroot of
	 * this data, and may also contain additional keys for one or more projects under
	 * the given hgroot.
	 * @param branch can be null (means all branches)
	 */
	public RemoteData(RemoteKey key, Direction direction, List<Changeset> changesets, File bundleFile) {
		super();

		this.direction = direction;
		this.changesets = new TreeSet<ChangeSet>();
		this.key = key;

		for (int i = 0, n = changesets.size(); i < n; i++) {
			this.changesets.add(new JHgChangeSet(key.getRoot(), changesets.get(i), key.getRepo(), direction, bundleFile));
		}
	}

	// operations

	public SortedSet<ChangeSet> getChangeSets(IResource resource){
		if (resource instanceof IProject) {
			return changesets;
		}

		SortedSet<ChangeSet> filtered = new TreeSet<ChangeSet>();
		IPath rootRelative = null;
		mainLoop: for (ChangeSet cs : changesets) {
			if (rootRelative == null) {
				File path = ResourceUtils.getFileHandle(resource);
				if (path == null || path.getPath().length() == 0) {
					return changesets;
				}
				rootRelative = new Path(cs.getHgRoot().toRelative(path));
				if (rootRelative.isEmpty()) {
					// hg root: return everything
					return changesets;
				}
			}
			List<FileStatus> files = cs.getChangedFiles();
			for (FileStatus fs : files) {
				if (rootRelative.equals(fs.getRootRelativePath())) {
					filtered.add(cs);
					continue mainLoop;
				}
			}
		}
		return filtered;
	}

	/**
	 * @return ALL changesets known by the hg root, or empty set, never null
	 */
	public SortedSet<ChangeSet> getChangeSets(){
		return changesets;
	}

	public IHgRepositoryLocation getRepo() {
		return key.getRepo();
	}

	public HgRoot getRoot() {
		return key.getRoot();
	}

	/**
	 * @return specific branch or null if the changesets are not limited by the branch
	 */
	public String getBranch() {
		return key.getBranch();
	}

	public Direction getDirection(){
		return direction;
	}

	public RemoteKey getKey(){
		return key;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RemoteData [");
		if (direction != null) {
			builder.append("direction=");
			builder.append(direction);
			builder.append(", ");
		}
		if (key != null) {
			builder.append("key=");
			builder.append(key);
		}
		builder.append("]");
		return builder.toString();
	}
}
