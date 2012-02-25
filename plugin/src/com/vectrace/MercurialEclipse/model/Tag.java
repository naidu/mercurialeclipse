/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	   Bastian	implementation
 *     Andrei Loskutov           - bug fixes
 *     Zsolt Koppany (Intland)   - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.model;

import org.eclipse.team.core.history.ITag;

import com.aragost.javahg.Changeset;
import com.vectrace.MercurialEclipse.HgRevision;
import com.vectrace.MercurialEclipse.properties.DoNotDisplayMe;

/**
 * @author Bastian
 */
public class Tag implements ITag, Comparable<Tag> {

	private static final String TIP = HgRevision.TIP.getNode();

	private final com.aragost.javahg.commands.Tag tag;

	public Tag(com.aragost.javahg.commands.Tag tag) {
		super();
		this.tag = tag;
	}

	public String getName() {
		return tag.getName();
	}

	private int getRevision() {
		return tag.getChangeset().getRevision();
	}

	public boolean isLocal() {
		return tag.isLocal();
	}

	public Changeset getChangeset() {
		return tag.getChangeset();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((tag == null) ? 0 : tag.hashCode());
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
		Tag other = (Tag) obj;
		if (tag == null) {
			if (other.tag != null) {
				return false;
			}
		} else if (!tag.equals(other.tag)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return tag.toString();
	}

	public int compareTo(Tag other) {
		/* "tip" must be always the first in the collection */
		if (other == null || getName() == null || isTip()) {
			return -1;
		}

		if (other.isTip()) {
			return 1;
		}

		int cmp = other.getRevision() - getRevision();
		if(cmp != 0){
			// sort by revision first
			return cmp;
		}

		// sort by name
		cmp = getName().compareToIgnoreCase(other.getName());
		if (cmp == 0) {
			// Check it case sensitive
			cmp = getName().compareTo(other.getName());
		}
		return cmp;
	}

	public boolean isTip(){
		return TIP.equals(getName());
	}

	public static Tag makeLight(String name, Changeset cs) {
		return new Tag(new com.aragost.javahg.commands.Tag(name, cs, false)) {
			/**
			 * @see com.vectrace.MercurialEclipse.model.Tag#isLocal()
			 */
			@Override
			@DoNotDisplayMe
			public boolean isLocal() {
				// TODO query for actual data
				throw new UnsupportedOperationException();
			}
		};
	}
}
