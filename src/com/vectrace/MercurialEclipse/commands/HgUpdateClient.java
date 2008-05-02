package com.vectrace.MercurialEclipse.commands;

import org.eclipse.core.resources.IProject;

import com.vectrace.MercurialEclipse.exception.HgException;

public class HgUpdateClient {

    public static void update(IProject project, String revision, boolean clean)
            throws HgException {
        HgCommand command = new HgCommand("update", project, false);
        if (revision != null) {
            command.addOptions("-r", revision);
        }
        if (clean) {
            command.addOptions("-C");
        }
        command.executeToBytes();
    }

}
