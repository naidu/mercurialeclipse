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
import java.util.ArrayList;
import java.util.List;

import com.vectrace.MercurialEclipse.model.HgRoot;

/**
 * @author stefanc
 *
 */
public class HgCommitClientTest extends AbstractCommandTest {

    /* (non-Javadoc)
     * @see com.vectrace.MercurialEclipse.commands.AbstractCommandTest#testCreateRepo()
     */
    public void testCommitSimpleMessage() throws Exception {
        File root = getRepository();
        File newFile = new File(root.getAbsolutePath() + File.separator + "dummy.txt");
        assertTrue("Unable to create commit file", newFile.createNewFile());
        addToRepository(newFile);
        HgRoot hgroot = new HgRoot(root.getAbsolutePath());
        List<File> files = new ArrayList<File>();
        files.add(newFile);
        HgCommitClient.commit(hgroot, files, "Simple", "the message");
    }
    /* (non-Javadoc)
     * @see com.vectrace.MercurialEclipse.commands.AbstractCommandTest#testCreateRepo()
     */
    public void testCommitMessageWithQuote() throws Exception {
        File root = getRepository();
        File newFile = new File(root.getAbsolutePath() + File.separator + "dummy.txt");
        assertTrue("Unable to create commit file", newFile.createNewFile());

        addToRepository(newFile);
        
        HgRoot hgroot = new HgRoot(root.getAbsolutePath());
        List<File> files = new ArrayList<File>();
        files.add(newFile);
        HgCommitClient.commit(hgroot, files, "Trasan 'O Banarne", "is this message \" really escaped?");
        HgCommitClient.commit(hgroot, files, "Simple", "the message");
    }
}
