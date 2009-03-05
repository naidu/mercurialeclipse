/*******************************************************************************
 * Copyright (c) 2006-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre - implementation
 *     Bastian Doetsch
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

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
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Object selectionObject = ((EvaluationContext) event
                .getApplicationContext()).getDefaultVariable();
        selection = new ArrayList<IResource>();
        if (selectionObject != null && selectionObject instanceof List) {
            List list = (List) selectionObject;
            for (Object listEntry : list) {
                if (listEntry != null && listEntry instanceof IAdaptable) {
                    IAdaptable selectionAdaptable = (IAdaptable) listEntry;
                    selection.add((IResource) selectionAdaptable
                            .getAdapter(IResource.class));
                }
            }
        }
        if (selection.size() == 0) {
            selection.add(ResourceUtils.getActiveResourceFromEditor());
        }

        try {
            run(getSelectedResources());
        } catch (Exception e) {
            MessageDialog
                    .openError(
                            getShell(),
                            Messages
                                    .getString("MultipleResourcesHandler.hgSays"), e.getMessage() + Messages.getString("MultipleResourcesHandler.seeErrorLog")); //$NON-NLS-1$ //$NON-NLS-2$
            throw new ExecutionException(e.getMessage(), e);
        }
        return null;
    }

    protected IProject ensureSameProject() throws HgException {
        List<IResource> resources = getSelectedResources();
        final IProject project = resources.get(0).getProject();
        for (IResource res : resources) {
            if (!res.getProject().equals(project)) {
                throw new HgException(
                        Messages
                                .getString("MultipleResourcesHandler.allResourcesMustBeInSameProject")); //$NON-NLS-1$
            }
        }
        return project;
    }

    protected abstract void run(List<IResource> resources) throws Exception;
}