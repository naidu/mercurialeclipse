/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Andrei	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.actions;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;

import com.vectrace.MercurialEclipse.SafeUiJob;
import com.vectrace.MercurialEclipse.team.ActionRevert;

/**
 * @author Andrei
 *
 */
public class RevertSynchronizeOperation extends SynchronizeModelOperation {
    private final IResource[] resources;

    public RevertSynchronizeOperation(
            ISynchronizePageConfiguration configuration,
            IDiffElement[] elements, IResource[] resources) {
        super(configuration, elements);
        this.resources = resources;
    }

    public void run(IProgressMonitor monitor) throws InvocationTargetException,
            InterruptedException {
        monitor.beginTask("Loading Revert Window...", 1);
        new SafeUiJob("Reverting selected resources...") {

            @Override
            protected IStatus runSafe(IProgressMonitor moni) {
                final ActionRevert revert = new ActionRevert();
                revert.selectionChanged(null, new StructuredSelection(resources));
                revert.run(null);
                return super.runSafe(moni);
            }

        }.schedule();
        monitor.done();
    }

}
