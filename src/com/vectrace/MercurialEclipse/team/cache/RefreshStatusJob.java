package com.vectrace.MercurialEclipse.team.cache;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.core.TeamException;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeWorkspaceJob;

/**
 * @author bastian
 * 
 */
final class RefreshStatusJob extends SafeWorkspaceJob {
    /**
     * 
     */
    private static final MercurialStatusCache mercurialStatusCache = MercurialStatusCache
            .getInstance();

    /**
     * @param name
     * @param mercurialStatusCache
     *            TODO
     */
    RefreshStatusJob(String name) {
        super(name);
    }

    @Override
    protected IStatus runSafe(IProgressMonitor monitor) {
        try {
            monitor.beginTask("Obtaining Mercurial Status information.", 5);
            mercurialStatusCache.refreshStatus(monitor);
        } catch (TeamException e) {
            MercurialEclipsePlugin.logError(e);
        }
        return super.runSafe(monitor);
    }
}