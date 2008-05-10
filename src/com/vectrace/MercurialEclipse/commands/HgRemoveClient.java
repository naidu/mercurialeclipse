/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;

public class HgRemoveClient {

    public static void removeResource(IResource resource,
            IProgressMonitor monitor) throws HgException {
        if (monitor != null) {
            monitor.subTask("Removing " + resource.getName()
                    + " from repository");
        }
        HgCommand command = new HgCommand("remove", resource.getProject(), true);
        command.addOptions("--force");
        command.addFiles(resource);
        command.setUsePreferenceTimeout(MercurialPreferenceConstants.RemoveTimeout);
        command.executeToBytes();
    }
}
