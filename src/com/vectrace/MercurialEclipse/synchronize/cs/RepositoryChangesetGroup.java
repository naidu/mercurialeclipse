/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Soren Mathiasen (Schantz) - implementation
 *     Andrei Loskutov			 - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.cs;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.team.internal.core.subscribers.ChangeSet;

import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;

/**
 * @author Soren Mathiasen
 */
@SuppressWarnings("restriction")
public class RepositoryChangesetGroup implements IAdaptable {

	private final String name;
	private ChangesetGroup incoming;
	private ChangesetGroup outgoing;
	private final IHgRepositoryLocation location;
	private List<IProject> projects;

	public RepositoryChangesetGroup(String name, IHgRepositoryLocation location) {
		this.name = name;
		this.location = location;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RepositoryChangesetGroup [");
		if (name != null) {
			builder.append("name=");
			builder.append(name);
			builder.append(", ");
		}
		if (incoming != null && incoming.getChangesets().size() > 0) {
			for (ChangeSet set : incoming.getChangesets()) {
				builder.append(set.toString() + " ,");
			}
		}
		if (outgoing != null && outgoing.getChangesets().size() > 0) {
			for (ChangeSet set : outgoing.getChangesets()) {
				builder.append(set.toString() + " ,");
			}
		}

		builder.append("]");
		return builder.toString();
	}

	/**
	 * @param incoming
	 *            the incoming to set
	 */
	public void setIncoming(ChangesetGroup incoming) {
		this.incoming = incoming;
		incoming.setRepositoryChangesetGroup(this);
	}

	/**
	 * @return the incoming
	 */
	public ChangesetGroup getIncoming() {
		return incoming;
	}

	/**
	 * @param outgoing
	 *            the outgoing to set
	 */
	public void setOutgoing(ChangesetGroup outgoing) {
		this.outgoing = outgoing;
		outgoing.setRepositoryChangesetGroup(this);
	}

	/**
	 * @return the outgoing
	 */
	public ChangesetGroup getOutgoing() {
		return outgoing;
	}

	/**
	 * @return the location
	 */
	public IHgRepositoryLocation getLocation() {
		return location;
	}

	/**
	 * @param projects
	 */
	public void setProjects(List<IProject> projects) {
		this.projects = projects;

	}

	public List<IProject> getProjects() {
		return projects;
	}

	public Object getAdapter(Class adapter) {
		// Resource adapter is enabled for "working" changeset only to avoid "dirty"
		// decorations shown in the tree on changeset files from already commited changesets
		if(adapter == IResource.class && projects != null && projects.size() == 1){
			return projects.get(0);
		}
		if (adapter == HgRoot.class && location instanceof HgRoot) {
			return location;
		}
		if(adapter == ResourceMapping.class){
			return new HgChangeSetResourceMapping(this);
		}
		return null;
	}
}
