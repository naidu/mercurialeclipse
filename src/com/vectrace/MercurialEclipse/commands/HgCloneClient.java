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

import java.io.File;
import java.net.URI;

import org.eclipse.core.resources.IWorkspace;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

public class HgCloneClient extends AbstractRepositoryClient {

    public static void clone(IWorkspace workspace, HgRepositoryLocation repo,
            String cloneParameters, String projectName) throws HgException {
        HgCommand command = new HgCommand("clone", workspace.getRoot(), false);
        command
                .setUsePreferenceTimeout(MercurialPreferenceConstants.CLONE_TIMEOUT);
        if (cloneParameters != null) {
            command.addOptions(cloneParameters);
        }
        command.addOptions(repo.getUrl(), projectName);
        command.executeToBytes();
    }

    public static void clone(String parentDirectory, HgRepositoryLocation repo,
            boolean noUpdate, boolean pull, boolean uncompressed,
            boolean timeout, String rev, String cloneName) throws HgException {
        HgCommand command = new HgCommand("clone", new File(parentDirectory),
                false);

        if (noUpdate) {
            command.addOptions("--noupdate");
        }
        if (pull) {
            command.addOptions("--pull");
        }
        if (uncompressed) {
            command.addOptions("--uncompressed");
        }
        if (rev != null && rev.length() > 0) {
            command.addOptions("--rev", rev);
        }

        URI uri = repo.getUri();
        command.addOptions(uri.toASCIIString(), cloneName);
        if (timeout) {
            command
                    .setUsePreferenceTimeout(MercurialPreferenceConstants.CLONE_TIMEOUT);
            command.executeToBytes();
        } else {
            command.executeToBytes(Integer.MAX_VALUE);
        }
    }
}
