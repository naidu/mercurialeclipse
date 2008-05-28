/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Stefan Chyssler	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;

/**
 * @author Stefan Chyssler
 * 
 */
public final class HgBisectClient {

    public enum Status { GOOD, BAD }
    
    public static String markGood(File repository, ChangeSet good)
            throws HgException {
        HgCommand cmd = new HgCommand("bisect", repository, true);
        cmd.addOptions("-g", getRevision(good));
        return cmd.executeToString();
    }

    public static String markBad(File repository, ChangeSet bad)
            throws HgException {
        HgCommand cmd = new HgCommand("bisect", repository, true);
        cmd.addOptions("-b", getRevision(bad));
        return cmd.executeToString();
    }

    public static String reset(File repository) throws HgException {
        HgCommand cmd = new HgCommand("bisect", repository, true);
        cmd.addOptions("-r");
        return cmd.executeToString();
    }
    
    public static boolean isBisecting(File repository) {
        try {
            File file = getStatusFile(repository);
            return file.exists();
        } catch (IOException e) {
            return false;
        }
    }
    
    public static Map<String, Status> getBisectStatus(File repository) {
        HashMap<String, Status> statusByRevision = new HashMap<String, Status>();
        if(!isBisecting(repository)) {
            return statusByRevision;
        }
        try {
            File file = getStatusFile(repository);
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line = null;
            while(null != (line = reader.readLine())) {
                String[] statusChangeset = line.split("\\s");
                if(statusChangeset[0].equalsIgnoreCase("bad")) {
                    statusByRevision.put(statusChangeset[1].trim(), Status.BAD);
                } else {
                    statusByRevision.put(statusChangeset[1].trim(), Status.GOOD);
                }
            }
            reader.close();
        } catch (IOException e) {
            MercurialEclipsePlugin.logError(e);
        }
        return statusByRevision;
    }

    private static File getStatusFile(File repository) throws IOException {
        String root = repository.getCanonicalPath();
        String bisectStatusFile = root + File.separator + ".hg" + File.separator + "bisect.state";
        File file = new File(bisectStatusFile);
        return file;
    }

    private static String getRevision(ChangeSet change) {
        return Integer.toString(change.getRevision().getRevision());
    }
}
