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
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeWorkspaceJob;
import com.vectrace.MercurialEclipse.dialogs.CommitDialog;
import com.vectrace.MercurialEclipse.dialogs.CommitResource;
import com.vectrace.MercurialEclipse.dialogs.CommitResourceUtil;
import com.vectrace.MercurialEclipse.dialogs.RevertDialog;
import com.vectrace.MercurialEclipse.exception.HgException;
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
        Shell shell;
        IWorkbench workbench;

        // do the actual work in here
        List<IResource> resources;
        try {
            root = null;
            resources = new ArrayList<IResource>();

            for (Object obj : selection.toList()) {
                if (obj instanceof IResource) {
                    IResource resource = (IResource) obj;
                    boolean merging = resource.getProject()
                            .getPersistentProperty(ResourceProperties.MERGING) != null;
                    boolean supervised = MercurialUtilities
                            .hgIsTeamProviderFor(resource, false) == true;
                    
                    if (supervised && !merging) {
                        resources.add(resource);
                        if (root == null) {
                            root = new HgRoot(MercurialUtilities
                                    .search4MercurialRoot(resources.get(0)
                                            .getLocation().toFile()));
                        }
                    }
                }
            }

            CommitResource[] commitResources = new CommitResourceUtil(root)
                    .getCommitResources(resources
                            .toArray(new IResource[resources.size()]));

            // Check to see if there are any that are untracked.
            int count = 0;
            for (int i = 0; i < commitResources.length; i++) {
                if (!commitResources[i].getStatus().startsWith(
                        CommitDialog.FILE_UNTRACKED)) {
                    count++;
                }
            }

            if (count != 0) {
                RevertDialog chooser = new RevertDialog(Display.getCurrent()
                        .getActiveShell());
                chooser.setFiles(commitResources);
                if (chooser.open() == Window.OK) {
                    final List<CommitResource> result = chooser.getSelection();
                    new SafeWorkspaceJob("Revert files") {
                        @Override
                        protected IStatus runSafe(IProgressMonitor monitor) {
                            doRevert(monitor, result);
                            return Status.OK_STATUS;
                        }
                    }.schedule();
                }
            } else {
                // Get shell & workbench
                if ((window != null) && (window.getShell() != null)) {
                    shell = window.getShell();
                } else {
                    workbench = PlatformUI.getWorkbench();
                    shell = workbench.getActiveWorkbenchWindow().getShell();
                }
                MessageDialog.openInformation(shell,
                        "Mercurial Eclipse hg revert", "No files to revert!");
            }
        } catch (CoreException e) {
            MercurialEclipsePlugin.logError(e);
            MercurialEclipsePlugin.showError(e);
        }
    }

    private void doRevert(IProgressMonitor monitor,
            List<CommitResource> resources) {
        // the last argument will be replaced with a path
        String launchCmd[] = { MercurialUtilities.getHGExecutable(), "revert",
                "--no-backup", "--", "" };
        monitor.beginTask("Reverting resources...", resources.size() * 2);
        for (CommitResource revertResource : resources) {
            IResource resource = revertResource.getResource();
            // Resource could be inside a link or something do nothing
            // in the future this could check is this is another repository

            // Setup and run command
            File workingDir = resource.getParent().getLocation().toFile();
            launchCmd[4] = resource.getName();
            // System.out.println("Revert = " + FullPath);
            // IResourceChangeEvent event = new IResourceChangeEvent();
            try {
                monitor.subTask("Reverting " + resource.getName() + "...");
                MercurialUtilities.executeCommand(launchCmd, workingDir, true);
                monitor.worked(1);
            } catch (HgException e) {
                MercurialEclipsePlugin.logError(e);
            }
            if (monitor.isCanceled()) {
                break;
            }
        }

        for (CommitResource commitResource : resources) {
            monitor.subTask("Refreshing " + commitResource + "...");
            IResource resource = commitResource.getResource();
            try {
                resource.refreshLocal(IResource.DEPTH_ONE, monitor);
            } catch (CoreException e) {
                MercurialEclipsePlugin.logError(e);
            }
            monitor.worked(1);
            // if (!refreshedProjects.contains(resource.getProject())) {
            // final IProject proj = resource.getProject();
            // new SafeUiJob("Updating status") {
            // @Override
            // protected IStatus runSafe(IProgressMonitor monitor1) {
            // try {
            // MercurialStatusCache.getInstance().refresh(proj);
            // } catch (TeamException e) {
            // MercurialEclipsePlugin.logError(
            // "Unable to refresh project: ", e);
            // }
            // return super.runSafe(monitor1);
            // }
            // }.schedule();
            // refreshedProjects.add(proj);
            // }
        }
        monitor.done();
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
