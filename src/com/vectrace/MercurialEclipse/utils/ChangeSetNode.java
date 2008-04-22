/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.utils;

import java.util.List;

import com.vectrace.MercurialEclipse.model.ChangeSet;

/**
 * Represents a changeset in a graph
 * 
 * @author Bastian Doetsch
 * 
 */
public class ChangeSetNode implements Comparable<ChangeSetNode> {
	

	private ChangeSet changeset = null;
	private List<ChangeSetNode> outgoingEdges = null;
	private List<ChangeSetNode> incomingEdges = null;
	public ChangeSet getChangeset() {
		return changeset;
	}

	public void setChangeset(ChangeSet changeset) {
		this.changeset = changeset;
	}

	public ChangeSetNode(ChangeSet changeset, List<ChangeSetNode> incomingEdges, List<ChangeSetNode> outgoingEdges) {
		super();
		this.changeset = changeset;
		this.incomingEdges = incomingEdges;
		this.outgoingEdges = outgoingEdges;
	}

		

	public List<ChangeSetNode> getOutgoingEdges() {
		return outgoingEdges;
	}

	public void setOutgoingEdges(List<ChangeSetNode> edges) {
		this.outgoingEdges = edges;
	}

	@Override
	public String toString() {
		return changeset.toString();
	}

	public int compareTo(ChangeSetNode o) {
		return this.getChangeset().compareTo(o.getChangeset());
	}

	public List<ChangeSetNode> getIncomingEdges() {
		return incomingEdges;
	}

	public void setIncomingEdges(List<ChangeSetNode> incomingEdges) {
		this.incomingEdges = incomingEdges;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((changeset == null) ? 0 : changeset.hashCode());
		result = prime * result
				+ ((incomingEdges == null) ? 0 : incomingEdges.hashCode());
		result = prime * result
				+ ((outgoingEdges == null) ? 0 : outgoingEdges.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final ChangeSetNode other = (ChangeSetNode) obj;
		if (changeset == null) {
			if (other.changeset != null)
				return false;
		} else if (!changeset.equals(other.changeset))
			return false;
		if (incomingEdges == null) {
			if (other.incomingEdges != null)
				return false;
		} else if (!incomingEdges.equals(other.incomingEdges))
			return false;
		if (outgoingEdges == null) {
			if (other.outgoingEdges != null)
				return false;
		} else if (!outgoingEdges.equals(other.outgoingEdges))
			return false;
		return true;
	}

}
