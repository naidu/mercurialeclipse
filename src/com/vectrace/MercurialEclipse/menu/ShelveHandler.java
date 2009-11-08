/*******************************************************************************
 * Copyright (c) 2005-2008 Bastian Doetsch and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IWorkbenchPart;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeWorkspaceJob;
import com.vectrace.MercurialEclipse.operations.ShelveOperation;

/**
 * @author bastian
 *
 */
public class ShelveHandler extends SingleResourceHandler {

	/* (non-Javadoc)
	 * @see com.vectrace.MercurialEclipse.menu.SingleResourceHandler#run(org.eclipse.core.resources.IResource)
	 */
	@Override
	protected void run(final IResource resource) throws Exception {
		new SafeWorkspaceJob(Messages.getString("ShelveHandler.Shelving")) { //$NON-NLS-1$
			/* (non-Javadoc)
			 * @see com.vectrace.MercurialEclipse.SafeWorkspaceJob#runSafe(org.eclipse.core.runtime.IProgressMonitor)
			 */
			@Override
			protected IStatus runSafe(IProgressMonitor monitor) {
				ShelveOperation op = new ShelveOperation(
						(IWorkbenchPart) null,
						resource.getProject());
				try {
					op.run(monitor);
					return super.runSafe(monitor);
				} catch (InvocationTargetException e) {
					return new Status(IStatus.ERROR, MercurialEclipsePlugin.ID,
							0, e.getLocalizedMessage(), e);
				} catch (InterruptedException e) {
					return new Status(IStatus.INFO, MercurialEclipsePlugin.ID,
							0, e.getLocalizedMessage(), e);
				}
			}
		}.schedule();

	}

}
