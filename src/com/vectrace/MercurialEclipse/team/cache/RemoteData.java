/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Andrei Loskutov (Intland) - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team.cache;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
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

	private static final SortedSet<ChangeSet> EMPTY_SETS = Collections
			.unmodifiableSortedSet(new TreeSet<ChangeSet>());
	private final HgRoot root;
	private final HgRepositoryLocation repo;
	private final String branch;
	private final Map<IProject, ProjectCache> projectMap;
	private final Direction direction;

	public RemoteData(HgRepositoryLocation repo, HgRoot root, Direction direction, String branch) {
		super();
		this.repo = repo;
		this.root = root;
		this.direction = direction;
		this.branch = branch;
		projectMap = new HashMap<IProject, ProjectCache>();
	}

	public SortedSet<ChangeSet> getChangeSets(IResource resource){
		if(resource == null){
			return EMPTY_SETS;
		}
		IProject project = resource.getProject();
		ProjectCache cache = projectMap.get(project);
		if(cache == null || cache.isEmpty()) {
			return EMPTY_SETS;
		}
		return cache.getChangesets();
	}

	public boolean isValid(IProject project){
		return projectMap.containsKey(project);
	}

	public boolean clear(IProject project){
		ProjectCache removed = projectMap.remove(project);
		return removed != null && !removed.isEmpty();
	}

	public boolean clear(){
		boolean changed = !projectMap.isEmpty();
		projectMap.clear();
		return changed;
	}

	public Set<IProject> getRelatedProjects(){
		return ResourceUtils.getProjects(root);
	}

	public HgRepositoryLocation getRepo() {
		return repo;
	}

	public HgRoot getRoot() {
		return root;
	}

	public String getBranch() {
		return branch;
	}

	public Direction getDirection(){
		return direction;
	}
}
