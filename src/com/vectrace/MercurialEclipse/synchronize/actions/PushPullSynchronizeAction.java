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

import com.vectrace.MercurialEclipse.synchronize.MercurialResourceVariant;

/**
 * Get action that appears in the synchronize view. It's main purpose is to
 * filter the selection and delegate its execution to the get operation.
 */
public class PushPullSynchronizeAction extends SynchronizeModelAction {

	private boolean update;
	private final boolean isPull;

    public PushPullSynchronizeAction(String text,
			ISynchronizePageConfiguration configuration) {
		super(text, configuration);
		this.isPull = true;
	}

	public PushPullSynchronizeAction(String text,
			ISynchronizePageConfiguration configuration,
			ISelectionProvider selectionProvider, boolean isPull, boolean update) {
		super(text, configuration, selectionProvider);
		this.isPull = isPull;
		this.update = update;
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
		int revision = 0;
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
                if (object instanceof MercurialResourceVariant) {
                   revision = Math.max(revision, ((MercurialResourceVariant)object).getRev().getRevision());
                }
            }
        }
		IResource[] resources = new IResource[selectedResources.size()];
		selectedResources.toArray(resources);
		return new PushPullSynchronizeOperation(configuration, elements,
				resources, revision > 0 ? Integer.toString(revision) : null, isPull, update);
	}

    @Override
    protected boolean updateSelection(IStructuredSelection selection) {
        boolean updateSelection = super.updateSelection(selection);
        if(!updateSelection){
            if(ISynchronizePageConfiguration.INCOMING_MODE == getConfiguration().getMode()) {
                return isPull;
            } else if (ISynchronizePageConfiguration.OUTGOING_MODE == getConfiguration().getMode()) {
                return !isPull;
            } else if (ISynchronizePageConfiguration.BOTH_MODE == getConfiguration().getMode()) {
                return true;
            }
        }
        return updateSelection;
    }

    public boolean isPull() {
        return isPull;
    }
}
