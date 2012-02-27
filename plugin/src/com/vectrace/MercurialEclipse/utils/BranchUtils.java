/*******************************************************************************
 * Copyright (c) 2005-2008 Bastian Doetsch and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov - bug fixes
 *     John Peberdy    - refactoring
 *******************************************************************************/
package com.vectrace.MercurialEclipse.utils;


/**
 * Utilities for branch names
 */
public class BranchUtils {

	public static final String DEFAULT = "default";

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
}
