package com.vectrace.MercurialEclipse.commands;

import org.eclipse.core.resources.IProject;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;

public class HgUpdateClient {

    public static void update(final IProject project, String revision, boolean clean)
            throws HgException {
        AbstractShellCommand command = new HgCommand("update", project, false); //$NON-NLS-1$
        command
                .setUsePreferenceTimeout(MercurialPreferenceConstants.UPDATE_TIMEOUT);
        if (revision != null) {
            command.addOptions("-r", revision); //$NON-NLS-1$
        }
        if (clean) {
            command.addOptions("-C"); //$NON-NLS-1$
        }
        command.executeToBytes();

        new RefreshWorkspaceStatusJob(project).schedule();
    }
}
