package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

/**
 * 
 * @author Jerome Negre <jerome+hg@jnegre.org>
 */
public class HgCommand extends AbstractShellCommand {

    public HgCommand(List<String> commands, File workingDir, boolean escapeFiles) {
        super(commands, workingDir, escapeFiles);
    }

    protected HgCommand(String command, File workingDir, boolean escapeFiles) {
        this.command = command;
        this.workingDir = workingDir;
        this.escapeFiles = escapeFiles;
    }

    protected HgCommand(String command, IContainer container,
            boolean escapeFiles) {
        this(command, container.getLocation().toFile(), escapeFiles);
    }

    protected HgCommand(String command, boolean escapeFiles) {
        this(command, (File) null, escapeFiles);
    }

    protected String getHgExecutable() {

        if (!MercurialEclipsePlugin.getDefault().isHgUsable()) {
            MercurialUtilities.configureHgExecutable();
            MercurialEclipsePlugin.getDefault().checkHgInstallation();
        }

        return MercurialEclipsePlugin.getDefault().getPreferenceStore()
                .getString(MercurialPreferenceConstants.MERCURIAL_EXECUTABLE);
    }

    protected String getDefaultUserName() {
        return MercurialEclipsePlugin.getDefault().getPreferenceStore()
                .getString(MercurialPreferenceConstants.MERCURIAL_USERNAME);
    }

    protected void addUserName(String user) {
        this.options.add("-u");
        this.options.add(user != null ? user : getDefaultUserName());
    }

    protected static Map<IProject, List<IResource>> groupByProject(
            List<IResource> resources) {
        Map<IProject, List<IResource>> result = new HashMap<IProject, List<IResource>>();
        for (IResource resource : resources) {
            List<IResource> list = result.get(resource.getProject());
            if (list == null) {
                list = new ArrayList<IResource>();
                result.put(resource.getProject(), list);
            }
            list.add(resource);
        }
        return result;
    }

    @Override
    protected String getExecutable() {
        return getHgExecutable();
    }
}
