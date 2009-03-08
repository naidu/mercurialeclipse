package com.vectrace.MercurialEclipse.commands;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

public class HgCatClient {

    public static String getContent(IFile file, String revision)
            throws HgException {
        AbstractShellCommand command = new HgCommand("cat", file.getProject() //$NON-NLS-1$
                .getLocation().toFile(), true);
        if (revision != null && revision.length() != 0) {
            command.addOptions("--rev", revision); //$NON-NLS-1$

        }
        command.addOptions("--decode"); //$NON-NLS-1$
        command.addOptions(file.getProjectRelativePath().toOSString());
        return command.executeToString();
    }

    public static String getContentFromBundle(IFile file, String revision,
            String overlayBundle) throws HgException {
        List<String> command = new ArrayList<String>();
        command.add(MercurialUtilities.getHGExecutable());
        command.add("-R"); //$NON-NLS-1$
        command.add(overlayBundle);
        command.add("cat"); //$NON-NLS-1$
        if (revision != null && revision.length() != 0) {
            command.add("-r"); //$NON-NLS-1$
            command.add("tip"); //$NON-NLS-1$
        }
        command.add("--decode"); //$NON-NLS-1$
        command.add(file.getProjectRelativePath().toOSString());
        AbstractShellCommand hgCommand = new HgCommand(command, file.getProject()
                .getLocation().toFile(), true);

        return hgCommand.executeToString();
    }
}
