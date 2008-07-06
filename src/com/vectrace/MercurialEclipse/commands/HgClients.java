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

import java.io.IOException;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;


/**
 * @author Stefan
 *
 */
public final class HgClients {

    private static IConsole console;
    private static IConfiguration config;
    private static IErrorHandler error;

    public static void initialize(IConsole c, 
            IErrorHandler errorHandler, 
            IConfiguration configuration) {
                HgClients.console = c;
                HgClients.error = errorHandler;
                HgClients.config = configuration;
        
    }
    
    /**
     * @return
     */
    public static String getExecutable() {
        if(config == null) {
            throw new IllegalStateException("HgClient has not been initialized with a configuration");
        }
        return config.getExecutable();
    }

    /**
     * @return
     */
    public static String getDefaultUserName() {
        if(config == null) {
            throw new IllegalStateException("HgClient has not been initialized with a configuration");
        }
        return config.getDefaultUserName();
    }

    /**
     * @return
     */
    public static IConsole getConsole() {
        if(console == null) {
            throw new IllegalStateException("HgClients has not been initialized with a console");
        }
        return console;
    }

    /**
     * @param e
     */
    public static void logError(IOException e) {
        error.logError(e);
    }

    /**
     * @param string
     * @param e
     */
    public static void logWarning(String message, Throwable e) {
        error.logWarning(message, e);
        MercurialEclipsePlugin.logWarning(message, e);
    }

    /**
     * @param timeoutConstant
     * @return
     */
    public static int getTimeOut(String commandId) {
        return config.getTimeOut(commandId);
    }
}
