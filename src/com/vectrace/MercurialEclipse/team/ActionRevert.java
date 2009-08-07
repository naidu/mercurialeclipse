/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Software Balm Consulting Inc (Peter Hunnisett <peter_hge at softwarebalm dot com>) - some updates
 *     StefanC                   - some updates
 *     Charles O'Farrell         - fix revert open file
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeWorkspaceJob;
import com.vectrace.MercurialEclipse.dialogs.RevertDialog;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.menu.UpdateHandler;
import com.vectrace.MercurialEclipse.model.HgRoot;

public class ActionRevert implements IWorkbenchWindowActionDelegate {
    private IWorkbenchWindow window;
    // private IWorkbenchPart targetPart;
    private IStructuredSelection selection;
    private HgRoot root;

    public ActionRevert() {
        super();
    }

    /**
     * We can use this method to dispose of any system resources we previously
     * allocated.
     *
     * @see IWorkbenchWindowActionDelegate#dispose
     */
    public void dispose() {
    }

    /**
     * We will cache window object in order to be able to provide parent shell
     * for the message dialog.
     *
     * @see IWorkbenchWindowActionDelegate#init
     */
    public void init(IWorkbenchWindow w) {
        this.window = w;
    }

    /**
     * The action has been activated. The argument of the method represents the
     * 'real' action sitting in the workbench UI.
     *
     * @see IWorkbenchWindowActionDelegate#run
     */

    public void run(IAction action) {
        if(window == null){
            window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        }
        // do the actual work in here
        List<IResource> resources;
        try {
            root = null;
            resources = new ArrayList<IResource>();

            boolean mergeIsRunning = collectResourcesToRevert(resources);

            if (resources.size() > 0 && !mergeIsRunning) {
                openRevertDialog(resources, false);
            } else {
                if(mergeIsRunning){
                    boolean doRevert = MessageDialog
                        .openConfirm(
                            getShell(),
                            Messages.getString("ActionRevert.HgRevert"), //$NON-NLS-1$
                            Messages.getString("ActionRevert.mergeIsRunning")); //$NON-NLS-1$
                    if(doRevert){
                        openRevertDialog(resources, true);
                    }
                } else {
                    MessageDialog
                            .openInformation(
                                    getShell(),
                                    Messages.getString("ActionRevert.HgRevert"), //$NON-NLS-1$
                                    Messages.getString("ActionRevert.noFilesToRevert")); //$NON-NLS-1$
                }
            }
        } catch (CoreException e) {
            MercurialEclipsePlugin.logError(e);
            MercurialEclipsePlugin.showError(e);
        }
    }

    private Shell getShell() {
        Shell shell;
        // Get shell
        if (window != null && window.getShell() != null) {
            shell = window.getShell();
        } else {
            shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        }
        return shell;
    }

    private void openRevertDialog(List<IResource> resources, final boolean cleanAfterMerge) {
        final List<IResource> result;
        if(!cleanAfterMerge){
            RevertDialog chooser = new RevertDialog(Display.getCurrent()
                    .getActiveShell(), root);
            chooser.setFiles(resources);
            if (chooser.open() != Window.OK) {
                return;
            }
            result = chooser.getSelection();
        } else {
            result = resources;
        }

        new SafeWorkspaceJob(Messages
                .getString("ActionRevert.revertFiles")) { //$NON-NLS-1$
            @Override
            protected IStatus runSafe(IProgressMonitor monitor) {
                doRevert(monitor, result, cleanAfterMerge);
                return Status.OK_STATUS;
            }
        }.schedule();
    }

    private boolean collectResourcesToRevert(List<IResource> resources) throws CoreException, HgException {
        boolean mergeIsRunning = false;
        for (Object obj : selection.toList()) {
            if (obj instanceof IResource) {
                IResource resource = (IResource) obj;
                boolean merging = resource.getProject()
                        .getPersistentProperty(ResourceProperties.MERGING) != null;
                if(merging){
                    mergeIsRunning = true;
                }
                boolean supervised = MercurialUtilities
                        .hgIsTeamProviderFor(resource, false) == true;

                if (supervised) {
                    resources.add(resource);
                    if (root == null) {
                        // XXX this does not work for projects from different hg roots
                        root = MercurialTeamProvider.getHgRoot(resource);
                    }
                }
            }
        }
        return mergeIsRunning;
    }

    public void doRevert(IProgressMonitor monitor, List<IResource> resources, boolean cleanAfterMerge) {
        monitor
                .beginTask(
                        Messages.getString("ActionRevert.revertingResources"), resources.size() * 2); //$NON-NLS-1$
        if(!cleanAfterMerge) {
            performRegularRevert(monitor, resources);
        } else {
            performRevertAfterMerge(monitor, resources);
        }

        for (IResource resource : resources) {
            monitor
                    .subTask(Messages.getString("ActionRevert.refreshing") + resource.getName() + "..."); //$NON-NLS-1$ //$NON-NLS-2$
            try {
                resource.refreshLocal(IResource.DEPTH_ONE, monitor);
            } catch (CoreException e) {
                MercurialEclipsePlugin.logError(e);
            }
            monitor.worked(1);
        }
        monitor.done();
    }

    private void performRegularRevert(IProgressMonitor monitor, List<IResource> resources) {
        // the last argument will be replaced with a path
        String launchCmd[] = { MercurialUtilities.getHGExecutable(), "revert", //$NON-NLS-1$
                "--no-backup", "--", "" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        for (IResource resource : resources) {
            if (monitor.isCanceled()) {
                break;
            }
            // Resource could be inside a link or something do nothing
            // in the future this could check is this is another repository

            // Setup and run command
            File workingDir = resource.getParent().getLocation().toFile();
            launchCmd[4] = resource.getName();
            // System.out.println("Revert = " + FullPath);
            // IResourceChangeEvent event = new IResourceChangeEvent();
            try {
                monitor
                        .subTask(Messages.getString("ActionRevert.reverting") + resource.getName() + "..."); //$NON-NLS-1$ //$NON-NLS-2$
                MercurialUtilities.executeCommand(launchCmd, workingDir, true);
                monitor.worked(1);
            } catch (HgException e) {
                MercurialEclipsePlugin.logError(e);
            }
        }
    }

    private void performRevertAfterMerge(IProgressMonitor monitor, List<IResource> resources) {
        // see http://mercurial.selenic.com/wiki/FAQ#FAQ.2BAC8-CommonProblems.hg_status_shows_changed_files_but_hg_diff_doesn.27t.21
        // To completely undo the uncommitted merge and discard all local modifications,
        // you will need to issue a hg update -C -r . (note the "dot" at the end of the command).
        try {
            UpdateHandler update = new UpdateHandler();
            update.setCleanEnabled(true);
            update.setRevision(".");
            update.setShell(getShell());
            update.run(resources.get(0).getProject());
        } catch (Exception e) {
            MercurialEclipsePlugin.logError(e);
        }

    }

    /**
     * Selection in the workbench has been changed. We can change the state of
     * the 'real' action here if we want, but this can only happen after the
     * delegate has been created.
     *
     * @see IWorkbenchWindowActionDelegate#selectionChanged
     */
    public void selectionChanged(IAction action, ISelection in_selection) {
        if (in_selection != null
                && in_selection instanceof IStructuredSelection) {
            selection = (IStructuredSelection) in_selection;
        }
    }

}
