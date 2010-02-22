/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.search;

import org.eclipse.core.resources.IFile;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.IFileMatchAdapter;
import org.eclipse.search.ui.text.Match;

/**
 * @author Bastian
 *
 */
public class MercurialFileMatchAdapter implements IFileMatchAdapter {

	/**
	 *
	 */
	public MercurialFileMatchAdapter() {
		// TODO Auto-generated constructor stub
	}

	public Match[] computeContainedMatches(AbstractTextSearchResult result,
			IFile file) {
		// TODO Auto-generated method stub
		return null;
	}

	public IFile getFile(Object element) {
		return null;
	}

}
