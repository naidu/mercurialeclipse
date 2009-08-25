package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
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

        HgRoot hgRoot = MercurialTeamProvider.getHgRoot(file);
        command.add(pathRelativeTo(file, hgRoot));

        AbstractShellCommand hgCommand = new HgCommand(command, hgRoot, true);

        return hgCommand.executeToString();
    }

    /**
     * Construct a path relative to the mercurial working copy root.
     *
     * @param file create relative path for this file.
     * @param hgRoot the working copy root.
     * @return a path to <code>file<code> relative to <code>hgRoot</code>
     *
     * @throws HgException
     */
    private static String pathRelativeTo(IFile file, HgRoot hgRoot) throws HgException {
        String rootPath = hgRoot.getAbsolutePath();
        String filePath;
        try {
            filePath = file.getLocation().toFile().getCanonicalPath();
        } catch (IOException e) {
            throw new HgException("Can't resolve path of " + file + ": " + e, e);
        }
        if (filePath.startsWith(rootPath)) {
            filePath = filePath.substring(rootPath.length());
        }
        if (filePath.startsWith(File.separator)) {
            filePath = filePath.substring(File.separator.length());
        }
        return filePath;
    }
}
