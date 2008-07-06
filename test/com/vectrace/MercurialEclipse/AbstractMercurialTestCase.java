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
package com.vectrace.MercurialEclipse;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import com.vectrace.MercurialEclipse.commands.HgClients;
import com.vectrace.MercurialEclipse.commands.TestConfiguration;

/**
 * Base class for test cases
 * 
 * @author bastian
 */
public abstract class AbstractMercurialTestCase extends TestCase {
    public static final List<String> CLONE_REPO_TEST1_CMD = new ArrayList<String>();
    public static final List<String> CLONE_REPO_TEST2_CMD = new ArrayList<String>();
    public static final String TEST1_LOCAL_NAME = "mercurialeclipse_tests_TEST1";
    public static final String TEST2_LOCAL_NAME = "mercurialeclipse_tests_TEST2";
    public static final String TEST1_REPO = "http://freehg.org/u/bastiand/mercurialeclipse_test1/";
    public static final String TEST2_REPO = "http://freehg.org/u/bastiand/mercurialeclipse_test2/";

    static {
        CLONE_REPO_TEST1_CMD.add("hg");
        CLONE_REPO_TEST1_CMD.add("clone");
        CLONE_REPO_TEST1_CMD.add(TEST1_REPO);
        CLONE_REPO_TEST1_CMD.add(TEST1_LOCAL_NAME);

        CLONE_REPO_TEST2_CMD.add("hg");
        CLONE_REPO_TEST2_CMD.add("clone");
        CLONE_REPO_TEST2_CMD.add(TEST2_REPO);
        CLONE_REPO_TEST2_CMD.add(TEST2_LOCAL_NAME);
    }

    /**
     * 
     */
    public AbstractMercurialTestCase() {
    }

    /**
     * @param name
     * @throws IOException
     * @throws InterruptedException
     */
    public AbstractMercurialTestCase(String name) throws IOException,
            InterruptedException {
        super(name);
    }

    static public boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return (path.delete());
    }

    /**
     * @throws IOException
     * @throws InterruptedException
     */
    protected String executeCommand(List<String> cmd) throws IOException,
            InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(cmd);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        LineNumberReader err = new LineNumberReader(new InputStreamReader(
                process.getInputStream()));
        int ret = process.waitFor();
        String result = "";
        String line = err.readLine();
        while (line != null) {
            result += "\n" + line;
            line = err.readLine();
        }
        if (ret != 0) {
            throw new RuntimeException(
                    "Cannot clone test repository. Err-Output:".concat(result));
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        TestConfiguration cfg = new TestConfiguration();
        HgClients.initialize(cfg, cfg, cfg);
        // clean up test1 repo
        deleteDirectory(new File(TEST1_LOCAL_NAME));
        // clean up test2 repo
        deleteDirectory(new File(TEST2_LOCAL_NAME));
        // set up test repository 1
        String result = executeCommand(CLONE_REPO_TEST1_CMD);
        System.out.println(result);
        // set up test repository 2
        result = executeCommand(CLONE_REPO_TEST2_CMD);
        System.out.println(result);
    }

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        // clean up test1 repo
        deleteDirectory(new File(TEST1_REPO));
        // clean up test2 repo
        deleteDirectory(new File(TEST2_REPO));
    }

}
