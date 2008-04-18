/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jérôme Nègre              - implementation
 *     Stefan C                  - Code cleanup
 *     Bastian Doetsch			 - extracted class since I need it for sync
 *******************************************************************************/
package com.vectrace.MercurialEclipse.storage;

import org.eclipse.core.resources.IProject;

import com.vectrace.MercurialEclipse.commands.HgLogClient;
import com.vectrace.MercurialEclipse.commands.HgParentClient;
import com.vectrace.MercurialEclipse.commands.HgTagClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.Tag;

public abstract class DataLoader {
	ChangeSet[] changeSets;

	public abstract IProject getProject();

	public abstract ChangeSet[] getRevisions() throws HgException;

	public Tag[] getTags() throws HgException {
		return HgTagClient.getTags(getProject());
	}

	public ChangeSet[] getHeads() throws HgException {
		return HgLogClient.getHeads(getProject());
	}

	public int[] getParents() throws HgException {
		return HgParentClient.getParents(getProject());
	}

	/**
	 * Searches for the complete changeset associated with a revision. This
	 * method must be called after getRevisions().
	 * 
	 * @param revision
	 * @return
	 */
	public ChangeSet getChangeSetByRevision(int revision) {
		for (ChangeSet changeSet : changeSets) {
			if (changeSet.getRevision().getRevision() == revision) {
				return changeSet;
			}
		}
		return null;
	}

	/**
	 * Searches for the complete changeset associated with the given string in
	 * tag, node, and node short entries. This method must be called after
	 * getRevisions().
	 * 
	 * @param tagOrNode
	 * @return
	 */
	public ChangeSet searchChangeSet(String tagOrNode) {
		for (ChangeSet changeSet : changeSets) {
			if (changeSet.getTag().equals(tagOrNode)
					|| changeSet.getChangeset().equals(tagOrNode)
					|| changeSet.getNodeShort().equals(tagOrNode)) {
				return changeSet;
			}
		}
		return null;
	}

	/**
	 * Gets the revision, which was tagged with the tag.
	 * 
	 * @param tag
	 * @return
	 */
	public ChangeSet getChangeSetByTag(Tag tag) {
		for (int i = 0; i < changeSets.length; i++) {
			ChangeSet cs = changeSets[i];
			if (cs.getTag().equals(tag.getName())
					|| cs.getChangeset().equals(tag.getGlobalId())
					|| cs.getChangesetIndex() == tag.getRevision()) {
				return cs;
			} 
			if (cs.getChangesetIndex() > tag.getRevision()) {
				if (i > 0) {
					return changeSets[i - 1];
				}
				return changeSets[0];				
			}
		}
		return null;
	}

}