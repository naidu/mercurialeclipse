/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * 
 * @author Jerome Negre <jerome+hg@jnegre.org>
 *
 */
public abstract class MultipleResourcesHandler extends AbstractHandler {

	private List<IResource> selection;

	protected Shell getShell() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
	}

	protected List<IResource> getSelectedResources() {
		return selection;
	}

    @SuppressWarnings("unchecked")
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        List<IAdaptable> selectionObject = (List<IAdaptable>)((EvaluationContext) event.getApplicationContext())
                .getDefaultVariable();
        try {
            this.selection = new ArrayList<IResource>();
            for(IAdaptable obj : selectionObject) {
                this.selection.add((IResource)obj.getAdapter(IResource.class));
            }
            run(getSelectedResources());
        } catch (Exception e) {
            MessageDialog.openError(getShell(), "Hg says...", e.getMessage()+"\nSee Error Log for more details.");
            throw new ExecutionException(e.getMessage(), e);
        }
        return null;
    }
	
		
	protected abstract void run(List<IResource> resources) throws Exception ;
}