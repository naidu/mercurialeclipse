package com.vectrace.MercurialEclipse.team;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.commands.HgUpdateClient;

public class UpdateAction extends SingleResourceAction {

    @Override
    protected void run(IResource resource) throws Exception {
        IProject project = resource.getProject();
        HgUpdateClient.update(project, null, false);
        project.refreshLocal(IResource.DEPTH_INFINITE, null);
    }

}
