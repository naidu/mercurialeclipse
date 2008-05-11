/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import org.eclipse.core.resources.IWorkspace;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

public class HgCloneClient {

    public static void clone(IWorkspace workspace, HgRepositoryLocation repo,
            String cloneParameters, String projectName) throws HgException {        
        HgCommand command = new HgCommand("clone", workspace.getRoot(), false);
        command.setUsePreferenceTimeout(MercurialPreferenceConstants.CLONE_TIMEOUT);
        if (cloneParameters != null) {
            command.addOptions(cloneParameters);
        }
        command.addOptions(repo.getUrl(), projectName);
        command.executeToBytes();
    }

}
