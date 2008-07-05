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

import java.io.PrintStream;

import com.vectrace.MercurialEclipse.commands.IConfiguration;
import com.vectrace.MercurialEclipse.commands.IConsole;
import com.vectrace.MercurialEclipse.commands.IErrorHandler;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

/**
 * @author Stefan
 *
 */
public class DefaultConfiguration implements IConsole, IErrorHandler, IConfiguration {

    private static PrintStream console = new PrintStream(MercurialUtilities
            .getMercurialConsole().newOutputStream());
    
    
    /* (non-Javadoc)
     * @see com.vectrace.MercurialEclipse.commands.IConsole#getOutputStream()
     */
    public PrintStream getOutputStream() {
        return console;
        
    }


    /* (non-Javadoc)
     * @see com.vectrace.MercurialEclipse.commands.IConfiguration#getDefaultUserName()
     */
    public String getDefaultUserName() {
        return MercurialEclipsePlugin.getDefault().getPreferenceStore()
        .getString(MercurialPreferenceConstants.MERCURIAL_USERNAME);
    }


    /* (non-Javadoc)
     * @see com.vectrace.MercurialEclipse.commands.IConfiguration#getExecutable()
     */
    public String getExecutable() {
        if (!MercurialEclipsePlugin.getDefault().isHgUsable()) {
            MercurialUtilities.configureHgExecutable();
            MercurialEclipsePlugin.getDefault().checkHgInstallation();
        }

        return MercurialEclipsePlugin.getDefault().getPreferenceStore()
                .getString(MercurialPreferenceConstants.MERCURIAL_EXECUTABLE);
    }


    /* (non-Javadoc)
     * @see com.vectrace.MercurialEclipse.commands.IErrorHandler#logError(java.lang.Throwable)
     */
    public void logError(Throwable e) {
        MercurialEclipsePlugin.logError(e);
    }


    /* (non-Javadoc)
     * @see com.vectrace.MercurialEclipse.commands.IErrorHandler#logWarning(java.lang.String, java.lang.Throwable)
     */
    public void logWarning(String message, Throwable e) {
        // TODO Auto-generated method stub
        
    }


    /* (non-Javadoc)
     * @see com.vectrace.MercurialEclipse.commands.IConfiguration#getTimeOut(java.lang.String)
     */
    public int getTimeOut(String commandId) {
        int timeout =  12000;
        String pref = MercurialUtilities.getPreference(commandId, String.valueOf(timeout));
        try {
            timeout = Integer.parseInt(pref);
            if (timeout < 0) {
                throw new NumberFormatException("Timeout < 0");
            }
        } catch (NumberFormatException e) {
            logWarning("Timeout for command " + commandId + " not correctly configured in preferences.",e);
        }
        return timeout;
    }
}
