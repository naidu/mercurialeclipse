/*******************************************************************************
 * Copyright (c) 2013 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *******************************************************************************/
package com.vectrace.MercurialEclipse.exception;

import junit.framework.TestCase;

public class HgExceptionTests extends TestCase {
	public static void testConciseError() {

		assertEquals("no such repository foo", new HgException(
				"remote: mercurial-server: no such repository foo\n"
						+ "abort: no suitable response from remote hg!").getConciseMessage());

		assertEquals("no such repository foo\tba   r", new HgException(
				"remote: mercurial-server: no such repository foo\tba   r\n"
						+ "abort: no suitable response from remote hg!").getConciseMessage());

		assertEquals("no such repository foo", new HgException(
				"asasdasdasd\nremote: mercurial-server: no such repository foo\n"
						+ "abort: no suitable response from remote hg!").getConciseMessage());

		assertEquals("no suitable response from remote hg!", new HgException(
				"asasdasdasdasdasd mercurial-server: no such repository foo\n"
						+ "abort: no suitable response from remote hg!").getConciseMessage());
	}
}
