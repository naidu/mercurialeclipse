/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - impl
 *     Bastian Doetsch           - small changes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.FlaggedAdaptable;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;

public class HgIMergeClient extends AbstractClient {

    public static String merge(IProject project, String revision)
            throws HgException {
        HgCommand command = new HgCommand("imerge", project, false);
        command
                .setUsePreferenceTimeout(MercurialPreferenceConstants.IMERGE_TIMEOUT);
        command.addOptions("--config", "extensions.imerge=");
        if (revision != null) {
            command.addOptions("-r", revision);
        }
        return new String(command.executeToBytes());
    }

    public static List<FlaggedAdaptable> getMergeStatus(IResource res)
            throws HgException {
        HgCommand command = new HgCommand("imerge", getWorkingDirectory(res),
                false);
        command.addOptions("--config", "extensions.imerge=");
        command.addOptions("status");
        command
                .setUsePreferenceTimeout(MercurialPreferenceConstants.IMERGE_TIMEOUT);
        String[] lines = command.executeToString().split("\n");
        ArrayList<FlaggedAdaptable> result = new ArrayList<FlaggedAdaptable>();
        if (lines.length != 1 || !"all conflicts resolved".equals(lines[0])) {
            for (String line : lines) {
                FlaggedAdaptable flagged = new FlaggedAdaptable(res
                        .getProject().getFile(line.substring(2)), line
                        .charAt(0));
                result.add(flagged);
            }
        }
        return result;
    }

}
