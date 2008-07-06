/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Stefan	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.PrintStream;

import junit.framework.TestCase;

/**
 * @author Stefan
 *
 */
public class TestConfiguration  extends TestCase implements IConsole, IErrorHandler, IConfiguration {

    /* (non-Javadoc)
     * @see com.vectrace.MercurialEclipse.commands.IConsole#getOutputStream()
     */
    public PrintStream getOutputStream() {
        return System.out;
    }

    /* (non-Javadoc)
     * @see com.vectrace.MercurialEclipse.commands.IErrorHandler#logError(java.lang.Throwable)
     */
    public void logError(Throwable e) {
        fail(e.getMessage());
    }

    /* (non-Javadoc)
     * @see com.vectrace.MercurialEclipse.commands.IErrorHandler#logWarning(java.lang.String, java.lang.Throwable)
     */
    public void logWarning(String message, Throwable e) {
        fail(e.getMessage());
    }

    /* (non-Javadoc)
     * @see com.vectrace.MercurialEclipse.commands.IConfiguration#getDefaultUserName()
     */
    public String getDefaultUserName() {
        return "foo";
    }

    /* (non-Javadoc)
     * @see com.vectrace.MercurialEclipse.commands.IConfiguration#getExecutable()
     */
    public String getExecutable() {
        String path = "c:/program/mercurial/hg.exe";
        // path = "hg";
        return path;
    }

    /* (non-Javadoc)
     * @see com.vectrace.MercurialEclipse.commands.IConfiguration#getTimeOut(java.lang.String)
     */
    public int getTimeOut(String commandId) {
        return 12000;
    }

    /* (non-Javadoc)
     * @see com.vectrace.MercurialEclipse.commands.IConsole#commandCompleted(int, java.lang.String, java.lang.Throwable)
     */
    public void commandCompleted(int exitCode, String message, Throwable error) {
        System.out.println(exitCode + " - " + message);
        if(error != null) {
            error.printStackTrace(System.out);
        }
    }

    /* (non-Javadoc)
     * @see com.vectrace.MercurialEclipse.commands.IConsole#commandInvoked(java.lang.String)
     */
    public void commandInvoked(String command) {
        System.out.println(command);
    }

    /* (non-Javadoc)
     * @see com.vectrace.MercurialEclipse.commands.IConsole#printError(java.lang.String, java.lang.Throwable)
     */
    public void printError(String message, Throwable root) {
        System.out.println(message);
        root.printStackTrace(System.out);
    }
}
