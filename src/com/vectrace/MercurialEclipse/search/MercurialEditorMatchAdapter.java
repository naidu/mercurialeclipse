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

import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.IEditorMatchAdapter;
import org.eclipse.search.ui.text.Match;
import org.eclipse.ui.IEditorPart;

/**
 * @author Bastian
 *
 */
public class MercurialEditorMatchAdapter implements IEditorMatchAdapter {

	/**
	 *
	 */
	public MercurialEditorMatchAdapter() {
		// TODO Auto-generated constructor stub
	}

	public Match[] computeContainedMatches(AbstractTextSearchResult result,
			IEditorPart editor) {
		return null;
	}

	public boolean isShownInEditor(Match match, IEditorPart editor) {
		// TODO Auto-generated method stub
		return false;
	}

}
