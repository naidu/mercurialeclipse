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
package com.vectrace.MercurialEclipse;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.vectrace.MercurialEclipse.commands.IConfiguration;
import com.vectrace.MercurialEclipse.commands.IConsole;
import com.vectrace.MercurialEclipse.commands.IErrorHandler;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.views.console.HgConsole;
import com.vectrace.MercurialEclipse.views.console.HgConsoleFactory;

/**
 * @author Stefan
 * 
 */
public class DefaultConfiguration implements IConsole, IErrorHandler,
        IConfiguration {

    private HgConsole console;

    /**
     * 
     */
    public DefaultConfiguration() {
        console = HgConsoleFactory.getInstance().getConsole();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.vectrace.MercurialEclipse.commands.IConfiguration#getDefaultUserName
     * ()
     */
    public String getDefaultUserName() {
        return MercurialUtilities.getHGUsername(false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.vectrace.MercurialEclipse.commands.IConfiguration#getExecutable()
     */
    public String getExecutable() {
        if (!MercurialEclipsePlugin.getDefault().isHgUsable()) {
            MercurialUtilities.configureHgExecutable();
            MercurialEclipsePlugin.getDefault().checkHgInstallation();
        }

        return MercurialEclipsePlugin.getDefault().getPreferenceStore()
                .getString(MercurialPreferenceConstants.MERCURIAL_EXECUTABLE);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.vectrace.MercurialEclipse.commands.IConfiguration#getPreference(java
     * .lang.String, java.lang.String)
     */
    public String getPreference(String preferenceConstant,
            String defaultIfNotSet) {
        return MercurialUtilities.getPreference(preferenceConstant,
                defaultIfNotSet);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.vectrace.MercurialEclipse.commands.IErrorHandler#logError(java.lang
     * .Throwable)
     */
    public void logError(Throwable e) {
        MercurialEclipsePlugin.logError(e);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.vectrace.MercurialEclipse.commands.IErrorHandler#logWarning(java.
     * lang.String, java.lang.Throwable)
     */
    public void logWarning(String message, Throwable e) {
        MercurialEclipsePlugin.logWarning(message, e);

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.vectrace.MercurialEclipse.commands.IConfiguration#getTimeOut(java
     * .lang.String)
     */
    public int getTimeOut(String commandId) {
        int timeout = 12000;
        String pref = getPreference(commandId, String.valueOf(timeout));
        try {
            timeout = Integer.parseInt(pref);
            if (timeout < 0) {
                throw new NumberFormatException(Messages.getString("DefaultConfiguration.timoutLessThanEqual")); //$NON-NLS-1$
            }
        } catch (NumberFormatException e) {
            logWarning(Messages.getString("DefaultConfiguration.timeoutForCommand") + commandId //$NON-NLS-1$
                    + Messages.getString("DefaultConfiguration.notCorrectlyConfigured"), e); //$NON-NLS-1$
        }
        return timeout;
    }

    /*
     * ======================================================
     * 
     * IConsole methods below
     * 
     * ======================================================
     */

    /*
     * (non-Javadoc) exitcodes 0 == ok, 1 == warning, all other are errors
     * 
     * @see
     * com.vectrace.MercurialEclipse.commands.IConsole#commandCompleted(int,
     * java.lang.String, java.lang.Throwable)
     */
    public void commandCompleted(int exitCode, String message, Throwable error) {
        int severity = IStatus.OK;
        switch (exitCode) {
        case 0:
            severity = IStatus.OK;
            break;
        case 1:
            severity = IStatus.OK;
            break;
        default:
            severity = IStatus.ERROR;
        }
        console.commandCompleted(new Status(severity,
                MercurialEclipsePlugin.ID, message), error);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.vectrace.MercurialEclipse.commands.IConsole#commandInvoked(java.lang
     * .String)
     */
    public void commandInvoked(String command) {
        console.commandInvoked(command);

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.vectrace.MercurialEclipse.commands.IConsole#printError(java.lang.
     * String, java.lang.Throwable)
     */
    public void printError(String message, Throwable root) {
        console.errorLineReceived(root.getMessage(), new Status(IStatus.ERROR,
                MercurialEclipsePlugin.ID, message, root));
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.vectrace.MercurialEclipse.commands.IConsole#printMessage(java.lang
     * .String, java.lang.Throwable)
     */
    public void printMessage(String message, Throwable root) {
        console.messageLineReceived(message, new Status(IStatus.INFO,
                MercurialEclipsePlugin.ID, message, root));
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.vectrace.MercurialEclipse.commands.IConfiguration#getHgRoot(java.
     * io.File)
     */
    public File getHgRoot(File file) throws CoreException {
        return MercurialTeamProvider.getHgRoot(file);
    }

}
