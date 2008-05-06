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

import java.util.Map;
import java.util.SortedSet;

import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

public class HgOutgoingClient extends AbstractParseChangesetClient {

    public static Map<IResource, SortedSet<ChangeSet>> getOutgoing(
            IResource res, HgRepositoryLocation loc) throws HgException {
        try {
            HgCommand command = new HgCommand("outgoing", res.getProject(),
                    false);

            command.addOptions("--template",
                    AbstractParseChangesetClient.TEMPLATE);

            command.addOptions(loc.toString());
            String result = command.executeToString();
            if (result.contains("no changes found")) {
                return null;
            }
            Map<IResource, SortedSet<ChangeSet>> revisions = createMercurialRevisions(
                    result, res.getProject(), null, loc, Direction.OUTGOING);
            return revisions;
        } catch (HgException hg) {
            if (hg.getMessage().contains("return code: 1")) {
                return null;
            }
            throw new HgException(hg.getMessage(), hg);
        }
    }

}
