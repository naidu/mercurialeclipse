package com.vectrace.MercurialEclipse.commands;

import org.eclipse.core.resources.ResourcesPlugin;

import com.vectrace.MercurialEclipse.exception.HgException;

public class HgDebugInstallClient {

    public static String debugInstall() throws HgException {
        HgCommand command = new HgCommand("debuginstall", ResourcesPlugin
                .getWorkspace().getRoot(), true);
        return command.executeToString().trim();
    }
}
