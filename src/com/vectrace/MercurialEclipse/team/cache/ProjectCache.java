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

import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.model.ChangeSet;

/**
 * A collection of changesets for the given project - branch pair
 * @author Andrei
 */
public class ProjectCache {

	private final IProject project;
	private final SortedSet<ChangeSet> changesets;
	private final String branch;

	public ProjectCache(IProject project, String branch) {
		this.project = project;
		this.branch = branch;
		this.changesets = new TreeSet<ChangeSet>();
	}

	public SortedSet<ChangeSet> getChangesets() {
		return changesets;
	}

	public SortedSet<ChangeSet> getChangesets(IResource resource) {
		// TODO sort out the changesets by a file.
		return changesets;
	}

	public String getBranch() {
		return branch;
	}

	public IProject getProject() {
		return project;
	}

	public boolean isEmpty(){
		return changesets.isEmpty();
	}
}
