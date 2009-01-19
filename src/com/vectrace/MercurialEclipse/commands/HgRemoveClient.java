/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *     Bastian Doetsch           - removeResources()
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;

public class HgRemoveClient {

    public static void removeResource(IResource resource,
            IProgressMonitor monitor) throws HgException {
        if (monitor != null) {
            monitor.subTask(Messages.getString("HgRemoveClient.removeResource.1") + resource.getName() //$NON-NLS-1$
                    + Messages.getString("HgRemoveClient.removeResource.2")); //$NON-NLS-1$
        }
        HgCommand command = new HgCommand("remove", resource.getProject(), true); //$NON-NLS-1$
        command.addOptions("--force"); //$NON-NLS-1$
        command.addFiles(resource);
        command
                .setUsePreferenceTimeout(MercurialPreferenceConstants.REMOVE_TIMEOUT);
        command.executeToBytes();
    }

    /**
     * @param filesToRemove
     * @throws HgException 
     */
    public static void removeResources(List<IResource> resources) throws HgException {
        Map<HgRoot, List<IResource>> resourcesByRoot;
        try {
            resourcesByRoot = HgCommand.groupByRoot(resources);
        } catch (IOException e) {
            throw new HgException(e.getLocalizedMessage(), e);
        }
        for (Map.Entry<HgRoot, List<IResource>> mapEntry : resourcesByRoot.entrySet()) {
            HgRoot root = mapEntry.getKey();
            // if there are too many resources, do several calls
            int size = resources.size();
            int delta = AbstractShellCommand.MAX_PARAMS - 1;
            for (int i = 0; i < size; i += delta) {
                AbstractShellCommand command = new HgCommand("remove", root, //$NON-NLS-1$
                        true);
                command
                        .setUsePreferenceTimeout(MercurialPreferenceConstants.REMOVE_TIMEOUT);
                command.addFiles(mapEntry.getValue().subList(i,
                        Math.min(i + delta, size - i)));
                command.executeToBytes();
            }
        }

    }
}
