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

import java.util.List;

import org.eclipse.core.resources.IProject;

import com.vectrace.MercurialEclipse.exception.HgException;

public class HgTransplantClient {

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
            String source, boolean branch) throws HgException {
        HgCommand command = new HgCommand("transplant", project, false);
        command.addOptions("--config extensions.transplant=");
        command.addOptions("--log");
        if (branch) {
            command.addOptions("-b");
        } else {
            command.addOptions("-s");
        }
        command.addOptions(source);
        if (nodeIds != null && nodeIds.size() > 0) {
            for (String node : nodeIds) {
                command.addOptions(node);
            }
        }
        return new String(command.executeToBytes());
    }
}
