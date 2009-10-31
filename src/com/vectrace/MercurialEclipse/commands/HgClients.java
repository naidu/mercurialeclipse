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

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IResource;
import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * @author Stefan
 *
 */
public final class HgClients {

    private static IConsole console;
    private static IConfiguration config;
    private static IErrorHandler error;

    public static void initialize(IConsole c, IErrorHandler errorHandler,
            IConfiguration configuration) {
        HgClients.config = configuration;
        HgClients.console = c;
        HgClients.error = errorHandler;
    }

    public static String getExecutable() {
        if (config == null) {
            throw new IllegalStateException(
                    Messages.getString("HgClients.error.notInitializedWithConfig")); //$NON-NLS-1$
        }
        return config.getExecutable();
    }

    public static String getDefaultUserName() {
        if (config == null) {
            throw new IllegalStateException(
                    Messages.getString("HgClients.error.notInitializedWithConfig")); //$NON-NLS-1$
        }
        return config.getDefaultUserName();
    }

    public static IConsole getConsole() {
        if (console == null) {
            throw new IllegalStateException(
                    Messages.getString("HgClients.error.notInitializedWithConsole")); //$NON-NLS-1$
        }
        return console;
    }

    public static void logError(IOException e) {
        error.logError(e);
    }

    public static void logWarning(String message, Throwable e) {
        error.logWarning(message, e);
        MercurialEclipsePlugin.logWarning(message, e);
    }

    public static int getTimeOut(String commandId) {
        return config.getTimeOut(commandId);
    }

    public static String getPreference(String preferenceConstant,
            String defaultIfNotSet) {
        return config.getPreference(preferenceConstant, defaultIfNotSet);
    }

    public static HgRoot getHgRoot(File file) {
        return config.getHgRoot(file);
    }

    public static HgRoot getHgRoot(IResource resource) {
        return config.getHgRoot(ResourceUtils.getFileHandle(resource));
    }
}
