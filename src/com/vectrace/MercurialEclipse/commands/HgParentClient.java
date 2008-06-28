/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.exception.HgException;

public class HgParentClient extends AbstractClient {

    private static final Pattern ANCESTOR_PATTERN = Pattern
            .compile("^([0-9]+):([0-9a-f]+)$");

    public static int[] getParents(IProject project) throws HgException {
        HgCommand command = new HgCommand("parents", project, false);
        command.addOptions("--template", "{rev}\n");
        String[] lines = command.executeToString().split("\n");
        int[] parents = new int[lines.length];
        for (int i = 0; i < lines.length; i++) {
            parents[i] = Integer.parseInt(lines[i]);
        }
        return parents;
    }

    public static String[] getParentNodeIds(IResource resource)
            throws HgException {
        HgCommand command = new HgCommand("parents",
                getWorkingDirectory(resource), false);
        command.addOptions("--template", "{node}\n");
        String[] lines = command.executeToString().split("\n");
        String[] parents = new String[lines.length];
        for (int i = 0; i < lines.length; i++) {
            parents[i] = lines[i].trim();
        }
        return parents;
    }

    public static int findCommonAncestor(IProject project, int r1, int r2)
            throws HgException {
        HgCommand command = new HgCommand("debugancestor", project, false);
        command.addOptions(Integer.toString(r1), Integer.toString(r2));
        String result = command.executeToString().trim();
        Matcher m = ANCESTOR_PATTERN.matcher(result);
        if (m.matches()) {
            return Integer.parseInt(m.group(1));
        }
        throw new HgException("Parse exception: '" + result + "'");
    }

    public static int findCommonAncestor(IResource resource, String node1, String node2)
            throws HgException {
        HgCommand command = new HgCommand("debugancestor", getWorkingDirectory(resource), false);
        command.addOptions(node1, node2);
        String result = command.executeToString().trim();
        Matcher m = ANCESTOR_PATTERN.matcher(result);
        if (m.matches()) {
            return Integer.parseInt(m.group(1));
        }
        throw new HgException("Parse exception: '" + result + "'");
    }

    public static String[] getParents(IResource rev, String node)
            throws HgException {
        HgCommand command = new HgCommand("parents", rev.getProject(), false);
        command.addOptions("--template", "{rev}:{node|short}\n");
        command.addOptions("-r", node);
        String[] lines = command.executeToString().split("\n");
        return lines;
    }

}
