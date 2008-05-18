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
package com.vectrace.MercurialEclipse.commands;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.eclipse.core.resources.IProject;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

public class HgTransplantClient extends AbstractRepositoryClient {

    /**
     * Cherrypicks given ChangeSets from repository or branch.
     * 
     * @param project
     *            the project
     * @param nodeIds
     *            the changeset identifiers
     * @param source
     *            the branch or repository
     * @param branch
     *            flag, if we want to pick from a branch. true if branch, false
     *            if repo.
     * @throws HgException
     */
    public static String transplant(IProject project, List<String> nodeIds,
            HgRepositoryLocation loc, boolean branch,
            String branchName, boolean all, boolean merge, String mergeNodeId,
            boolean prune, String pruneNodeId, boolean continueLastTransplant,
            boolean filterChangesets, String filter) throws HgException {

        try {
            HgCommand command = new HgCommand("transplant", project, false);
            command.setUsePreferenceTimeout(MercurialPreferenceConstants.PULL_TIMEOUT);
            command.addOptions("--config extensions.transplant=");
            command.addOptions("--log");
            if (branch) {
                command.addOptions("--branch");
                command.addOptions(branchName);
                if (all) {
                    command.addOptions("--all");
                }
            } else {
                command.addOptions("--source");
                URI uri = getRepositoryURI(loc, loc.getUser(), loc.getPassword());
                command.addOptions(uri.toASCIIString());
            }
            
            if (prune) {
                command.addOptions("--prune");
                command.addOptions(pruneNodeId);
            }
            
            if (merge) {
                command.addOptions("--merge");
                command.addOptions(mergeNodeId);
            }

            if (nodeIds != null && nodeIds.size() > 0) {
                for (String node : nodeIds) {
                    command.addOptions(node);
                }
            }
            return new String(command.executeToBytes());
        } catch (URISyntaxException e) {
            throw new HgException("Invalid URI", e);
        }
    }
}
