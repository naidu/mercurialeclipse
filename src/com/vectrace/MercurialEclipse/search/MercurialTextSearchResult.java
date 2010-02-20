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

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.search.internal.ui.SearchPluginImages;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.IEditorMatchAdapter;
import org.eclipse.search.ui.text.IFileMatchAdapter;

/**
 * @author Bastian
 *
 */
@SuppressWarnings("restriction")
public class MercurialTextSearchResult extends AbstractTextSearchResult {

	private MercurialTextSearchQuery query;

	public MercurialTextSearchResult() {
		super();
	}

	/**
	 * @param mercurialTextSearchQuery
	 */
	public MercurialTextSearchResult(MercurialTextSearchQuery query) {
		this.query = query;
	}

	@Override
	public IEditorMatchAdapter getEditorMatchAdapter() {
		return new MercurialEditorMatchAdapter();
	}

	@Override
	public IFileMatchAdapter getFileMatchAdapter() {
		return new MercurialFileMatchAdapter();
	}

	public ImageDescriptor getImageDescriptor() {
		return SearchPluginImages.DESC_OBJ_TSEARCH_DPDN;
	}

	public String getLabel() {
		return query.getResultLabel(super.getMatchCount());
	}

	public ISearchQuery getQuery() {
		return this.query;
	}

	public String getTooltip() {
		return getLabel();
	}

}
