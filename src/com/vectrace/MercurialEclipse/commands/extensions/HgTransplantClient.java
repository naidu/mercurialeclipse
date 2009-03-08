/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch           - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands.extensions;

import java.net.URI;
import java.util.List;

import org.eclipse.core.resources.IProject;

import com.vectrace.MercurialEclipse.commands.AbstractShellCommand;
import com.vectrace.MercurialEclipse.commands.HgCommand;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

public class HgTransplantClient {

    /**
     * Cherrypicks given ChangeSets from repository or branch.
     */
    public static String transplant(IProject project, List<String> nodeIds,
            HgRepositoryLocation repo, boolean branch, String branchName,
            boolean all, boolean merge, String mergeNodeId, boolean prune,
            String pruneNodeId, boolean continueLastTransplant,
            boolean filterChangesets, String filter) throws HgException {

        AbstractShellCommand command = new HgCommand("transplant", project, false); //$NON-NLS-1$
        command
                .setUsePreferenceTimeout(MercurialPreferenceConstants.PULL_TIMEOUT);
        command.addOptions("--config", "extensions.hgext.transplant="); //$NON-NLS-1$ //$NON-NLS-2$
        if (continueLastTransplant) {
            command.addOptions("--continue"); //$NON-NLS-1$
        } else {
            command.addOptions("--log"); //$NON-NLS-1$
            if (branch) {
                command.addOptions("--branch"); //$NON-NLS-1$
                command.addOptions(branchName);
                if (all) {
                    command.addOptions("--all"); //$NON-NLS-1$
                }
            } else {
                command.addOptions("--source"); //$NON-NLS-1$
                URI uri = repo.getUri();
                if (uri != null ) {
                    command.addOptions(uri.toASCIIString());
                } else {
                    command.addOptions(repo.getLocation());
                }
            }

            if (prune) {
                command.addOptions("--prune"); //$NON-NLS-1$
                command.addOptions(pruneNodeId);
            }

            if (merge) {
                command.addOptions("--merge"); //$NON-NLS-1$
                command.addOptions(mergeNodeId);
            }

            if (nodeIds != null && nodeIds.size() > 0) {
                for (String node : nodeIds) {
                    command.addOptions(node);
                }
            }

            if (filterChangesets) {
                command.addOptions("--filter", filter); //$NON-NLS-1$
            }
        }
        return new String(command.executeToBytes());
    }
}
