package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;

public class HgIdentClient extends AbstractClient {

    public static String getCurrentRevision(IContainer root) throws HgException {
        HgCommand command = new HgCommand("identify", root, true); //$NON-NLS-1$
        command.addOptions("-n", "-i"); //$NON-NLS-1$ //$NON-NLS-2$
        command
                .setUsePreferenceTimeout(MercurialPreferenceConstants.STATUS_TIMEOUT);
        return command.executeToString().trim();
    }

    public static String getCurrentRevision(IResource resource)
            throws HgException {
        HgCommand command = new HgCommand("identify", //$NON-NLS-1$
                getWorkingDirectory(resource), true);
        command
                .setUsePreferenceTimeout(MercurialPreferenceConstants.STATUS_TIMEOUT);
        command.addOptions("-nibt"); //$NON-NLS-1$
        return command.executeToString().trim();
    }

    /**
     * Returns the current node-id as a String
     * 
     * @param repository
     *            the root of the repository to identify
     * @return Returns the node-id for the current changeset
     * @throws HgException
     * @throws IOException
     */
    public static String getCurrentChangesetId(File repository)
            throws HgException, IOException {
        String dirstate = repository.getCanonicalPath() + File.separator
                + ".hg" + File.separator + "dirstate"; //$NON-NLS-1$ //$NON-NLS-2$
        FileInputStream reader;
        try {
            reader = new FileInputStream(dirstate);
        } catch (FileNotFoundException e) {
            return null;
        }
        try {
            byte[] nodid = new byte[20];
            reader.read(nodid);
            StringBuilder id = new StringBuilder();
            for (byte b : nodid) {
                int x = b;
                x = x & 0xFF;
                String s = Integer.toHexString(x);
                if (s.length() == 1) {
                    s = "0" + s; //$NON-NLS-1$
                }
                id.append(s);
            }
            return id.toString();
        } finally {
            reader.close();
        }
    }
}
