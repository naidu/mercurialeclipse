package com.vectrace.MercurialEclipse.menu;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgCommitClient;
import com.vectrace.MercurialEclipse.team.ResourceProperties;

public class CommitMergeHandler extends SingleResourceHandler {

    @Override
    protected void run(IResource resource) throws Exception {
        IProject project = resource.getProject();
        HgCommitClient.commitProject(project, null, "merge");
        project.setPersistentProperty(ResourceProperties.MERGING, null);
        // project.refreshLocal(IResource.DEPTH_INFINITE, null);
        // TODO refresh FlagModel
        MercurialEclipsePlugin.refreshProjectFlags(project);
    }

}
