package com.vectrace.MercurialEclipse.commands;

import org.eclipse.core.resources.IProject;

import com.vectrace.MercurialEclipse.exception.HgException;

public class HgUpdateClient {

    //FIXME find a better name
    public static void rollback(IProject project) throws HgException {
        HgCommand command = new HgCommand("update", project, false);
        command.addOptions("-C");
        command.executeToBytes();
    }
    
}
