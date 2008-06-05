/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands.mq;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.commands.HgCommand;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.Patch;

/**
 * @author bastian
 * 
 */
public class HgQSeriesClient extends AbstractClient {
    public static List<Patch> getPatchesInSeries(IResource resource)
            throws HgException {
        HgCommand command = new HgCommand("qseries",
                getWorkingDirectory(resource), true);
        command.addOptions("-v");
        command.addOptions("--summary");
        return parse(command.executeToString());
    }

    /**
     * @param executeToString
     * @return
     */
    public static List<Patch> parse(String executeToString) {
        List<Patch> list = new ArrayList<Patch>();
        if (executeToString != null && executeToString.indexOf("\n") >= 0) {
            String[] patches = executeToString.split("\n");
            for (String string : patches) {
                String[] components = string.split(":");
                String[] patchData = components[0].trim().split(" ");
                
                Patch p = new Patch();
                p.setIndex(patchData[0]);
                p.setApplied(patchData[1].equals("A") ? true : false);
                p.setName(patchData[2].trim());
                
                if (components.length>1) {
                    String summary = components[1].trim();
                    p.setSummary(summary);
                }
                
                list.add(p);
            }
        }
        return list;
    }

    public static List<Patch> getPatchesNotInSeries(IResource resource)
            throws HgException {
        HgCommand command = new HgCommand("qseries",
                getWorkingDirectory(resource), true);
        command.addOptions("--summary", "--missing");
        return parse(command.executeToString());
    }
}
