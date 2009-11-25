/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * adam.berkes	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.actions;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.ui.actions.DeleteResourceAction;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.repository.actions.HgAction;

/**
 * @author adam.berkes <adam.berkes@intland.com>
 */
public class DeleteAction extends DeleteResourceAction {
	private final ISynchronizePageConfiguration configuration;
	private final List<IResource> resources;

	public DeleteAction(ISynchronizePageConfiguration configuration) {
		super(configuration.getSite().getWorkbenchSite());
		resources = new LinkedList<IResource>();
		this.configuration = configuration;
	}

	@Override
	public void run() {
		initResources(configuration.getSite().getSelectionProvider().getSelection());
		if (resources.size() > 0) {
			for (IResource res : resources) {
				try {
					res.delete(false, new NullProgressMonitor());
				} catch (CoreException ex) {
					MercurialEclipsePlugin.logError(ex);
				}
			}
		}
	}

	protected void initResources(ISelection selection) {
		resources.clear();
		if (selection instanceof IStructuredSelection) {
		for (Object sel : ((IStructuredSelection)selection).toArray()) {
			Object adapter = HgAction.getAdapter(sel,
					IResource.class);
			if (adapter != null && adapter instanceof IResource) {
				resources.add((IResource)adapter);
			}
		}
		}
	}
}
