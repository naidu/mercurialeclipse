/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * stefanc	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import junit.framework.TestCase;

/**
 * @author stefanc
 *
 */
public class HgIdentClientTest extends TestCase {

    public void testGetCurrentChangesetId() throws Exception {
        AbstractShellCommand.setConsole(new PrintStream(new OutputStream() {
            @Override
            public void write(int b) throws IOException {
            }} ));
        File repo = new File(".");
        String id = HgIdentClient.getCurrentChangesetId(repo);
        System.out.println(id);
    }

}
