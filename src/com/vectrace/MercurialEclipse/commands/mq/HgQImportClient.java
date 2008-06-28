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
package com.vectrace.MercurialEclipse.commands.mq;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;

import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.commands.HgCommand;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;

/**
 * @author bastian
 * 
 */
public class HgQImportClient extends AbstractClient {
    public static String qimport(IResource resource, boolean force, boolean git, boolean existing,
            ChangeSet[] changesets, IPath patchFile) throws HgException {
        Assert.isNotNull(resource);
        HgCommand command = new HgCommand("qimport",
                getWorkingDirectory(resource), true);
        command.setUsePreferenceTimeout(MercurialPreferenceConstants.CLONE_TIMEOUT);

        if (force) {
            command.addOptions("--force");
        }
        
        if (git) {
            command.addOptions("--git");
        }
        
        if (changesets != null && changesets.length>0) {
            command.addOptions("--rev", changesets[changesets.length-1].getChangeset()+ ":" +changesets[0].getChangeset() );                       
        } else {
            Assert.isNotNull(patchFile);
            if (existing) {
                command.addOptions("--existing");
            } else {
                command.addOptions("--name");
            }
            patchFile.toOSString();
        }
        
        return command.executeToString();
    }
}
