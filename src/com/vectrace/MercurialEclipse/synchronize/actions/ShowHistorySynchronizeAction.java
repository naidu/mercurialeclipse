/*******************************************************************************
 * Copyright (c) 2004, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 *     Bastian Doetsch				- Adaption to Mercurial
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.ui.synchronize.ISynchronizeModelElement;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelAction;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;

/**
 * Get action that appears in the synchronize view. It's main purpose is to
 * filter the selection and delegate its execution to the get operation.
 */
public class ShowHistorySynchronizeAction extends SynchronizeModelAction {

	public ShowHistorySynchronizeAction(String text,
			ISynchronizePageConfiguration configuration) {
		super(text, configuration);
	}

	public ShowHistorySynchronizeAction(String text,
			ISynchronizePageConfiguration configuration,
			ISelectionProvider selectionProvider) {
		super(text, configuration, selectionProvider);
	}

	@Override
	protected SynchronizeModelOperation getSubscriberOperation(
			ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		List<IResource> selectedResources = new ArrayList<IResource>(
				elements.length);
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] instanceof ISynchronizeModelElement) {
				selectedResources.add(((ISynchronizeModelElement) elements[i])
						.getResource());
			}
		}
        // XXX currently I have no idea why IDiffElement[] elements is empty...
        if(selectedResources.size() == 0){
            IStructuredSelection sel = getStructuredSelection();
            Object[] objects = sel.toArray();
            for (Object object : objects) {
                if (object instanceof IResource) {
                    selectedResources.add(((IResource) object));
                } else if (object instanceof IAdaptable){
                    IAdaptable adaptable = (IAdaptable) object;
                    IResource resource = (IResource) adaptable.getAdapter(IResource.class);
                    if(resource != null){
                        selectedResources.add(resource);
                    }
                }
            }
        }
		IResource[] resources = new IResource[selectedResources.size()];
		selectedResources.toArray(resources);
		return new ShowHistorySynchronizeOperation(configuration, elements,
				resources);
	}

    @Override
    protected boolean updateSelection(IStructuredSelection selection) {
        boolean updateSelection = super.updateSelection(selection);
        if(!updateSelection){
            // TODO implement constraints check here
            return true;
        }
        return updateSelection;
    }
}
