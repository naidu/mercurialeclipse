/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian Doetsch	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import org.eclipse.core.resources.IProject;

import com.vectrace.MercurialEclipse.exception.HgException;

/**
 * Calls hg root
 * @author bastian
 *
 */
public class HgRootClient {
    public static String getHgRoot(IProject proj) throws HgException {
        HgCommand command = new HgCommand("root", proj, true);
        return command.executeToString().replaceAll("\n", "");
    }
}
