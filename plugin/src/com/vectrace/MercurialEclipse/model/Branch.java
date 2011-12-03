/*******************************************************************************
 * Copyright (c) 2005-2008 Bastian Doetsch and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.model;

import java.util.Comparator;

public class Branch {

	public static final Comparator<Branch> COMPARATOR = new BranchComparator();

	public static final String DEFAULT = "default";

	/** name of the branch, unique in the repository */
	private final String name;
	private final int revision;
	private final String globalId;
	private final boolean active;

	public Branch(String name, int revision, String globalId, boolean active) {
		super();
		this.name = name;
		this.revision = revision;
		this.globalId = globalId;
		this.active = active;
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

	public boolean isActive() {
		return active;
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
		final Branch other = (Branch) obj;
		return same(name, other.name);
	}

	/**
	 * @param name1 may be null
	 * @param name2 may be null
	 * @return true if both names can represent same hg branch
	 */
	public static boolean same(String name1, String name2){
		if(name1 == null || name2 == null){
			return name1 == name2;
		}
		if(name1.equals(name2)){
			return true;
		}
		if(isDefault(name1) && isDefault(name2)){
			return true;
		}
		return false;
	}

	/**
	 * @param name may be null
	 * @return true if the given name matches the hg default branch name
	 * (this is also the case if given name is null)
	 */
	public static boolean isDefault(String name){
		return name == null || (name.length() == 0 || name.equals(DEFAULT));
	}

	private static class BranchComparator implements Comparator<Branch>
	{
		/**
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		public int compare(Branch o1, Branch o2) {
			return o2.getRevision() - o1.getRevision();
		}
	}
}
