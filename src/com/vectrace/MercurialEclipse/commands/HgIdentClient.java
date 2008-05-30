package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

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
     * @throws IOException 
     */
    public static String getCurrentChangesetId(File repository)
            throws HgException, IOException {
        String dirstate = repository.getCanonicalPath() + File.separator
                + ".hg" + File.separator + "dirstate";
        FileInputStream reader = new FileInputStream(dirstate);
        try {
            byte[] nodid = new byte[20];
            reader.read(nodid);
            StringBuilder id = new StringBuilder();
            for (byte b : nodid) {
                int x = b;
                x = x & 0xFF;
                id.append(Integer.toHexString(x));
            }
            return id.toString();
        } finally {
            reader.close();
        }
    }
}
