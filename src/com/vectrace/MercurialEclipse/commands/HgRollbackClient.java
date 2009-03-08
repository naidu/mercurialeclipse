/*******************************************************************************
 * Copyright (c) 2007-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch	- implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import org.eclipse.core.resources.IProject;

import com.vectrace.MercurialEclipse.exception.HgException;

public class HgRollbackClient {

    public static String rollback(IProject project) throws HgException {
        AbstractShellCommand command = new HgCommand("rollback", project.getLocation() //$NON-NLS-1$
                .toFile(), true);
        String result = command.executeToString();
        return result;
    }

}
