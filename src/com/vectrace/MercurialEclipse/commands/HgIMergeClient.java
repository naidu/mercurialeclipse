package com.vectrace.MercurialEclipse.commands;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.FlaggedAdaptable;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;

public class HgIMergeClient {

    public static String merge(IProject project, String revision)
            throws HgException {
        HgCommand command = new HgCommand("imerge", project, false);
        command.setUsePreferenceTimeout(MercurialPreferenceConstants.IMERGE_TIMEOUT);
        command.addOptions("--config", "extensions.imerge=");
        if (revision != null) {
            command.addOptions("-r", revision);
        }
        return new String(command.executeToBytes());
    }

    public static List<FlaggedAdaptable> getMergeStatus(IProject project)
            throws HgException {
        HgCommand command = new HgCommand("imerge", project, false);
        command.addOptions("--config", "extensions.imerge=");
        command.addOptions("status");
        command.setUsePreferenceTimeout(MercurialPreferenceConstants.IMERGE_TIMEOUT);
        String[] lines = command.executeToString().split("\n");
        ArrayList<FlaggedAdaptable> result = new ArrayList<FlaggedAdaptable>();
        if (lines.length != 1 || !"all conflicts resolved".equals(lines[0])) {
            for (String line : lines) {
                FlaggedAdaptable flagged = new FlaggedAdaptable(project
                        .getFile(line.substring(2)), line.charAt(0));
                result.add(flagged);
            }
        }
        return result;
    }

}
