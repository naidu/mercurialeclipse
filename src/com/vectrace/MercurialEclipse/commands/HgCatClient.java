package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class HgCatClient extends AbstractClient {

    public static String getContent(IFile resource, String revision)
            throws HgException {
        HgRoot hgRoot = getHgRoot(resource);
        File file = ResourceUtils.getFileHandle(resource);
        AbstractShellCommand command = new HgCommand("cat", hgRoot, true);
        if (revision != null && revision.length() != 0) {
            command.addOptions("-r", revision); //$NON-NLS-1$

        }
        command.addOptions("--decode"); //$NON-NLS-1$
        command.addOptions(hgRoot.toRelative(file));
        return command.executeToString();
    }

    public static String getContentFromBundle(IFile resource, String revision,
            String overlayBundle) throws HgException {
        HgRoot hgRoot = getHgRoot(resource);
        File file = ResourceUtils.getFileHandle(resource);
        List<String> command = new ArrayList<String>();
        command.add(MercurialUtilities.getHGExecutable());
        command.add("-R"); //$NON-NLS-1$
        command.add(overlayBundle);
        command.add("cat"); //$NON-NLS-1$
        if (revision != null && revision.length() != 0) {
            command.add("-r"); //$NON-NLS-1$
            command.add(revision);
        }
        command.add("--decode"); //$NON-NLS-1$

        command.add(hgRoot.toRelative(file));

        AbstractShellCommand hgCommand = new HgCommand(command, hgRoot, true);

        return hgCommand.executeToString();
    }
}
