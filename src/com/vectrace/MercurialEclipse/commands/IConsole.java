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


/**
 * @author Stefan
 *
 */
public interface IConsole {

    /**
     * @param command
     */
    void commandInvoked(String command);

    /**
     * @param status
     * @param error
     */
    void commandCompleted(int exitCode, String message, Throwable error);

    /**
     * @param string
     * @param hgEx
     */
    void printError(String message, Throwable root);

}
