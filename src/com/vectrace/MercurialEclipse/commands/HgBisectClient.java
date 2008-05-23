/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Stefan Chyssler	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.File;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;

/**
 * @author Stefan Chyssler
 * 
 */
public final class HgBisectClient {

    public static String markGood(File repository, ChangeSet good)
            throws HgException {
        HgCommand cmd = new HgCommand("bisect", repository, true);
        cmd.addOptions("-g", getRevision(good));
        return cmd.executeToString();
    }

    public static String markBad(File repository, ChangeSet bad)
            throws HgException {
        HgCommand cmd = new HgCommand("bisect", repository, true);
        cmd.addOptions("-b", getRevision(bad));
        return cmd.executeToString();
    }

    public static String reset(File repository) throws HgException {
        HgCommand cmd = new HgCommand("bisect", repository, true);
        cmd.addOptions("-r");
        return cmd.executeToString();
    }

    private static String getRevision(ChangeSet change) {
        return Integer.toString(change.getRevision().getRevision());
    }
}
