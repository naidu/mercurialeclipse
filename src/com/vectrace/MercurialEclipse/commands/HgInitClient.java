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
package com.vectrace.MercurialEclipse.commands;

import org.eclipse.core.resources.IProject;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;

/**
 * @author bastian
 *
 */
public class HgInitClient extends AbstractClient {
    public static String init(IProject project, String path) throws HgException {
        HgCommand command = new HgCommand("init", getWorkingDirectory(project), //$NON-NLS-1$
                false);
        command.addOptions(path);
        command
                .setUsePreferenceTimeout(MercurialPreferenceConstants.DEFAULT_TIMEOUT);
        return command.executeToString();
    }
}
