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

import org.eclipse.core.resources.IResource;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.text.FileTextSearchScope;
import org.eclipse.search.ui.text.TextSearchQueryProvider;
import org.eclipse.ui.IWorkingSet;

/**
 * @author Bastian
 *
 */
public class MercurialTextSearchQueryProvider extends TextSearchQueryProvider {

	/**
	 *
	 */
	public MercurialTextSearchQueryProvider() {
		// TODO Auto-generated constructor stub
	}

	/*
	 * (non-Javadoc)
	 *
	 * @seeorg.eclipse.search.ui.text.TextSearchQueryProvider#createQuery(
	 * TextSearchInput)
	 */
	@Override
	public ISearchQuery createQuery(TextSearchInput input) {
		FileTextSearchScope scope = input.getScope();
		String text = input.getSearchText();
		boolean regEx = input.isRegExSearch();
		boolean caseSensitive = input.isCaseSensitiveSearch();
		return new MercurialTextSearchQuery(text, regEx, caseSensitive, scope);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.search.ui.text.TextSearchQueryProvider#createQuery(java.lang
	 * .String)
	 */
	@Override
	public ISearchQuery createQuery(String searchForString) {
		FileTextSearchScope scope = FileTextSearchScope.newWorkspaceScope(
				getPreviousFileNamePatterns(), false);
		return new MercurialTextSearchQuery(searchForString, false, true, scope);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.search.ui.text.TextSearchQueryProvider#createQuery(java.lang
	 * .String, org.eclipse.core.resources.IResource[])
	 */
	@Override
	public ISearchQuery createQuery(String selectedText, IResource[] resources) {
		FileTextSearchScope scope = FileTextSearchScope.newSearchScope(
				resources, getPreviousFileNamePatterns(), false);
		return new MercurialTextSearchQuery(selectedText, false, true, scope);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.search.ui.text.TextSearchQueryProvider#createQuery(java.lang
	 * .String, org.eclipse.ui.IWorkingSet[])
	 */
	@Override
	public ISearchQuery createQuery(String selectedText, IWorkingSet[] ws) {
		FileTextSearchScope scope = FileTextSearchScope.newSearchScope(ws,
				getPreviousFileNamePatterns(), false);
		return new MercurialTextSearchQuery(selectedText, false, true, scope);
	}

	private String[] getPreviousFileNamePatterns() {
		return new String[] { "*" }; //$NON-NLS-1$
	}
}
