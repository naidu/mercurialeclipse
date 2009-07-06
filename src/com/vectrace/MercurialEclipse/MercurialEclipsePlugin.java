/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Stefan Groschupf          - logError
 *     Jérôme Nègre              - some fixes
 *     Stefan C                  - Code cleanup
 *******************************************************************************/

package com.vectrace.MercurialEclipse;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSetManager;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.vectrace.MercurialEclipse.commands.HgClients;
import com.vectrace.MercurialEclipse.commands.HgDebugInstallClient;
import com.vectrace.MercurialEclipse.mapping.HgActiveChangeSetCollector;
import com.vectrace.MercurialEclipse.storage.HgCommitMessageManager;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocationManager;
import com.vectrace.MercurialEclipse.synchronize.MercurialSynchronizeSubscriber;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.team.cache.AbstractCache;
import com.vectrace.MercurialEclipse.team.cache.IncomingChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.team.cache.OutgoingChangesetCache;

/**
 * The main plugin class to be used in the desktop.
 */
public class MercurialEclipsePlugin extends AbstractUIPlugin {

    public static final String ID = "com.vectrace.MercurialEclipse"; //$NON-NLS-1$

    public static final String ID_ChangeLogView = "com.vectrace.MercurialEclipse.views.ChangeLogView"; //$NON-NLS-1$

    public static final String BUNDLE_FILE_PREFIX = "bundlefile"; //$NON-NLS-1$

    // The shared instance.
    private static MercurialEclipsePlugin plugin;
    
    // the repository manager
    private static HgRepositoryLocationManager repoManager = new HgRepositoryLocationManager();

    // the commit message manager
    private static HgCommitMessageManager commitMessageManager = new HgCommitMessageManager();

    
    private boolean hgUsable = true;

    private ActiveChangeSetManager changeSetManager;

    /**
     * The constructor.
     */
    public MercurialEclipsePlugin() {
        plugin = this;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        try {
            super.start(context);
            DefaultConfiguration cfg = new DefaultConfiguration();
            HgClients.initialize(cfg, cfg, cfg);
            new Job(
                    "Starting MercurialEclipse.") {
                /*
                 * (non-Javadoc)
                 * 
                 * @see
                 * com.vectrace.MercurialEclipse.SafeWorkspaceJob#runSafe(org
                 * .eclipse.core.runtime.IProgressMonitor)
                 */
                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    try {
                        monitor.beginTask("Starting MercurialEclipse", 3);
                        monitor.subTask("Checking Mercurial installation.");
                        checkHgInstallation();
                        monitor.done();
                        // read known repositories
                        monitor
                                .subTask("Loading known Mercurial repositories.");
                        repoManager.start();
                        monitor.worked(1);
                        // read in commit messages from disk
                        monitor.subTask("Starting Commit Message manager.");
                        commitMessageManager.start();
                        monitor.worked(1);
                        monitor.done();
                        return new Status(IStatus.OK, ID, "MercurialEclipse started successfully.");
                    } catch (Exception e) {
                        MercurialEclipsePlugin.logError(e);
                        return new Status(IStatus.ERROR, ID, e
                                .getLocalizedMessage(), e);
                    }
                }
            }.schedule();
        } catch (Exception e) {
            this.hgUsable = false;
            logError(Messages.getString("MercurialEclipsePlugin.unableToStart"), e); //$NON-NLS-1$
        }
        
    }

    /**
     * Checks if Mercurial is configured properly by issuing the hg debuginstall
     * command.
     */
    public void checkHgInstallation() {
        try {
            this.hgUsable = true;
            MercurialUtilities.getHGExecutable(true);
            String result = HgDebugInstallClient.debugInstall();
            if (result.endsWith("No problems detected")) { //$NON-NLS-1$
                this.hgUsable = true;
                return;
            }
        } catch (Exception e) {
            MercurialEclipsePlugin.logError(e);
            MercurialEclipsePlugin.showError(e);
            this.hgUsable = false;
        }
    }

    /**
     * Gets the repository manager
     * 
     * @return
     */
    static public HgRepositoryLocationManager getRepoManager() {
        return repoManager;
    }

    public static HgCommitMessageManager getCommitMessageManager() {
        return commitMessageManager;
    }

    public static void setCommitMessageManager(
            HgCommitMessageManager commitMessageManager) {
        MercurialEclipsePlugin.commitMessageManager = commitMessageManager;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        try {
            MercurialStatusCache.getInstance().clear();
            AbstractCache.clearNodeMap();
            LocalChangesetCache.getInstance().clear();
            IncomingChangesetCache.getInstance().clear();
            OutgoingChangesetCache.getInstance().clear();
            repoManager.stop();
            // save commit messages to disk
            commitMessageManager.stop();
            plugin = null;
        } finally {
            super.stop(context);
        }
    }

    /**
     * Returns the shared instance.
     */
    public static MercurialEclipsePlugin getDefault() {
        return plugin;
    }

    /**
     * Returns an image descriptor for the image file at the given plug-in
     * relative path.
     * 
     * @param path
     *            the path
     * @return the image descriptor
     */
    public static ImageDescriptor getImageDescriptor(String path) {
        return AbstractUIPlugin.imageDescriptorFromPlugin(
                "com.vectrace.MercurialEclipse", "icons/" + path); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public static final void logError(String message, Throwable error) {
        getDefault().getLog().log(
                createStatus(message, 0, IStatus.ERROR, error));
    }

    public static void showError(final Throwable error) {
        new SafeUiJob(Messages.getString("MercurialEclipsePlugin.showError")) { //$NON-NLS-1$
            @Override
            protected IStatus runSafe(IProgressMonitor monitor) {
                ErrorDialog.openError(null, Messages.getString("MercurialEclipsePlugin.unexpectedError"), error.getMessage(), //$NON-NLS-1$
                        createStatus(error.getMessage(), 0, IStatus.ERROR, error));
                return super.runSafe(monitor);
            }
        }.schedule();
    }

    public static final void logWarning(String message, Throwable error) {
        getDefault().getLog().log(
                createStatus(message, 0, IStatus.WARNING, error));
    }

    public static final void logInfo(String message, Throwable error) {
        getDefault().getLog()
                .log(createStatus(message, 0, IStatus.INFO, error));
    }

    private static IStatus createStatus(String msg, int code, int severity,
            Throwable ex) {
        return new Status(severity, ID, code, msg, ex);
    }

    /**
     * @param ex
     */
    public final static void logError(Throwable ex) {
        logError(ex.getMessage(), ex);
    }

    /**
     * Creates a busy cursor and runs the specified runnable. May be called from
     * a non-UI thread.
     * 
     * @param parent
     *            the parent Shell for the dialog
     * @param cancelable
     *            if true, the dialog will support cancelation
     * @param runnable
     *            the runnable
     * 
     * @exception InvocationTargetException
     *                when an exception is thrown from the runnable
     * @exception InterruptedException
     *                when the progress monitor is cancelled
     */
    public static void runWithProgress(Shell parent, boolean cancelable,
            final IRunnableWithProgress runnable)
            throws InvocationTargetException, InterruptedException {

        boolean createdShell = false;
        Shell myParent = parent;
        try {
            if (myParent == null || myParent.isDisposed()) {
                Display display = Display.getCurrent();
                if (display == null) {
                    // cannot provide progress (not in UI thread)
                    runnable.run(new NullProgressMonitor());
                    return;
                }
                // get the active shell or a suitable top-level shell
                myParent = display.getActiveShell();
                if (myParent == null) {
                    myParent = new Shell(display);
                    createdShell = true;
                }
            }
            // pop up progress dialog after a short delay
            final Exception[] holder = new Exception[1];
            BusyIndicator.showWhile(myParent.getDisplay(), new Runnable() {
                public void run() {
                    try {
                        runnable.run(new NullProgressMonitor());
                    } catch (InvocationTargetException e) {
                        holder[0] = e;
                    } catch (InterruptedException e) {
                        holder[0] = e;
                    }
                }
            });
            if (holder[0] != null) {
                if (holder[0] instanceof InvocationTargetException) {
                    throw (InvocationTargetException) holder[0];
                }
                throw (InterruptedException) holder[0];

            }
            // new TimeoutProgressMonitorDialog(parent, TIMEOUT).run(true
            // /*fork*/, cancelable, runnable);
        } finally {
            if (createdShell) {
                parent.dispose();
            }
        }
    }

    /**
     * Convenience method to get the currently active workbench page. Note that
     * the active page may not be the one that the usr perceives as active in
     * some situations so this method of obtaining the activae page should only
     * be used if no other method is available.
     * 
     * @return the active workbench page
     */
    public static IWorkbenchPage getActivePage() {
        IWorkbenchWindow window = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow();
        if (window == null) {
            return null;
        }
        return window.getActivePage();
    }

    /**
     * @return the hgUsable
     */
    public boolean isHgUsable() {
        return hgUsable;
    }

    /**
     * @return
     */
    public static Display getStandardDisplay() {
        return PlatformUI.getWorkbench().getDisplay();
    }

    public synchronized ActiveChangeSetManager getChangeSetManager() {
        if (changeSetManager == null) {
            changeSetManager = new HgActiveChangeSetCollector(
                    MercurialSynchronizeSubscriber.getInstance());
        }
        return changeSetManager;
    }
}