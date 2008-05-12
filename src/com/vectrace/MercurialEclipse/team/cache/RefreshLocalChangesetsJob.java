package com.vectrace.MercurialEclipse.team.cache;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeWorkspaceJob;
import com.vectrace.MercurialEclipse.exception.HgException;

/**
 * @author bastian
 *
 */
public class RefreshLocalChangesetsJob extends SafeWorkspaceJob {
    /**
     * 
     */
    private final IProject project;

    /**
     * @param name
     * @param project
     */
    public RefreshLocalChangesetsJob(String name, IProject project) {
        super(name);
        this.project = project;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vectrace.MercurialEclipse.SafeWorkspaceJob#runSafe(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    protected IStatus runSafe(IProgressMonitor monitor) {
        try {
            LocalChangesetCache.getInstance()
                    .refreshAllLocalRevisions(project, true);
        } catch (HgException e) {      
            MercurialEclipsePlugin.logError(e);
            MercurialEclipsePlugin.showError(e);
        }
        return super.runSafe(monitor);
    }
}