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
import java.util.List;

import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.model.ChangeSet;

public class RepositoryGraph {


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
