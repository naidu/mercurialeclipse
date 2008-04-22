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
 *     Stefan C                  - Code cleanup
 *******************************************************************************/
package com.vectrace.MercurialEclipse.history;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.ui.history.HistoryPageSource;
import org.eclipse.ui.part.Page;

import com.vectrace.MercurialEclipse.team.MercurialStatusCache;

/**
 * @author zingo
 *
 */
public class MercurialHistoryPageSource extends HistoryPageSource
{
   MercurialHistoryProvider fileHistoryProvider;
   
  public MercurialHistoryPageSource(MercurialHistoryProvider fileHistoryProvider)
  {
    super();
//    System.out.println("MercurialHistoryPageSource::MercurialHistoryPageSource()");
    this.fileHistoryProvider = fileHistoryProvider;
  }

  /* (non-Javadoc)
   * @see org.eclipse.team.ui.history.IHistoryPageSource#canShowHistoryFor(java.lang.Object)
   */
  public boolean canShowHistoryFor(Object object)
  {
	if (object instanceof IResource) {
			return MercurialStatusCache.getInstance().isSupervised(
					(IResource) object);
	}
	return true;
	
  }

  /* (non-Javadoc)
   * @see org.eclipse.team.ui.history.IHistoryPageSource#createPage(java.lang.Object)
   */
  public Page createPage(Object object)
  {
//    System.out.println("MercurialHistoryPageSource::createPage()");
//    if (object instanceof IResource && ((IResource) object).getType() == IResource.FILE) 
//    {
      return new MercurialHistoryPage((IResource) object);
//    }
//    return null;
  }

}
