package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

/**
 * 
 * @author Jerome Negre <jerome+hg@jnegre.org>
 */
public class HgCommand extends AbstractShellCommand {

    public HgCommand(List<String> commands, File workingDir, boolean escapeFiles) {
        super(commands, workingDir, escapeFiles);
    }

    public HgCommand(String command, File workingDir, boolean escapeFiles) {
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
        return HgClients.getExecutable();
        
    }

    protected String getDefaultUserName() {
        return HgClients.getDefaultUserName();
    }

    protected void addUserName(String user) {
        this.options.add("-u");
        this.options.add(user != null ? user : getDefaultUserName());
    }

    protected static Map<HgRoot, List<IResource>> groupByRoot(
            List<IResource> resources) throws HgException, IOException {
        Map<HgRoot, List<IResource>> result = new HashMap<HgRoot, List<IResource>>();
        for (IResource resource : resources) {
            HgRoot root = new HgRoot(MercurialTeamProvider.getHgRoot(resource)
                    .getCanonicalPath());
            List<IResource> list = result.get(root);
            if (list == null) {
                list = new ArrayList<IResource>();                
                result.put(root, list);
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
