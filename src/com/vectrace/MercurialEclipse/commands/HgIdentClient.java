package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
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

    static String getCurrentChangesetId(InputStream inputStream) throws HgException {
        StringBuilder id = new StringBuilder(20);
        byte[] first20bytes = new byte[20];
        int read;
        try {
            read = inputStream.read(first20bytes);
        } catch (IOException e) {
            throw new HgException(e.getMessage(), e);
        }
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
     */
    public static String getCurrentChangesetId(HgRoot repository) throws HgException {
        StringBuilder pathStr = new StringBuilder(repository.getAbsolutePath());
        pathStr.append(File.separator).append(".hg");
        pathStr.append(File.separator).append("dirstate");
        FileInputStream reader = null;
        try {
            reader = new FileInputStream(pathStr.toString());
        } catch (FileNotFoundException e) {
            // sometimes hg is writing the file at same time we trying to read from it
            // this happens especially if we run many add/remove operations
            synchronized (HgIdentClient.class) {
                try {
                    HgIdentClient.class.wait(300);
                } catch (InterruptedException e1) {
                    MercurialEclipsePlugin.logError(e1);
                }
            }
            try {
                reader = new FileInputStream(pathStr.toString());
            } catch (FileNotFoundException e1) {
                MercurialEclipsePlugin.logError(e1);
            }
            if(reader == null) {
                throw new HgException("Dirstate failed for the path: " + pathStr, e);
            }
        }
        try {
            return getCurrentChangesetId(reader);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                MercurialEclipsePlugin.logError(e);
            }
        }
    }
}
