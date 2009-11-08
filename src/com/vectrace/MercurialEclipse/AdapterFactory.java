/*******************************************************************************
 * Copyright (c) 2007-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Stefan Groschupf          - logError
 *******************************************************************************/
package com.vectrace.MercurialEclipse;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.team.ui.history.IHistoryPageSource;

import com.vectrace.MercurialEclipse.history.MercurialHistoryProvider;
import com.vectrace.MercurialEclipse.history.MercurialHistoryPageSource;

public class AdapterFactory implements IAdapterFactory
{

/* (non-Javadoc)
   * @see org.eclipse.core.runtime.IAdapterFactory#getAdapter(java.lang.Object, java.lang.Class)
   */
@SuppressWarnings("unchecked")
public MercurialHistoryPageSource getAdapter(Object adaptableObject, Class adapterType)
{
//    System.out.println("AdapterFactory::getAdapter()");

	if((adaptableObject instanceof MercurialHistoryProvider) && adapterType == IHistoryPageSource.class)
	{
//      System.out.println("AdapterFactory::getAdapter() MercurialHistoryPageSource");
	  return new MercurialHistoryPageSource((MercurialHistoryProvider)adaptableObject);
	}
	return null;
}

/* (non-Javadoc)
   * @see org.eclipse.core.runtime.IAdapterFactory#getAdapterList()
   */
@SuppressWarnings("unchecked")
public Class<IHistoryPageSource>[] getAdapterList()
{
//    System.out.println("AdapterFactory::getAdapterList()");
	return new Class[] { IHistoryPageSource.class };
}

}
