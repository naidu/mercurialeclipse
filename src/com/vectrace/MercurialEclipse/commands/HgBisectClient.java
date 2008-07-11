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
    
    /**
     * Marks the specified changeset as a good revision. If no changeset is specified
     * bisect will use the "current" changeset.
     * 
     * @param repository the repository to bisect
     * @param the changeset to mark as good, or null for current
     * @return a message from the command
     * @throws HgException
     */
    public static String markGood(File repository, ChangeSet good)
            throws HgException {
        HgCommand cmd = new HgCommand("bisect", repository, true);
        cmd.addOptions("-g");
        if(good != null) {
            cmd.addOptions(getRevision(good));
        }
        return cmd.executeToString();
    }

    /**
     * Marks the specified changeset as a bad revivision. If no changeset is specified
     * bisect will use teh "current" changeset.
     * 
     * @param repository the repository to bisect
     * @param bad the changeset to mark as bad, or null for current
     * @return a message from the command
     * @throws HgException
     */
    public static String markBad(File repository, ChangeSet bad)
            throws HgException {
        HgCommand cmd = new HgCommand("bisect", repository, true);
        cmd.addOptions("-b");
        if(bad != null) {
            cmd.addOptions(getRevision(bad));
        }
        return cmd.executeToString();
    }

    /**
     * Resets the bisect status for this repository. This command will not update the repository
     * to the head.
     * @param repository the repository to reset bisect status for
     * @return
     * @throws HgException
     */
    public static String reset(File repository) throws HgException {
        HgCommand cmd = new HgCommand("bisect", repository, true);
        cmd.addOptions("-r");
        return cmd.executeToString();
    }
    
    /**
     * Checks if the repository is currently being bisected
     * @param repository
     * @return
     */
    public static boolean isBisecting(File repository) {
        try {
            File file = getStatusFile(repository);
            return file.exists();
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Gets a Status by Changeset map containing marked bisect statuses.
     * @param repository
     * @return
     * @throws HgException 
     */
    public static Map<String, Status> getBisectStatus(File repository) throws HgException {
        HashMap<String, Status> statusByRevision = new HashMap<String, Status>();
        if(!isBisecting(repository)) {
            return statusByRevision;
        }
        BufferedReader reader = null;
        try {
            File file = getStatusFile(repository);
            reader = new BufferedReader(new FileReader(file));
            String line = null;
            while(null != (line = reader.readLine())) {
                String[] statusChangeset = line.split("\\s");
                if(statusChangeset[0].equalsIgnoreCase("bad")) {
                    statusByRevision.put(statusChangeset[1].trim(), Status.BAD);
                } else {
                    statusByRevision.put(statusChangeset[1].trim(), Status.GOOD);
                }
            }
            
        } catch (IOException e) {
            MercurialEclipsePlugin.logError(e);  
            throw new HgException(e.getLocalizedMessage(),e);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {            
                MercurialEclipsePlugin.logError(e);
            }
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
