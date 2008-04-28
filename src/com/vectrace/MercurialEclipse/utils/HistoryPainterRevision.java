/*******************************************************************************
 * Copyright (c) 2007-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch	- implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.team.MercurialStatusCache;

/**
 * Stores revision information for History Painter revision node.
 * 
 * @author bastian
 * 
 */
public class HistoryPainterRevision implements
		Comparable<HistoryPainterRevision> {
	private static MercurialStatusCache cache = MercurialStatusCache
			.getInstance();
	private int lane;
	private int lanes;
	private ChangeSet changeSet;
	private List<HistoryPainterRevision> parents;
	private IResource resource;
	private List<HistoryPainterEdge>edgeStarts;
	private List<HistoryPainterEdge>edgeStops;

	/**
	 * Constructor. Builds subtree by adding parents.
	 * 
	 * @param changeSet
	 * @param parents
	 */
	public HistoryPainterRevision(IResource res, ChangeSet changeSet) {
		this.changeSet = changeSet;
		this.parents = new ArrayList<HistoryPainterRevision>();
		this.resource = res;
		List<String> list = RepositoryGraph.getParentsForResource(res,
				changeSet);
		for (String string : list) {
			ChangeSet parent = cache.getChangeSet(string);
			if (parent != null) {
				parents.add(new HistoryPainterRevision(res, parent));
			}
		}
		Collections.sort(parents);
		edgeStarts = new ArrayList<HistoryPainterEdge>();
		edgeStops = new ArrayList<HistoryPainterEdge>();
	}

	HistoryPainterRevision() {
	}

	/**
	 * @return the lanes
	 */
	public int getLanes() {
		return lanes;
	}

	/**
	 * @param lanes
	 *            the lanes to set
	 */
	public void setLanes(int lanes) {
		this.lanes = lanes;
	}

	/**
	 * @return the lane
	 */
	public int getLane() {
		return lane;
	}

	/**
	 * @param lane
	 *            the lane to set
	 */
	public void setLane(int lane) {
		this.lane = lane;
	}

	/**
	 * @return the changeSet
	 */
	public ChangeSet getChangeSet() {
		return changeSet;
	}

	/**
	 * @param changeSet
	 *            the changeSet to set
	 */
	public void setChangeSet(ChangeSet changeSet) {
		this.changeSet = changeSet;
	}

	public int compareTo(HistoryPainterRevision o) {
		// reverse order (312, 311, etc)
		return o.getChangeSet().getChangesetIndex()
				- this.getChangeSet().getChangesetIndex();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((changeSet == null) ? 0 : changeSet.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof HistoryPainterRevision))
			return false;
		final HistoryPainterRevision other = (HistoryPainterRevision) obj;
		if (changeSet == null) {
			if (other.changeSet != null)
				return false;
		} else if (!changeSet.equals(other.changeSet))
			return false;
		return true;
	}

	/**
	 * @return the parents
	 */
	public List<HistoryPainterRevision> getParents() {
		return parents;
	}

	/**
	 * @param parents
	 *            the parents to set
	 */
	public void setParents(List<HistoryPainterRevision> parents) {
		this.parents = parents;
	}

	@Override
	public String toString() {
		return resource.getName()
				+ ","
				+ changeSet.toString()
				+ ",Lane:"
				+ lane
				+ ",Lanes:"
				+ lanes
				+ ",Parents:"+parents.size();
	}

	/**
	 * @return the resource
	 */
	public IResource getResource() {
		return resource;
	}

	/**
	 * @param resource
	 *            the resource to set
	 */
	public void setResource(IResource resource) {
		this.resource = resource;
	}

	/**
	 * @return the edgeStarts
	 */
	public List<HistoryPainterEdge> getEdgeStarts() {
		return edgeStarts;
	}

	/**
	 * @param edgeStarts the edgeStarts to set
	 */
	public void setEdgeStarts(List<HistoryPainterEdge> edgeStarts) {
		this.edgeStarts = edgeStarts;
	}

	/**
	 * @return the edgeStops
	 */
	public List<HistoryPainterEdge> getEdgeStops() {
		return edgeStops;
	}

	/**
	 * @param edgeStops the edgeStops to set
	 */
	public void setEdgeStops(List<HistoryPainterEdge> edgeStops) {
		this.edgeStops = edgeStops;
	}
}
