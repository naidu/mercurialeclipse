/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * lali	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.storage;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Repository location line format:
 * [u|d]<dateAsLong> <len> uri <len> username <len> password <len> alias/id[ <len> project]
 *
 * @author adam.berkes
 */
public class HgRepositoryLocationParser {

    protected static final String PART_SEPARATOR = " ";

    protected static HgRepositoryLocation parseLine(final String line) {
        if (line == null || line.length() < 1) {
            return null;
        }
        String repositoryLine = new String(line);
        //get direction indicator
        String direction = repositoryLine.substring(0,1);
        repositoryLine = repositoryLine.substring(1);
        try {
            //get date
            Date lastUsage = new Date(Long.valueOf(repositoryLine.substring(0, repositoryLine.indexOf(PART_SEPARATOR))).longValue());
            repositoryLine = repositoryLine.substring(repositoryLine.indexOf(PART_SEPARATOR) + 1);
            List<String> parts = new ArrayList<String>(5);
            while (repositoryLine != null && repositoryLine.length() > 0) {
                int len = Integer.valueOf(repositoryLine.substring(0, repositoryLine.indexOf(PART_SEPARATOR))).intValue();
                repositoryLine = repositoryLine.substring(repositoryLine.indexOf(PART_SEPARATOR) + 1);
                String partValue = repositoryLine.substring(0, len);
                repositoryLine = repositoryLine.substring(repositoryLine.length() > len ? len + 1 : repositoryLine.length());
                parts.add(partValue);
            }
            HgRepositoryLocation location = new HgRepositoryLocation(parts.get(3), direction.equals("u"), parts.get(0), parts.get(1), parts.get(2));
            location.setLastUsage(lastUsage);
            if (parts.size() > 4) {
                location.setProjectName(parts.get(4));
            }
            return location;
        } catch(Throwable th) {

            return null;
        }
    }

    protected static String createLine(final HgRepositoryLocation location) {
        StringBuilder line = new StringBuilder(location.isPush() ? "u" : "d");
        line.append(location.getLastUsage().getTime());
        line.append(PART_SEPARATOR);
        line.append(String.valueOf(location.getLocation().length()));
        line.append(PART_SEPARATOR);
        line.append(location.getLocation());
        line.append(PART_SEPARATOR);
        line.append(String.valueOf(location.getUser().length()));
        line.append(PART_SEPARATOR);
        line.append(location.getUser());
        line.append(PART_SEPARATOR);
        line.append(String.valueOf(location.getPassword().length()));
        line.append(PART_SEPARATOR);
        line.append(location.getPassword());
        line.append(PART_SEPARATOR);
        line.append(String.valueOf(location.getLogicalName().length()));
        line.append(PART_SEPARATOR);
        line.append(location.getLogicalName());
        if (location.getProjectName() != null) {
            line.append(PART_SEPARATOR);
            line.append(String.valueOf(location.getProjectName().length()));
            line.append(PART_SEPARATOR);
            line.append(location.getProjectName());
        }
        return line.toString();
    }
}
