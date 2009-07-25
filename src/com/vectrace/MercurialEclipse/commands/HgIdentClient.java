package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;

public class HgIdentClient extends AbstractClient {

    public static String getCurrentRevision(IContainer root) throws HgException {
        AbstractShellCommand command = new HgCommand("identify", root, true); //$NON-NLS-1$
        command.addOptions("-n", "-i"); //$NON-NLS-1$ //$NON-NLS-2$
        command
        .setUsePreferenceTimeout(MercurialPreferenceConstants.STATUS_TIMEOUT);
        return command.executeToString().trim();
    }

    public static String getCurrentRevision(IResource resource)
    throws HgException {
        AbstractShellCommand command = new HgCommand("identify", //$NON-NLS-1$
                getWorkingDirectory(resource), true);
        command
        .setUsePreferenceTimeout(MercurialPreferenceConstants.STATUS_TIMEOUT);
        command.addOptions("-nibt"); //$NON-NLS-1$
        return command.executeToString().trim();
    }

    /**
     * @param b
     *            byte array containing integer data
     * @param idx
     *            start index to read integer from
     * @return integer value, corresponding to the 4 bytes from the given array at given position
     */
    private static int getNextInt(byte[] b, int idx) {
        int result = 0;
        for(int i = 0; i < 4 && i + idx < b.length; i++) {
            result = ((result << 8) + (b[i + idx] & 0xff));
        }
        return result;
    }

    static String getCurrentChangesetId(InputStream inputStream) throws IOException {
        StringBuilder id = new StringBuilder(20);
        byte[] first20bytes = new byte[20];
        int read = inputStream.read(first20bytes);
        for (int i = 0; i < read; i += 4) {
            int next = getNextInt(first20bytes, i);
            String s = Integer.toHexString(next);
            int size = s.length();
            while(size < 8) {
                id.append('0');
                size ++;
            }
            id.append(s);
        }
        return id.toString();
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
        StringBuilder pathStr = new StringBuilder(repository.getCanonicalPath());
        pathStr.append(File.separator).append(".hg");
        pathStr.append(File.separator).append("dirstate");
        FileInputStream reader;
        try {
            reader = new FileInputStream(pathStr.toString());
        } catch (FileNotFoundException e) {
            return null;
        }
        try {
            return getCurrentChangesetId(reader);
        } finally {
            reader.close();
        }
    }
}
