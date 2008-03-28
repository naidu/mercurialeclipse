/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     StefanC - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.dialogs;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

class UntrackedFilesFilter extends ViewerFilter 
  {
    public UntrackedFilesFilter() 
    {
      super();
    }

    /**
     * Filter out untracked files.
     */
    public boolean select(Viewer viewer, Object parentElement,Object element) 
    {
      if (element instanceof CommitResource) 
      {
        String str = ((CommitResource) element).getStatus();
        return str.startsWith(CommitDialog.FILE_UNTRACKED) != true;
      }
      return true;
    }
  }