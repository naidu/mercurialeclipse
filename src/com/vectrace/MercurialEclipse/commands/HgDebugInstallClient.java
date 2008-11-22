/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian Doetsch  -  implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.File;

import com.vectrace.MercurialEclipse.exception.HgException;

public class HgDebugInstallClient {

    public static String debugInstall() throws HgException {
        // we don't really need a working dir...
        HgCommand command = new HgCommand("debuginstall", (File)null, true); //$NON-NLS-1$
        return new String(command.executeToBytes(Integer.MAX_VALUE)).trim();
    }
}
