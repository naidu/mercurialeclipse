/*******************************************************************************
 * Copyright (c) 2000, 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Juerg Billeter, juergbi@ethz.ch - 47136 Search view should show match objects
 *     Ulrich Etter, etteru@ethz.ch - 47136 Search view should show match objects
 *     Roman Fuchs, fuchsro@ethz.ch - 47136 Search view should show match objects
 *     Bastian Doetsch - Adaptation for MercurialEclipse
 *******************************************************************************/
package com.vectrace.MercurialEclipse.search;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.Match;

import com.vectrace.MercurialEclipse.model.HgFile;

public class MercurialTextSearchTreeContentProvider implements ITreeContentProvider,
		IMercurialTextSearchContentProvider {

	private static final Object[] EMPTY_ARR = new Object[0];

	private AbstractTextSearchResult fResult;
	private final MercurialTextSearchResultPage fPage;
	private final AbstractTreeViewer fTreeViewer;
	private Map<Object, Set<Object>> fChildrenMap;

	MercurialTextSearchTreeContentProvider(MercurialTextSearchResultPage page,
			AbstractTreeViewer viewer) {
		fPage = page;
		fTreeViewer = viewer;
	}

	public Object[] getElements(Object inputElement) {
		Object[] children = getChildren(inputElement);
		int elementLimit = getElementLimit();
		if (elementLimit != -1 && elementLimit < children.length) {
			Object[] limitedChildren = new Object[elementLimit];
			System.arraycopy(children, 0, limitedChildren, 0, elementLimit);
			return limitedChildren;
		}
		return children;
	}

	private int getElementLimit() {
		return fPage.getElementLimit().intValue();
	}

	public void dispose() {
		// nothing to do
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput instanceof MercurialTextSearchResult) {
			initialize((MercurialTextSearchResult) newInput);
		}
	}

	private synchronized void initialize(AbstractTextSearchResult result) {
		fResult = result;
		fChildrenMap = new HashMap<Object, Set<Object>>();
		addMatches(result);
	}

	/**
	 * @param result
	 * @param showLineMatches
	 */
	private void addMatches(AbstractTextSearchResult result) {
		boolean showLineMatches = !((MercurialTextSearchQuery) fResult.getQuery())
				.isFileNameSearch();

		if (result != null && showLineMatches) {
			Object[] elements = result.getElements();
			for (int i = 0; i < elements.length; i++) {
				Match[] matches = result.getMatches(elements[i]);
				for (int j = 0; j < matches.length; j++) {
					MercurialMatch m = (MercurialMatch) matches[j];
					insert(m, false, m);
				}
			}
		}
	}

	private void insert(Object child, boolean refreshViewer, MercurialMatch mrs) {
		Object parent = getParent(child, mrs);
		while (parent != null) {
			if (insertChild(parent, child)) {
				if (refreshViewer) {
					fTreeViewer.add(parent, child);
				}
			} else {
				if (refreshViewer) {
					fTreeViewer.refresh(parent);
				}
				return;
			}
			child = parent;
			parent = getParent(child, mrs);
		}
		if (insertChild(fResult, child)) {
			if (refreshViewer) {
				fTreeViewer.add(fResult, child);
			}
		}
	}

	/**
	 * Adds the child to the parent.
	 *
	 * @param parent
	 *            the parent
	 * @param child
	 *            the child
	 * @return <code>true</code> if this set did not already contain the specified element
	 */
	private boolean insertChild(Object parent, Object child) {
		Set<Object> children = fChildrenMap.get(parent);
		if (children == null) {
			children = new HashSet<Object>();
			fChildrenMap.put(parent, children);
		}
		return children.add(child);
	}

	public Object[] getChildren(Object parentElement) {
		Set<Object> children = fChildrenMap.get(parentElement);
		if (children == null) {
			return EMPTY_ARR;
		}
		return children.toArray();
	}

	public boolean hasChildren(Object element) {
		return getChildren(element).length > 0;
	}

	/**
	 * @see com.vectrace.MercurialEclipse.search.IMercurialTextSearchContentProvider#elementsChanged(java.lang.Object[])
	 */
	public synchronized void elementsChanged(Object[] updatedElements) {
		for (int i = 0; i < updatedElements.length; i++) {
			if (updatedElements[i] instanceof HgFile) {
				addMatches(fResult);
				fTreeViewer.refresh();
			}
		}
	}

	public void clear() {
		initialize(fResult);
		fTreeViewer.refresh();
	}

	public static Object getParent(Object element, MercurialMatch mrs) {
		if (element instanceof IProject) {
			return null;
		}
		if (element instanceof IResource) {
			IResource resource = (IResource) element;
			return resource.getParent();
		}
		if (element instanceof HgFile) {
			return mrs.getFile();
		}
		if (element instanceof MercurialMatch) {
			return mrs.getHgFile();
		}
		return null;
	}

	public Object getParent(Object element) {
		return null;
	}
}
