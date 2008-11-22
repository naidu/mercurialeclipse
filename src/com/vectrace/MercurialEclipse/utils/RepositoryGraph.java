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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.history.MercurialRevision;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.team.cache.AbstractCache;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;

public class RepositoryGraph {
	private static RepositoryGraph instance = null;

	private Map<IResource, Map<String, ChangeSetNode>> revMap = new HashMap<IResource, Map<String, ChangeSetNode>>();

	// private SortedSet<ChangeSet> resChangeSets;
	private static AbstractCache cache = MercurialStatusCache
			.getInstance();

	private RepositoryGraph() {
	}

	public static RepositoryGraph getInstance() {
		if (instance == null) {
			instance = new RepositoryGraph();
		}
		return instance;
	}

	public ChangeSetNode getSubgraph(MercurialRevision base) {
		// get corresponding changeset
		ChangeSet cs = cache.getChangeSet(base.getChangeSet().toString());
		Map<String, ChangeSetNode> nodes = revMap.get(base.getResource());
		if (nodes == null || nodes.size() == 0) {
			nodes = new HashMap<String, ChangeSetNode>();
			revMap.put(base.getResource(), nodes);
			ChangeSetNode baseNode = traverseGraph(base.getResource(), null, cs);
			nodes.put(cs.getChangeset(), baseNode);
			return baseNode;
		}
		return nodes.get(cs.getChangeset());
	}

	private ChangeSetNode traverseGraph(IResource res, ChangeSetNode father,
			ChangeSet cs) {
		if (cs == null) {
			throw new RuntimeException(
					Messages.getString("RepositoryGraph.changesetWasNull")); //$NON-NLS-1$
		}
		Map<String, ChangeSetNode> nodes = revMap.get(res);

		ChangeSetNode csn = null;

		// if we have this node, return
		if (nodes.containsKey(cs.getChangeset())) {
			return csn;
		}

		// determine parent
		List<String> parents = getParentsForResource(res, cs);

		// new node
		csn = new ChangeSetNode(cs, new ArrayList<ChangeSetNode>(),
				new ArrayList<ChangeSetNode>());

		if (father != null) {
			csn.getIncomingEdges().add(father);
		}

		nodes.put(cs.getChangeset(), csn);

		// walk tree by recursively getting nodes for parent changesets
		Set<ChangeSetNode> edges = new TreeSet<ChangeSetNode>();
		for (int parCount = 0; parents != null && parCount < parents.size(); parCount++) {
			ChangeSet parent = cache.getChangeSet(cs.getParents()[parCount]);

			if (parent == null) {
				// break recursion, we're at the bottom
				continue;
			}

			// recursive call, get edges
			ChangeSetNode edge = traverseGraph(res, father, parent);
			edges.add(edge);
		}
		csn.getOutgoingEdges().addAll(edges);
		return csn;
	}

	/**
	 * @param res
	 * @param cs
	 * @return
	 */
	public static List<String> getParentsForResource(IResource res, ChangeSet cs) {
		List<String> parents = new ArrayList<String>(2);
		if (cs.getParents() != null) {
			for (String string : parents) {
			    // a non filled parent slot is reported by log --debug as "-1:0000000000"
                if (string.charAt(0)!='-') {
                    parents.add(string);
                }
            }
		}
		return parents;
	}
}
