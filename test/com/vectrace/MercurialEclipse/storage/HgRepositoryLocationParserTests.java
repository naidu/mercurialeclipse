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

import java.net.URI;
import java.util.Date;

import junit.framework.TestCase;

/**
 * @author adam.berkes
 *
 */
public class HgRepositoryLocationParserTests extends TestCase {

    public void testParseLine() throws Exception {
        final String uri = "http://javaforge.com/hg/hgeclipse";
        final String user = "test";
        final String password = "test";
        final String alias = "default";
        final Date date = new Date();
        HgRepositoryLocation location = HgRepositoryLocationParser.parseLine(createTestLine(true, date, uri, user, password, alias, null));
        assertNotNull(location);
        assertTrue(location.isPush());
        assertEquals(date, location.getLastUsage());
        assertEquals(uri, location.getLocation());
        assertEquals(user, location.getUser());
        assertEquals(password, location.getPassword());
        assertEquals(alias, location.getLogicalName());
        assertNull(location.getProjectName());
    }

    public void testParseLineWithProject() throws Exception {
        final String uri = "http://javaforge.com/hg/hgeclipse";
        final String user = "test";
        final String password = "test";
        final String alias = "default";
        final String project = "hgeclipse";
        final Date date = new Date();
        HgRepositoryLocation location = HgRepositoryLocationParser.parseLine(createTestLine(true, date, uri, user, password, alias, project));
        assertNotNull(location);
        assertTrue(location.isPush());
        assertEquals(date, location.getLastUsage());
        assertEquals(uri, location.getLocation());
        assertEquals(user, location.getUser());
        assertEquals(password, location.getPassword());
        assertEquals(alias, location.getLogicalName());
        assertEquals(project, location.getProjectName());
    }

    public void testCreateLine() throws Exception {
        final String repo = "http://javaforge.com/hg/hgeclipse";
        final URI uri = new URI(repo);
        final String user = "test";
        final String password = "test";
        final String alias = "default";
        final Date date = new Date();
        HgRepositoryLocation location = new HgRepositoryLocation(alias, false, uri, repo, user, password);
        location.setLastUsage(date);
        String repoLine = HgRepositoryLocationParser.createLine(location);
        assertNotNull(repoLine);
        assertTrue(repoLine.length() > 0);
        assertEquals(repoLine, createTestLine(location.isPush(), location.getLastUsage(), location.getLocation(), location.getUser(),
                location.getPassword(), location.getLogicalName(), location.getProjectName()));
    }

    public void testCreateLineWithProject() throws Exception {
        final String repo = "http://javaforge.com/hg/hgeclipse";
        final URI uri = new URI(repo);
        final String user = "test";
        final String password = "test";
        final String alias = "default";
        final String project = "hgeclipse";
        final Date date = new Date();
        HgRepositoryLocation location = new HgRepositoryLocation(alias, false, uri, repo, user, password);
        location.setLastUsage(date);
        location.setProjectName(project);
        String repoLine = HgRepositoryLocationParser.createLine(location);
        assertNotNull(repoLine);
        assertTrue(repoLine.length() > 0);
        assertEquals(createTestLine(location.isPush(), location.getLastUsage(), location.getLocation(), location.getUser(),
                location.getPassword(), location.getLogicalName(), location.getProjectName()), repoLine);
    }

    public void testParseCreateLineLocalWinOld() throws Exception {
        final String uri = "C:\\Documents and settings\\workspace\\hgeclipse";
        final String alias = "default";
        final String user = "test";
        final String password = "test";
        HgRepositoryLocation location = HgRepositoryLocationParser.parseLine(alias, true, uri, user, password);
        assertNotNull(location);
        assertTrue(location.isPush());
        assertEquals(null, location.getLastUsage());
        assertEquals(uri, location.getLocation());
        assertEquals(user, location.getUser());
        assertEquals(password, location.getPassword());
        assertEquals(alias, location.getLogicalName());
        assertEquals(null, location.getProjectName());
        String saveString = HgRepositoryLocationParser.createSaveString(location);
        assertNotNull(saveString);
        assertTrue(saveString.length() > 0);
        assertEquals(uri + HgRepositoryLocationParser.SPLIT_TOKEN + user +
                HgRepositoryLocationParser.PASSWORD_TOKEN + password + HgRepositoryLocationParser.ALIAS_TOKEN + alias, saveString);
    }

    public void testParseCreateLineLocalLinOld() throws Exception {
        final String uri = "/home/adam.berkes/workspace/hgeclipse";
        final String alias = "default";
        HgRepositoryLocation location = HgRepositoryLocationParser.parseLine(alias, true, uri, null, null);
        assertNotNull(location);
        assertTrue(location.isPush());
        assertEquals(null, location.getLastUsage());
        assertEquals(uri, location.getLocation());
        assertEquals(null, location.getUser());
        assertEquals(null, location.getPassword());
        assertEquals(alias, location.getLogicalName());
        assertEquals(null, location.getProjectName());
        String saveString = HgRepositoryLocationParser.createSaveString(location);
        assertNotNull(saveString);
        assertTrue(saveString.length() > 0);
        assertEquals(uri + HgRepositoryLocationParser.ALIAS_TOKEN + alias, saveString);
    }

    private String createTestLine(boolean isPush, Date date, String uri, String user, String password, String alias, String project) {
        StringBuilder line = new StringBuilder(isPush ? "u" : "d");
        line.append(date.getTime());
        line.append(HgRepositoryLocationParser.PART_SEPARATOR);
        line.append(uri.length());
        line.append(HgRepositoryLocationParser.PART_SEPARATOR);
        line.append(uri);
        line.append(HgRepositoryLocationParser.PART_SEPARATOR);
        line.append(user.length());
        line.append(HgRepositoryLocationParser.PART_SEPARATOR);
        line.append(user);
        line.append(HgRepositoryLocationParser.PART_SEPARATOR);
        line.append(password.length());
        line.append(HgRepositoryLocationParser.PART_SEPARATOR);
        line.append(password);
        line.append(HgRepositoryLocationParser.PART_SEPARATOR);
        line.append(alias.length());
        line.append(HgRepositoryLocationParser.PART_SEPARATOR);
        line.append(alias);
        if (project != null) {
            line.append(HgRepositoryLocationParser.PART_SEPARATOR);
            line.append(project.length());
            line.append(HgRepositoryLocationParser.PART_SEPARATOR);
            line.append(project);
        }
        return line.toString();
    }
}
