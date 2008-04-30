/*******************************************************************************
 * Copyright (c) 2003, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package com.vectrace.MercurialEclipse.repository.model;

 
import java.util.Arrays;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.repository.RepositoryComparator;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

/**
 * AllRootsElement is the model element for the repositories view.
 * Its children are the array of all known repository roots.
 * 
 * Because we extend IAdaptable, we don't need to register this adapter
 * as we need for RemoteFileElement, RemoteFolderElement ...
 */
public class AllRootsElement extends HgModelElement  implements IAdaptable  {
	public ImageDescriptor getImageDescriptor(Object object) {
		return null;
	}
	
	@Override
    public Object[] internalGetChildren(Object o, IProgressMonitor monitor) {
        HgRepositoryLocation[] locations =
            MercurialEclipsePlugin.getRepoManager().getAllRepoLocations().toArray(new HgRepositoryLocation[0]);
        Arrays.sort(locations, new RepositoryComparator());
        return locations; 	
    }
	
	public String getLabel(Object o) {
		return o.toString();
	}
	
	@SuppressWarnings("unchecked")
    @Override
    public Object getAdapter(Class adapter) {
		if (adapter == IWorkbenchAdapter.class) return this;
		return null;
	}
	public Object getParent(Object o) {
		return null;
	}
}

