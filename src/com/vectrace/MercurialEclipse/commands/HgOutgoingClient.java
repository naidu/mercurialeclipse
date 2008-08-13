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

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.SortedSet;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

public class HgOutgoingClient extends AbstractParseChangesetClient {

    public static Map<IPath, SortedSet<ChangeSet>> getOutgoing(IResource res,
            HgRepositoryLocation repository) throws HgException {
        try {
            HgCommand command = new HgCommand("outgoing", res.getProject(),
                    false);
            command
                    .setUsePreferenceTimeout(MercurialPreferenceConstants.PULL_TIMEOUT);
            command.addOptions("--style", AbstractParseChangesetClient
                    .getStyleFile(true).getCanonicalPath());

            URI uri = repository.getUri();
            if (uri != null) {
                command.addOptions(uri.toASCIIString());
            } else {
                command.addOptions(repository.getLocation());
            }
            
            String result = command.executeToString();
            if (result.contains("no changes found")) {
                return null;
            }
            Map<IPath, SortedSet<ChangeSet>> revisions = createMercurialRevisions(
                    res, result, true,
                    Direction.OUTGOING, repository, null);
            return revisions;
        } catch (HgException hg) {
            if (hg.getMessage().contains("return code: 1")) {
                return null;
            }
            throw new HgException(hg.getMessage(), hg);
        } catch (IOException e) {
            throw new HgException(e.getLocalizedMessage(), e);
        }
    }

}
