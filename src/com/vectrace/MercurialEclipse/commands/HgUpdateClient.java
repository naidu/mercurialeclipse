package com.vectrace.MercurialEclipse.commands;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.ResourceProperties;

public class HgUpdateClient {

    public static void update(IProject project, String revision, boolean clean)
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
        String branch = HgBranchClient.getActiveBranch(project.getLocation().toFile());
        try {
            project.setSessionProperty(ResourceProperties.HG_BRANCH, branch);
        } catch (CoreException e) {
            MercurialEclipsePlugin.logError(e);
        }
    }

}
