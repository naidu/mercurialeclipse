package com.vectrace.MercurialEclipse.search;

import org.eclipse.jface.viewers.IContentProvider;

public interface IMercurialTextSearchContentProvider extends IContentProvider {

	public abstract void elementsChanged(Object[] updatedElements);

	public abstract void clear();

}