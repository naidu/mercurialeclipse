package com.vectrace.MercurialEclipse.commands;

import java.util.List;

import org.eclipse.core.resources.IProject;

import com.vectrace.MercurialEclipse.exception.HgException;

public class HgTransplantClient {

    /**
     * Cherrypicks given ChangeSets from repository or branch.
     * @param project the project
     * @param nodeIds the changeset identifiers
     * @param source the branch or repository
     * @param branch flag, if we want to pick from a branch. true if branch, false if repo.
     * @throws HgException
     */
    public static void transplant(IProject project, List<String> nodeIds,
            String source, boolean branch) throws HgException {
        HgCommand command = new HgCommand("transplant", project, false);
        command.addOptions("--log");
        if (branch) {
            command.addOptions("-b");
        } else {
            command.addOptions("-s");
        }
        command.addOptions(source);
        if (nodeIds != null && nodeIds.size() > 0) {
            for (String node : nodeIds) {
                command.addOptions(node);
            }
        }
        command.executeToBytes();
    }
}
