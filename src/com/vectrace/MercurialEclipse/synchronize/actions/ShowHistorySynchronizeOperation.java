/*******************************************************************************
 * Copyright (c) 2008 MercurialEclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch				- implementation
 *     Subclipse                    - original impl.o
 ******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;
import org.eclipse.ui.PartInitException;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;

public class ShowHistorySynchronizeOperation extends SynchronizeModelOperation {
	private IResource[] resources;

	public ShowHistorySynchronizeOperation(
			ISynchronizePageConfiguration configuration,
			IDiffElement[] elements, IResource[] resources) {
		super(configuration, elements);
		this.resources = resources;
	}

	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		monitor.beginTask("Loading History View...", 1);
		getShell().getDisplay().syncExec(new Runnable() {
			public void run() {
				IHistoryView view;
				try {
					view = (IHistoryView) getPart().getSite().getPage()
							.showView("org.eclipse.team.ui.GenericHistoryView");

					if (view != null) {
						view.showHistoryFor(resources[0]);
					}
				} catch (PartInitException e) {
					MercurialEclipsePlugin.logError(e);
				}
			}
		});
		monitor.done();
	}
}
