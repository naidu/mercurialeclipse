package com.vectrace.MercurialEclipse.menu;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import com.vectrace.MercurialEclipse.commands.HgCommitClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.team.ResourceProperties;
import com.vectrace.MercurialEclipse.team.cache.RefreshJob;

public class CommitMergeHandler extends SingleResourceHandler {

    @Override
    protected void run(IResource resource) throws Exception {
        commitMerge(resource);
    }

    /**
     * @param resource
     * @throws HgException
     * @throws CoreException
     */
    public static String commitMerge(IResource resource) throws HgException,
            CoreException {
        IProject project = resource.getProject();
        String result = HgCommitClient.commitProject(project, null, "Merge with "
                + project.getPersistentProperty(ResourceProperties.MERGING));
        project.setPersistentProperty(ResourceProperties.MERGING, null);
        project.refreshLocal(IResource.DEPTH_INFINITE, null);
        new RefreshJob("Refresh status and changesets after merge", null,
                project).schedule();
        return result;
    }

}
