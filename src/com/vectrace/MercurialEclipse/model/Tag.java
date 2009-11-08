/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	   Bastian	implementation
 *     Andrei Loskutov (Intland) - bug fixes
 *     Zsolt Koppany (Intland)   - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.model;

import com.vectrace.MercurialEclipse.HgRevision;

/**
 * @author <a href="mailto:zsolt.koppany@intland.com">Zsolt Koppany</a>
 * @version $Id$
 */
public class Tag implements Comparable<Tag> {
	private final static String TIP = HgRevision.TIP.getChangeset();

	/** name of the tag, unique in the repository */
	private final String name;
	private final int revision;
	private final String globalId;
	private final boolean local;

	public Tag(String name, int revision, String globalId, boolean local) {
		super();
		this.name = name;
		this.revision = revision;
		this.globalId = globalId;
		this.local = local;
	}

	public String getName() {
		return name;
	}

	public int getRevision() {
		return revision;
	}

	public String getGlobalId() {
		return globalId;
	}

	public boolean isLocal() {
		return local;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Tag other = (Tag) obj;
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return name + " [" + revision +  ':' + globalId + ']';
	}

	public int compareTo(Tag tag) {
		/* "tip" must be always the first in the collection */
		if (tag == null || name == null || TIP.equals(name)) {
			return -1;
		}

		if (TIP.equals(tag.getName())) {
			return 1;
		}

		int cmp = tag.getRevision() - revision;
		if(cmp != 0){
			// sort by revision first
			return cmp;
		}

		// sort by name
		cmp = name.compareToIgnoreCase(tag.getName());
		if (cmp == 0) {
			// Check it case sensitive
			cmp = name.compareTo(tag.getName());
		}
		return cmp;
	}
}
