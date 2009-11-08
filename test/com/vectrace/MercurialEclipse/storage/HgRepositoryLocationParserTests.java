/*******************************************************************************
 * Copyright (c) 2009 Intland.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Adam Berkes (Intland) - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.storage;

import java.io.File;
import java.util.Date;

import junit.framework.TestCase;

import com.vectrace.MercurialEclipse.exception.HgException;

/**
 * Unit test for parsing/creating hg repository representation
 * @author adam.berkes <adam.berkes@intland.com>
 */
public class HgRepositoryLocationParserTests extends TestCase {

	public void testParseLine() throws Exception {
		parseLine(false);
	}

	public void testParseLineEncrypted() throws Exception {
		parseLine(true);
	}

	public void testParseLineWithProject() throws Exception {
		final String uri = "http://javaforge.com/hg/hgeclipse";
		final String user = "test";
		final String password = "test";
		final String alias = "default";
		final String project = "hgeclipse";
		final Date date = new Date();
		HgRepositoryLocation location = HgRepositoryLocationParser.parseLine(createTestLine(true, date, uri, user, password, alias, project, true));
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
		final String user = "test";
		final String password = "test";
		final String alias = "default";
		final Date date = new Date();
		HgRepositoryLocation location = new HgRepositoryLocation(alias, false, repo, user, password);
		location.setLastUsage(date);
		String repoLine = HgRepositoryLocationParser.createLine(location);
		assertNotNull(repoLine);
		assertTrue(repoLine.length() > 0);
		assertEquals(createTestLine(location.isPush(), location.getLastUsage(), location.getLocation(), location.getUser(),
				location.getPassword(), location.getLogicalName(), location.getProjectName(), true), repoLine);
	}

	public void testCreateLineWithProject() throws Exception {
		final String repo = "http://javaforge.com/hg/hgeclipse";
		final String user = "test";
		final String password = "test";
		final String alias = "default";
		final String project = "hgeclipse";
		final Date date = new Date();
		HgRepositoryLocation location = new HgRepositoryLocation(alias, false, repo, user, password);
		location.setLastUsage(date);
		location.setProjectName(project);
		String repoLine = HgRepositoryLocationParser.createLine(location);
		assertNotNull(repoLine);
		assertTrue(repoLine.length() > 0);
		assertEquals(createTestLine(location.isPush(), location.getLastUsage(), location.getLocation(), location.getUser(),
				location.getPassword(), location.getLogicalName(), location.getProjectName(), true), repoLine);
	}

	public void testParseCreateLineLocalWinOld() throws Exception {
		final String uri = "C:\\Documents and settings\\workspace\\hgeclipse";
		final String alias = "default";
		final String user = "test";
		final String password = "test";
		String saveString = null;
		try {
			HgRepositoryLocation location = HgRepositoryLocationParser.parseLine(alias, true, uri, user, password);
			assertNotNull(location);
			assertTrue(location.isPush());
			assertEquals(null, location.getLastUsage());
			assertEquals(uri, location.getLocation());
			assertEquals(user, location.getUser());
			assertEquals(password, location.getPassword());
			assertEquals(alias, location.getLogicalName());
			assertEquals(null, location.getProjectName());
			saveString = HgRepositoryLocationParser.createSaveString(location);
		} catch(HgException ex) {
			if (File.pathSeparator.equals("\\")) {
				assertTrue(ex.getMessage(), false);
			}
			return;
		}
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

	private void parseLine(boolean toEncryptAuth) throws Exception {
		final String uri = "http://javaforge.com/hg/hgeclipse";
		final String user = "test";
		final String password = "test";
		final String alias = "default";
		final Date date = new Date();
		HgRepositoryLocation location = HgRepositoryLocationParser.parseLine(createTestLine(true, date, uri, user, password, alias, null, toEncryptAuth));
		assertNotNull(location);
		assertTrue(location.isPush());
		assertEquals(date, location.getLastUsage());
		assertEquals(uri, location.getLocation());
		assertEquals(user, location.getUser());
		assertEquals(password, location.getPassword());
		assertEquals(alias, location.getLogicalName());
		assertNull(location.getProjectName());
	}

	private String createTestLine(boolean isPush, Date date, String uri, String user, String password, String alias, String project, boolean toEncryptAuth) {
		HgRepositoryAuthCrypter crypter = HgRepositoryAuthCrypterFactory.create();
		StringBuilder line = new StringBuilder(isPush ? "u" : "d");
		line.append(date.getTime());
		line.append(HgRepositoryLocationParser.PART_SEPARATOR);
		line.append(uri.length());
		line.append(HgRepositoryLocationParser.PART_SEPARATOR);
		line.append(uri);
		line.append(HgRepositoryLocationParser.PART_SEPARATOR);
		if (toEncryptAuth) {
			user = HgRepositoryLocationParser.ENCRYPTED_PREFIX + HgRepositoryLocationParser.PART_SEPARATOR + crypter.encrypt(user);
		}
		line.append(user.length());
		line.append(HgRepositoryLocationParser.PART_SEPARATOR);
		line.append(user);
		line.append(HgRepositoryLocationParser.PART_SEPARATOR);
		if (toEncryptAuth) {
			password = HgRepositoryLocationParser.ENCRYPTED_PREFIX + HgRepositoryLocationParser.PART_SEPARATOR + crypter.encrypt(password);
		}
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
