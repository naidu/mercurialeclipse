package com.vectrace.MercurialEclipse.commands;

import java.io.File;

import org.eclipse.core.resources.IContainer;

import com.vectrace.MercurialEclipse.exception.HgException;

public class HgIdentClient {

    public static String getCurrentRevision(IContainer root) throws HgException {
        HgCommand command = new HgCommand("ident", root, true);
        command.addOptions("-n", "-i");
        return command.executeToString().trim();
    }

    /**
     * Returns the current node-id as a String
     * @param repository the root of the repository to identify
     * @return Returns the node-id for the current changeset
     * @throws HgException
     */
    public static String getCurrentChangesetId(File repository) throws HgException {
        HgCommand command = new HgCommand("ident", repository, true);
        command.addOptions("-i", "--debug");
        String nodeid = command.executeToString().trim();
        return nodeid.replace("+", "");
    }
}
