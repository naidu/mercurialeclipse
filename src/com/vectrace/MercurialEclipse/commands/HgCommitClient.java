package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;

public class HgCommitClient {

    public static void commitResources(List<IResource> resources, String user,
            String message, IProgressMonitor monitor) throws HgException {

        Map<HgRoot, List<IResource>> resourcesByRoot;
        try {
            resourcesByRoot = HgCommand.groupByRoot(resources);
        } catch (IOException e) {
            throw new HgException(e.getLocalizedMessage(), e);
        }
        for (HgRoot root : resourcesByRoot.keySet()) {
            if (monitor != null) {
                monitor.subTask("Committing resources from " + root.getName());
            }
            List<IResource> files = resourcesByRoot.get(root);
            commitRepository(root, files, user, message);
        }
    }

    private static void commitRepository(HgRoot root, List<IResource> files,
            String user, String message) throws HgException {
        commit(root, AbstractClient.toFiles(files), user, message);

    }

    public static String commit(HgRoot root, List<File> files, String user,
            String message) throws HgException {

        HgCommand command = new HgCommand("commit", root, true);
        command
                .setUsePreferenceTimeout(MercurialPreferenceConstants.COMMIT_TIMEOUT);
        command.addUserName(quote(user));
        command.addOptions("-m", quote(message));
        command.addFiles(AbstractClient.toPaths(files));
        return command.executeToString();
    }

    static String quote(String str) {
        if (str == null || str.length() == 0) {
            return str;
        }
        return "\"" + str.replaceAll("\"", "\\\\\"") + "\"";
    }

    public static String commitProject(IProject project, String user,
            String message) throws HgException {
        HgRoot hgroot = new HgRoot(HgRootClient.getHgRoot(project));
        return commit(hgroot, new ArrayList<File>(), user, message);
        // HgCommand command = new HgCommand("commit", project, false);
        // command.setUsePreferenceTimeout(MercurialPreferenceConstants.
        // COMMIT_TIMEOUT);
        // command.addUserName(user);
        // command.addOptions("-m", message);
        // return new String(command.executeToBytes());
    }

}
