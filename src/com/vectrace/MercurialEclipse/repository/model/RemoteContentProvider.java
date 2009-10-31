/*******************************************************************************
 * Copyright (c) 2003, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 *     Bastian Doetsch              - adaptation
 ******************************************************************************/
package com.vectrace.MercurialEclipse.repository.model;

import java.util.ArrayList;

import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.progress.DeferredTreeContentManager;

import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

/**
 * Extension to the generic workbench content provider mechanism to lazily
 * determine whether an element has children. That is, children for an element
 * aren't fetched until the user clicks on the tree expansion box.
 */
public class RemoteContentProvider extends WorkbenchContentProvider {

    private DeferredTreeContentManager manager;

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer,
     *      java.lang.Object, java.lang.Object)
     */
    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        if (viewer instanceof AbstractTreeViewer) {
            manager = new DeferredTreeContentManager(this,
                    (AbstractTreeViewer) viewer);
        }
        super.inputChanged(viewer, oldInput, newInput);
    }

    /*
     * (non-Javadoc) Method declared on WorkbenchContentProvider.
     */
    @Override
    public boolean hasChildren(Object element) {
        if (element == null) {
            return false;
        }

        if (manager != null) {
            if (manager.isDeferredAdapter(element))
                return manager.mayHaveChildren(element);
        }

        return super.hasChildren(element);
    }

    /**
     * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object[] getChildren(Object parentElement) {
		if (manager != null) {
			Object[] children = manager.getChildren(parentElement);
			if (children != null) {
				if (parentElement instanceof HgRepositoryLocation) {
					ArrayList childrenArray = new ArrayList();
					for (int i = 0; i < children.length; i++) childrenArray.add(children[i]);
					children = new Object[childrenArray.size()];
					childrenArray.toArray(children);
				}
				// This will be a placeholder to indicate
				// that the real children are being fetched
				return children;
			}
		}
		return super.getChildren(parentElement);
	}
        
    public void cancelJobs(HgRepositoryLocation[] roots) {
        if (manager != null) {
            for (int i = 0; i < roots.length; i++) {
                HgRepositoryLocation root = roots[i];
                cancelJobs(root);
            }
        }
    }

    /**
     * Cancel any jobs that are fetching content from the given location.
     * 
     * @param location
     */
    public void cancelJobs(HgRepositoryLocation location) {
        if (manager != null) {
            manager.cancel(location);
        }
    }
}
