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
package com.vectrace.MercurialEclipse.ui;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import com.vectrace.MercurialEclipse.dialogs.CommitDialog;
import com.vectrace.MercurialEclipse.dialogs.CommitResource;

public class UntrackedFilesFilter extends ViewerFilter 
{
    private final boolean allowMissing;

    public UntrackedFilesFilter(boolean allowMissing) 
    {
        super();
        this.allowMissing = allowMissing;
    }

    /**
     * Filter out untracked files.
     */
    @Override
    public boolean select(Viewer viewer, Object parentElement,Object element) 
    {
        if (element instanceof CommitResource) 
        {
            String str = ((CommitResource) element).getStatus();
            if (str.startsWith(CommitDialog.FILE_UNTRACKED))
                return false;
            if (!allowMissing && str.startsWith(CommitDialog.FILE_DELETED))
                return false;
        }
        return true;
    }
}