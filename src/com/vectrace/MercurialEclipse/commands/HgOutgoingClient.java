/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch           - implementation
 *     Philip Graf               - proxy support
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

import org.eclipse.compare.patch.IFilePatch;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.utils.PatchUtils;

public class HgOutgoingClient extends AbstractParseChangesetClient {

    /**
     * @return never return null
     */
    public static Map<IPath, SortedSet<ChangeSet>> getOutgoing(IResource res,
            HgRepositoryLocation repository) throws HgException {
        AbstractShellCommand command = getCommand(res);
        try {
            command.addOptions("--style", AbstractParseChangesetClient //$NON-NLS-1$
                    .getStyleFile(true).getCanonicalPath());
        } catch (IOException e) {
            throw new HgException(e.getLocalizedMessage(), e);
        }

        addRepoToHgCommand(repository, command);

        String result = getResult(command);
        if (result == null) {
            return new HashMap<IPath, SortedSet<ChangeSet>>();
        }

        Map<IPath, SortedSet<ChangeSet>> revisions = createMercurialRevisions(
                res, result, true, Direction.OUTGOING, repository, null,
                getOutgoingPatches(res, repository));
        return revisions;
    }

    private static IFilePatch[] getOutgoingPatches(IResource res,
            HgRepositoryLocation repository) throws HgException {
        String outgoingPatch = getOutgoingPatch(res, repository);
        return PatchUtils.getFilePatches(outgoingPatch);
    }

    private static String getResult(AbstractShellCommand command) throws HgException {
        try {
            String result = command.executeToString();
            if (result.endsWith("no changes found")) { //$NON-NLS-1$
                return null;
            }
            return result;
        } catch (HgException hg) {
            if (hg.getStatus().getCode() == 1) {
                return null;
            }
            throw hg;
        }
    }

    private static AbstractShellCommand getCommand(IResource res) {
        AbstractShellCommand command = new HgCommand("outgoing", res.getProject(), //$NON-NLS-1$
                false);
        command.setUsePreferenceTimeout(MercurialPreferenceConstants.PULL_TIMEOUT);
        return command;
    }

    private static String getOutgoingPatch(IResource res, HgRepositoryLocation repository) throws HgException {
        AbstractShellCommand command = getCommand(res);
        command.addOptions("-p");
        addRepoToHgCommand(repository, command);
        return getResult(command);
    }

}
