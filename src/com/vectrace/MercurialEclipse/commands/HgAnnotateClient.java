package com.vectrace.MercurialEclipse.commands;

import org.eclipse.core.resources.IFile;

import com.vectrace.MercurialEclipse.exception.HgException;

public class HgAnnotateClient {

    public static String getAnnotation(IFile file) throws HgException {
        HgCommand command = new HgCommand("annotate", file.getProject(), true);
        command.addOptions("--user", "--number", "--changeset", "--date");
        command.addFiles(file);
        return command.executeToString();
    }
}
