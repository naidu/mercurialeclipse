package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.Branch;

public class HgBranchClient extends AbstractClient {

    private static final Pattern GET_BRANCHES_PATTERN = Pattern
            .compile("^(.+[^ ]) +([0-9]+):([a-f0-9]+)( +(.+))?$"); //$NON-NLS-1$

    public static Branch[] getBranches(IProject project) throws HgException {
        AbstractShellCommand command = new HgCommand("branches", project, false); //$NON-NLS-1$
        command.addOptions("-v"); //$NON-NLS-1$
        String[] lines = command.executeToString().split("\n"); //$NON-NLS-1$
        int length = lines.length;
        Branch[] branches = new Branch[length];
        for (int i = 0; i < length; i++) {
            Matcher m = GET_BRANCHES_PATTERN.matcher(lines[i]);
            if (m.matches()) {
                Branch branch = new Branch(m.group(1), Integer.parseInt(m.group(2)), m
                        .group(3), (m.group(5) == null || !m.group(5).equals("(inactive)"))); //$NON-NLS-1$
                branches[i] = branch;
            } else {
                throw new HgException("Parse exception: '" + lines[i] + "'"); //$NON-NLS-1$ //$NON-NLS-2$
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
    public static String addBranch(IResource resource, String name,
            String user, boolean force) throws HgException {
        AbstractShellCommand command = new HgCommand("branch", getWorkingDirectory(resource), false); //$NON-NLS-1$
        if (force) {
            command.addOptions("-f"); //$NON-NLS-1$
        }
        command.addOptions(name);
        return command.executeToString();
    }

    /**
     * Get active branch of working directory
     * 
     * @param workingDir
     *            a file or a directory within the local repository
     * @return the branch name
     * @throws HgException
     *             if a hg error occurred
     */
    public static String getActiveBranch(File workingDir) throws HgException {
        AbstractShellCommand command = new HgCommand("branch", getWorkingDirectory(workingDir), false); //$NON-NLS-1$
        return command.executeToString().replaceAll("\n", "");
    }

}
