package com.vectrace.MercurialEclipse.team;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgIMergeClient;
import com.vectrace.MercurialEclipse.views.MergeView;

public class MergeAction extends SingleResourceAction {

    //TODO belongs to ResourceProperties
    public final static String MERGING_PROPERTY = "merging";
    
    @Override
    protected void run(IResource resource) throws Exception {
        IProject project = resource.getProject();
        if(project.getPersistentProperty(new QualifiedName(MercurialEclipsePlugin.ID, MERGING_PROPERTY)) == null) {
            HgIMergeClient.merge(project, null);//FIXME handle other revisions
            project.setPersistentProperty(new QualifiedName(MercurialEclipsePlugin.ID, MERGING_PROPERTY), "true");
            project.refreshLocal(IResource.DEPTH_INFINITE, null);
            //TODO update Merge view
            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(
                    MergeView.ID);
        }
    }

}
