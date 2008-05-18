package com.vectrace.MercurialEclipse.commands;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.Branch;

public class HgBranchClient {

    private static final Pattern GET_BRANCHES_PATTERN = Pattern
            .compile("^(.+[^ ]) +([0-9]+):([a-f0-9]+)( +(.+))?$");

    public static Branch[] getBranches(IProject project) throws HgException {
        HgCommand command = new HgCommand("branches", project, false);
        command.addOptions("-v");
        String[] lines = command.executeToString().split("\n");
        int length = lines.length;
        Branch[] branches = new Branch[length];
        for (int i = 0; i < length; i++) {
            Matcher m = GET_BRANCHES_PATTERN.matcher(lines[i]);
            if (m.matches()) {
                Branch branch = new Branch(m.group(1), Integer.parseInt(m.group(2)), m
                        .group(3), (m.group(5) == null || !m.group(5).equals("(inactive)")));
                branches[i] = branch;
            } else {
                throw new HgException("Parse exception: '" + lines[i] + "'");
            }
        }
        return branches;
    }

    /**
     * 
     * @param resource
     * @param name
     * @param user
     *            if null, uses the default user
     * @param local
     * @throws HgException
     */
    public static void addBranch(IResource resource, String name,
            String user, boolean force) throws HgException {
        HgCommand command = new HgCommand("branch", resource.getProject(), false);
        if (force) {
            command.addOptions("-f");
        }
        command.addUserName(user);
        command.addOptions(name);
        command.executeToBytes();
    }

}
