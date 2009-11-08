/*******************************************************************************
 * Copyright (c) 2007-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch	- implementation
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.utils;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;

/**
 * Stores revision information for History Painter revision node.
 *
 * @author bastian
 *
 */
public class HistoryPainterRevision implements Comparable<HistoryPainterRevision> {
	private int lane;
	private int lanes;
	private ChangeSet changeSet;
	private List<HistoryPainterRevision> parents;
	private IResource resource;
	private static final LocalChangesetCache LOCAL_CACHE = LocalChangesetCache.getInstance();

	/**
	 * Constructor. Builds subtree by adding parents.
	 */
	public HistoryPainterRevision(IResource res, ChangeSet changeSet) {
		this.changeSet = changeSet;
		this.parents = new ArrayList<HistoryPainterRevision>();
		this.resource = res;
		List<String> list = RepositoryGraph.getParentsForResource(res, changeSet);
		for (String string : list) {
			ChangeSet parent = LOCAL_CACHE.getChangesetById(res.getProject(), string);
			if (parent != null) {
				parents.add(new HistoryPainterRevision(res, parent));
			}
		}
	}

	HistoryPainterRevision() {
	}

	public int getLanes() {
		return lanes;
	}

	public void setLanes(int lanes) {
		this.lanes = lanes;
	}

	public int getLane() {
		return lane;
	}

	public void setLane(int lane) {
		this.lane = lane;
	}

	public ChangeSet getChangeSet() {
		return changeSet;
	}

	public void setChangeSet(ChangeSet changeSet) {
		this.changeSet = changeSet;
	}

	public int compareTo(HistoryPainterRevision o) {
		// reverse order (312, 311, etc)
		return o.getChangeSet().getChangesetIndex()
				- this.getChangeSet().getChangesetIndex();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((changeSet == null) ? 0 : changeSet.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof HistoryPainterRevision)) {
			return false;
		}
		final HistoryPainterRevision other = (HistoryPainterRevision) obj;
		if (changeSet == null) {
			if (other.changeSet != null) {
				return false;
			}
		} else if (!changeSet.equals(other.changeSet)) {
			return false;
		}
		return true;
	}

	public List<HistoryPainterRevision> getParents() {
		return parents;
	}

	public void setParents(List<HistoryPainterRevision> parents) {
		this.parents = parents;
	}

	@Override
	public String toString() {
		return resource.getName()
				+ "," //$NON-NLS-1$
				+ changeSet.toString()
				+ ",Lane:" //$NON-NLS-1$
				+ lane
				+ ",Lanes:" //$NON-NLS-1$
				+ lanes
				+ ",Parents:"+parents.size(); //$NON-NLS-1$
	}

	public IResource getResource() {
		return resource;
	}

	public void setResource(IResource resource) {
		this.resource = resource;
	}


}
